[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9._@:-]+$')]
    [string] $SshTarget,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^/home/[A-Za-z0-9._-]+(?:/[A-Za-z0-9._-]+)*$')]
    [string] $RemoteRoot,

    [int] $SshPort = 22,

    [ValidateScript({ Test-Path -LiteralPath $_ -PathType Leaf })]
    [string] $IdentityFile,

    [switch] $PreflightOnly,

    [switch] $UploadOnly,

    [switch] $ApproveUpload
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$sshArgs = @('-o', 'BatchMode=yes', '-o', 'ConnectTimeout=10', '-p', $SshPort.ToString())
if ($IdentityFile) {
    $resolvedIdentityFile = (Resolve-Path -LiteralPath $IdentityFile).Path
    $sshArgs += @('-i', $resolvedIdentityFile, '-o', 'IdentitiesOnly=yes')
}
$sshArgs += $SshTarget

function Invoke-SshScript {
    param([Parameter(Mandatory = $true)][string] $Script)
    $Script | & ssh @sshArgs 'sh -s'
    if ($LASTEXITCODE -ne 0) {
        throw "SSH command failed with exit code $LASTEXITCODE."
    }
}

foreach ($command in @('ssh', 'scp', 'tar', 'git')) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "Required local command is missing: $command"
    }
}

$safeRemoteRoot = $RemoteRoot
$preflight = @"
set -eu
remote_root='$safeRemoteRoot'
[ "`$(uname -s)" = Linux ] || { echo 'VPS is not Linux.' >&2; exit 69; }
case "`$remote_root" in /home/*) ;; *) echo 'Remote root must be below /home.' >&2; exit 73;; esac
command -v docker >/dev/null 2>&1 || { echo 'Docker Engine is missing.' >&2; exit 69; }
docker compose version >/dev/null 2>&1 || { echo 'Docker Compose plugin is missing.' >&2; exit 69; }
docker info >/dev/null 2>&1 || { echo 'Docker is unavailable to this SSH user.' >&2; exit 69; }
available_kb=`$(df -Pk /home | awk 'NR == 2 { print `$4 }')
[ "`$available_kb" -ge 10485760 ] || { echo 'At least 10 GB must be free below /home.' >&2; exit 69; }
if [ -e "`$remote_root" ] && [ ! -f "`$remote_root/.sharedhouse-managed" ]; then
  [ ! -d "`$remote_root" ] || [ -z "`$(ls -A "`$remote_root")" ] || {
    echo 'Remote directory exists and is not owned by the SharedHouse installer.' >&2
    exit 73
  }
fi
echo 'Read-only VPS preflight: OK'
echo "Docker: `$(docker version --format '{{.Server.Version}}')"
echo "Compose: `$(docker compose version --short)"
echo "Existing containers: `$(docker ps -aq | wc -l | tr -d ' ') (none will be modified)"
docker ps --format '  {{.Names}} | {{.Image}} | {{.Status}}'
"@

Write-Host 'Running read-only VPS and container preflight...'
Invoke-SshScript -Script $preflight
if ($PreflightOnly) {
    Write-Host 'Preflight-only mode completed. Nothing was written to the VPS.'
    exit 0
}

if (-not $ApproveUpload) {
    $confirmation = Read-Host "Type DEPLOY to upload SharedHouse only to $RemoteRoot"
    if ($confirmation -cne 'DEPLOY') {
        throw 'Deployment cancelled. Nothing was written to the VPS.'
    }
}

$timestamp = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')
$archive = Join-Path ([IO.Path]::GetTempPath()) "sharedhouse-$timestamp.tar.gz"
$fileList = Join-Path ([IO.Path]::GetTempPath()) "sharedhouse-$timestamp-files.txt"
$remoteArchive = "$RemoteRoot.upload-$timestamp.tar.gz"

try {
    Push-Location $repositoryRoot
    try {
        $files = @(& git ls-files -co --exclude-standard)
        if ($LASTEXITCODE -ne 0 -or $files.Count -eq 0) {
            throw 'Could not build the repository file list.'
        }
        [IO.File]::WriteAllLines($fileList, $files, [Text.UTF8Encoding]::new($false))
        & tar -czf $archive -T $fileList
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $archive)) {
            throw 'Could not create the deployment archive.'
        }
    }
    finally {
        Pop-Location
    }

    $scpArgs = @('-o', 'BatchMode=yes', '-o', 'ConnectTimeout=10', '-P', $SshPort.ToString())
    if ($IdentityFile) {
        $scpArgs += @('-i', $resolvedIdentityFile, '-o', 'IdentitiesOnly=yes')
    }
    $scpArgs += @($archive, "${SshTarget}:$remoteArchive")
    & scp @scpArgs
    if ($LASTEXITCODE -ne 0) {
        throw "SCP upload failed with exit code $LASTEXITCODE."
    }

    $safeRemoteArchive = $remoteArchive
    $installCommand = if ($UploadOnly) { ':' } else { './infra/production/scripts/install-interactive.sh' }
    $install = @"
set -eu
remote_root='$safeRemoteRoot'
remote_archive='$safeRemoteArchive'
cleanup() { rm -f -- "`$remote_archive"; }
trap cleanup EXIT HUP INT TERM
umask 077
mkdir -p -- "`$remote_root"
touch "`$remote_root/.sharedhouse-managed"
tar -xzf "`$remote_archive" -C "`$remote_root"
chmod +x "`$remote_root/infra/production/scripts/"*.sh
cd "`$remote_root"
$installCommand
"@

    Write-Host 'Archive uploaded. Running the scoped remote staging script...'
    $interactiveArgs = @('-t', '-o', 'ConnectTimeout=10', '-p', $SshPort.ToString())
    if ($IdentityFile) {
        $interactiveArgs += @('-i', $resolvedIdentityFile, '-o', 'IdentitiesOnly=yes')
    }
    $encodedInstall = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($install))
    $remoteScriptPath = "/home/.sharedhouse-remote-$timestamp.sh"
    $remoteRunner = "umask 077; printf '%s' '$encodedInstall' | base64 -d > '$remoteScriptPath'; chmod 700 '$remoteScriptPath'; sh '$remoteScriptPath'; result=`$?; rm -f -- '$remoteScriptPath'; exit `$result"
    $interactiveArgs += @($SshTarget, $remoteRunner)
    & ssh @interactiveArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Remote installer failed with exit code $LASTEXITCODE."
    }
}
finally {
    Remove-Item -LiteralPath $archive, $fileList -Force -ErrorAction SilentlyContinue
}

if ($UploadOnly) {
    Write-Host 'SharedHouse source upload completed.'
}
else {
    Write-Host 'SharedHouse installation and public health gate completed.'
}

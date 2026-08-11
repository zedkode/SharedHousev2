[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$secretRoot = Join-Path $env:USERPROFILE '.sharedhouse\release'
$keystore = Join-Path $secretRoot 'sharedhouse-direct-release.jks'
$credentialFile = Join-Path $secretRoot 'sharedhouse-direct-release.credential.clixml'
$passwordFile = Join-Path $secretRoot '.keytool-password.tmp'
$alias = 'sharedhouse-direct-release'

if ((Test-Path -LiteralPath $keystore) -or (Test-Path -LiteralPath $credentialFile)) {
    throw "Direct signing material already exists below $secretRoot. Refusing to replace it."
}

New-Item -ItemType Directory -Path $secretRoot -Force | Out-Null
$randomBytes = [byte[]]::new(36)
$random = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $random.GetBytes($randomBytes)
}
finally {
    $random.Dispose()
}
$password = [Convert]::ToBase64String($randomBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
[IO.File]::WriteAllText($passwordFile, $password, [Text.UTF8Encoding]::new($false))

try {
    $keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
    if (-not (Test-Path -LiteralPath $keytool -PathType Leaf)) {
        $keytool = 'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe'
    }
    if (-not (Test-Path -LiteralPath $keytool -PathType Leaf)) {
        throw 'A complete JDK keytool.exe is required.'
    }

    & $keytool -genkeypair -noprompt `
        -keystore $keystore `
        -storetype PKCS12 `
        -storepass:file $passwordFile `
        -keypass:file $passwordFile `
        -alias $alias `
        -keyalg RSA `
        -keysize 4096 `
        -validity 10000 `
        -dname 'CN=SharedHouse Direct Release, O=SharedHouse, C=GB'
    if ($LASTEXITCODE -ne 0) {
        throw "keytool failed with exit code $LASTEXITCODE."
    }

    $securePassword = ConvertTo-SecureString $password -AsPlainText -Force
    [PSCredential]::new($alias, $securePassword) | Export-Clixml -LiteralPath $credentialFile
}
finally {
    Remove-Item -LiteralPath $passwordFile -Force -ErrorAction SilentlyContinue
    $password = $null
}

& icacls.exe $secretRoot /inheritance:r /grant:r "${env:USERNAME}:(OI)(CI)F" | Out-Null
Write-Output "Direct Android signing material created below $secretRoot."
Write-Output 'It is not a Google Play upload key. Back up this directory securely to preserve app updates.'

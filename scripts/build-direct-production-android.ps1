[CmdletBinding()]
param(
    [ValidateRange(1, 2100000000)]
    [int] $VersionCode = 2,

    [ValidatePattern('^[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9.-]+)?$')]
    [string] $VersionName = '0.2.0',

    [switch] $RequireConnectedDevice
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$secretRoot = Join-Path $env:USERPROFILE '.sharedhouse\release'
$keystore = Join-Path $secretRoot 'sharedhouse-direct-release.jks'
$credentialFile = Join-Path $secretRoot 'sharedhouse-direct-release.credential.clixml'

if (-not (Test-Path -LiteralPath $keystore -PathType Leaf) -or
    -not (Test-Path -LiteralPath $credentialFile -PathType Leaf)) {
    throw 'Direct signing material is missing. Run initialize-direct-android-signing.ps1 once.'
}

$credential = Import-Clixml -LiteralPath $credentialFile
$password = $credential.GetNetworkCredential().Password
$previousJavaHome = $env:JAVA_HOME
$androidStudioJbr = 'C:\Program Files\Android\Android Studio\jbr'
if (-not (Test-Path -LiteralPath (Join-Path $androidStudioJbr 'bin\jlink.exe') -PathType Leaf)) {
    throw 'Android Studio JBR with jlink.exe is required for the production build.'
}
$environmentNames = @(
    'SHAREDHOUSE_RELEASE_STORE_FILE',
    'SHAREDHOUSE_RELEASE_STORE_PASSWORD',
    'SHAREDHOUSE_RELEASE_KEY_ALIAS',
    'SHAREDHOUSE_RELEASE_KEY_PASSWORD',
    'SHAREDHOUSE_VERSION_CODE',
    'SHAREDHOUSE_VERSION_NAME',
    'SHAREDHOUSE_ENABLE_GOOGLE_SERVICES'
)

try {
    $env:SHAREDHOUSE_RELEASE_STORE_FILE = $keystore
    $env:SHAREDHOUSE_RELEASE_STORE_PASSWORD = $password
    $env:SHAREDHOUSE_RELEASE_KEY_ALIAS = $credential.UserName
    $env:SHAREDHOUSE_RELEASE_KEY_PASSWORD = $password
    $env:SHAREDHOUSE_VERSION_CODE = $VersionCode.ToString()
    $env:SHAREDHOUSE_VERSION_NAME = $VersionName
    $env:SHAREDHOUSE_ENABLE_GOOGLE_SERVICES = 'false'
    $env:JAVA_HOME = $androidStudioJbr

    Push-Location $repositoryRoot
    try {
        & .\gradlew.bat --stop
        & .\gradlew.bat --no-configuration-cache `
            :shared:domain:jvmTest `
            :shared:network:jvmTest `
            :apps:android:app:testPublicDebugUnitTest `
            :apps:android:app:lintPublicRelease `
            :apps:android:app:packagePublicReleaseApk
        if ($LASTEXITCODE -ne 0) {
            throw "Production Android build failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }
}
finally {
    $password = $null
    $env:JAVA_HOME = $previousJavaHome
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
}

$apk = Join-Path $repositoryRoot "apps\android\app\build\outputs\apk\release\SharedHouse-v$VersionName-public-release-signed.apk"
if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) {
    throw "Expected production APK is missing: $apk"
}
$buildConfig = Join-Path $repositoryRoot 'apps\android\app\build\generated\source\buildConfig\public\release\com\sharedhouse\android\BuildConfig.java'
$buildConfiguration = Get-Content -Raw -LiteralPath $buildConfig
if (-not $buildConfiguration.Contains('API_BASE_URL = "https://houseapi.dohotstudio.com"') -or
    -not $buildConfiguration.Contains('ADMOB_CONFIGURED = false') -or
    -not $buildConfiguration.Contains('FIREBASE_CONFIGURED = false')) {
    throw 'Production endpoint or disabled optional-service flags are incorrect.'
}

$apksigner = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Recurse -Filter apksigner.bat |
    Sort-Object FullName -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if ([string]::IsNullOrWhiteSpace($apksigner)) {
    throw 'Android SDK apksigner.bat is required to verify the production APK.'
}
& $apksigner verify --verbose --print-certs $apk
if ($LASTEXITCODE -ne 0) {
    throw 'The production APK signature could not be verified.'
}

if ($RequireConnectedDevice) {
    $adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
    if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) {
        throw 'Android platform-tools adb.exe is required for the connected-device release gate.'
    }

    $connectedDevices = @(
        & $adb devices |
            Select-String -Pattern '^([^\s]+)\s+device$' |
            ForEach-Object { $_.Matches[0].Groups[1].Value }
    )
    if ($connectedDevices.Count -ne 1) {
        throw "The release gate requires exactly one authorized Android device; found $($connectedDevices.Count)."
    }

    $deviceSerial = $connectedDevices[0]
    $packageName = 'com.sharedhouse.android'
    $componentName = "$packageName/.MainActivity"
    $crashPattern = "Process: $packageName,"
    $baselineCrashCount = @(
        & $adb -s $deviceSerial logcat -b crash -d -v brief 2>&1 |
            Select-String -SimpleMatch $crashPattern
    ).Count

    $installOutput = @(& $adb -s $deviceSerial install -r $apk 2>&1)
    if ($LASTEXITCODE -ne 0 -or -not ($installOutput -match '^Success$')) {
        throw "Production APK installation failed on ${deviceSerial}: $($installOutput -join ' ')"
    }

    $coldStartCount = 5
    for ($attempt = 1; $attempt -le $coldStartCount; $attempt++) {
        & $adb -s $deviceSerial shell am force-stop $packageName | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Could not stop $packageName before cold-start attempt $attempt."
        }

        $launchOutput = @(& $adb -s $deviceSerial shell am start -W -n $componentName 2>&1)
        if ($LASTEXITCODE -ne 0 -or -not ($launchOutput -match '^Status:\s+ok$')) {
            throw "Cold-start attempt $attempt failed: $($launchOutput -join ' ')"
        }

        Start-Sleep -Seconds 3
        $processId = (& $adb -s $deviceSerial shell pidof $packageName 2>&1 | Out-String).Trim()
        if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($processId)) {
            throw "The app process was not alive after cold-start attempt $attempt."
        }

        $currentCrashCount = @(
            & $adb -s $deviceSerial logcat -b crash -d -v brief 2>&1 |
                Select-String -SimpleMatch $crashPattern
        ).Count
        if ($currentCrashCount -ne $baselineCrashCount) {
            throw "A new $packageName crash was recorded during cold-start attempt $attempt."
        }
    }

    $activityDump = @(& $adb -s $deviceSerial shell dumpsys activity activities 2>&1)
    $resumedActivity = @(
        $activityDump |
            Select-String -Pattern '(topResumedActivity|mResumedActivity|ResumedActivity|Resumed:).*com\.sharedhouse\.android/.MainActivity'
    )
    if ($resumedActivity.Count -eq 0) {
        throw 'SharedHouse is running but MainActivity is not reported as the resumed foreground activity.'
    }

    Write-Output "DEVICE_GATE=$deviceSerial; install=passed; cold_starts=$coldStartCount; new_crashes=0; foreground=passed"
}

$hash = Get-FileHash -Algorithm SHA256 -LiteralPath $apk
Write-Output "APK=$ap
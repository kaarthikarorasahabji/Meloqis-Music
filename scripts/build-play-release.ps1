[CmdletBinding()]
param(
    [string]$SigningFile,
    [string]$JavaHome,
    [string]$AndroidSdk
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $SigningFile) { $SigningFile = Join-Path $projectRoot 'release-private/signing.json' }
if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
if (-not (Test-Path "$env:JAVA_HOME/bin/java.exe")) {
    $javaCommand = Get-Command java.exe -ErrorAction Stop
    $env:JAVA_HOME = Split-Path (Split-Path $javaCommand.Source -Parent) -Parent
}
if ($AndroidSdk) { $env:ANDROID_HOME = $AndroidSdk; $env:ANDROID_SDK_ROOT = $AndroidSdk }
$privateSigning = Get-Content -LiteralPath $SigningFile -Raw | ConvertFrom-Json
$keystorePath = Join-Path (Split-Path $SigningFile -Parent) 'meloqis-upload.jks'
if (-not (Test-Path -LiteralPath $keystorePath)) { throw "Upload keystore missing: $keystorePath" }
$env:RELEASE_KEYSTORE_PATH = $keystorePath
$env:RELEASE_KEY_ALIAS = $privateSigning.alias
$env:RELEASE_STORE_PASSWORD = $privateSigning.storePassword
$env:RELEASE_KEY_PASSWORD = $privateSigning.keyPassword
$originalJavaOptions = $env:JAVA_TOOL_OPTIONS
$tempPath = Join-Path $projectRoot 'build/jtmp'
New-Item -ItemType Directory -Path $tempPath -Force | Out-Null
$env:JAVA_TOOL_OPTIONS = "$originalJavaOptions -Djava.net.preferIPv4Stack=true -Djava.io.tmpdir=`"$tempPath`" -Djdk.net.unixdomain.tmpdir=`"$tempPath`""
Push-Location $projectRoot
try {
    & ./gradlew.bat :androidApp:bundleRelease -PplayStore=true --no-configuration-cache --console=plain -x :androidApp:uploadCrashlyticsMappingFileRelease
    if ($LASTEXITCODE -ne 0) { throw 'Play release build failed. No upload-ready bundle was produced.' }
    $destination = Join-Path $projectRoot 'launch-package'
    New-Item -ItemType Directory -Path $destination -Force | Out-Null
    $bundle = Join-Path $destination 'Meloqis-Music-play-release.aab'
    Copy-Item androidApp/build/outputs/bundle/release/androidApp-release.aab $bundle -Force
    $signature = & "$env:JAVA_HOME/bin/jarsigner.exe" -verify -verbose -certs $bundle 2>&1
    $signature | Set-Content (Join-Path $destination 'signature-verification.txt')
    if ($LASTEXITCODE -ne 0 -or -not ($signature -match 'jar verified')) { throw 'Bundle signature verification failed.' }
    Copy-Item androidApp/build/outputs/mapping/release/mapping.txt $destination -Force
    Get-FileHash $bundle -Algorithm SHA256 | Format-List | Out-File (Join-Path $destination 'SHA256.txt')
    Write-Output "Signed Play bundle: $bundle"
} finally {
    Pop-Location
    $env:JAVA_TOOL_OPTIONS = $originalJavaOptions
    Remove-Item Env:RELEASE_KEYSTORE_PATH,Env:RELEASE_KEY_ALIAS,Env:RELEASE_STORE_PASSWORD,Env:RELEASE_KEY_PASSWORD -ErrorAction SilentlyContinue
}

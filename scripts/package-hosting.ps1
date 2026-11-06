[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$output = Join-Path $projectRoot 'launch-package'
$publicRoot = Join-Path $output 'hosting'
$siteRoot = Join-Path $publicRoot 'meloqis'
New-Item -ItemType Directory -Path $siteRoot -Force | Out-Null
# Explicit allowlist prevents local hosting credentials, signing material and build files from being published.
$publicFiles = @('index.html', 'privacy.html', 'support.html', 'delete-data.html', 'terms.html', 'legal.css', 'robots.txt', '_headers', 'vercel.json', 'festivals.json')
foreach ($name in $publicFiles) {
    Copy-Item -LiteralPath (Join-Path $projectRoot "website/$name") -Destination $siteRoot -Force
}
Copy-Item -LiteralPath (Join-Path $projectRoot 'website/assets') -Destination $siteRoot -Recurse -Force
Copy-Item -LiteralPath (Join-Path $projectRoot 'LICENSE') -Destination (Join-Path $siteRoot 'LICENSE.txt') -Force
& python (Join-Path $projectRoot 'scripts/package-release-source.py')
if ($LASTEXITCODE -ne 0) { throw 'Release source packaging failed' }
Compress-Archive -Path (Join-Path $publicRoot 'meloqis') -DestinationPath (Join-Path $output 'Meloqis-hosting.zip') -Force
Write-Output "Public hosting files: $publicRoot"
Write-Output "Upload the meloqis folder to the existing axenoraai.in website root."

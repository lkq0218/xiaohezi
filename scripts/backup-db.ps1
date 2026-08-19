param(
    [string]$ComposeFile = "$PSScriptRoot/../infra/docker-compose.yml",
    [string]$OutputDirectory = "$PSScriptRoot/../backups"
)
$ErrorActionPreference = 'Stop'
$resolvedOutput = [IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $resolvedOutput | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$containerFile = "/tmp/xiaohezi-$stamp.sql"
$target = Join-Path $resolvedOutput "xiaohezi-$stamp.sql"
docker compose -f $ComposeFile exec -T mysql sh -c "mysqldump -u root -p`$MYSQL_ROOT_PASSWORD --single-transaction --routines --triggers xiaohezi > $containerFile"
if ($LASTEXITCODE -ne 0) { throw 'mysqldump failed' }
$containerId = docker compose -f $ComposeFile ps -q mysql
docker cp "${containerId}:$containerFile" $target
docker compose -f $ComposeFile exec -T mysql rm -f $containerFile
if (-not (Test-Path $target) -or (Get-Item $target).Length -eq 0) { throw 'backup verification failed' }
Write-Output $target

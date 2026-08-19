param(
    [Parameter(Mandatory)][string]$BackupFile,
    [switch]$ConfirmRestore,
    [string]$ComposeFile = "$PSScriptRoot/../infra/docker-compose.yml"
)
$ErrorActionPreference = 'Stop'
if (-not $ConfirmRestore) { throw 'Restore overwrites database state. Re-run with -ConfirmRestore.' }
$resolved = [IO.Path]::GetFullPath($BackupFile)
if (-not (Test-Path -LiteralPath $resolved) -or [IO.Path]::GetExtension($resolved) -ne '.sql') {
    throw 'Backup must be an existing .sql file.'
}
$containerId = docker compose -f $ComposeFile ps -q mysql
docker cp $resolved "${containerId}:/tmp/xiaohezi-restore.sql"
docker compose -f $ComposeFile exec -T mysql sh -c 'mysql -u root -p$MYSQL_ROOT_PASSWORD xiaohezi < /tmp/xiaohezi-restore.sql'
if ($LASTEXITCODE -ne 0) { throw 'restore failed' }
docker compose -f $ComposeFile exec -T mysql rm -f /tmp/xiaohezi-restore.sql
docker compose -f $ComposeFile exec -T mysql sh -c 'mysql -u root -p$MYSQL_ROOT_PASSWORD -N -e "SELECT COUNT(*) FROM xiaohezi.flyway_schema_history;"'

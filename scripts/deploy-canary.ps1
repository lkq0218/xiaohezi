param(
    [Parameter(Mandatory)][string]$Image,
    [int]$CanaryPort = 18080,
    [string]$ComposeFile = "$PSScriptRoot/../infra/docker-compose.canary.yml"
)
$ErrorActionPreference = 'Stop'
$env:APP_IMAGE = $Image
$env:APP_PORT = "$CanaryPort"
docker compose -p xiaohezi-canary -f $ComposeFile up -d --no-deps app
if ($LASTEXITCODE -ne 0) { throw 'canary deployment failed' }
for ($attempt = 1; $attempt -le 30; $attempt++) {
    try {
        $health = Invoke-RestMethod "http://localhost:$CanaryPort/actuator/health/readiness"
        if ($health.status -eq 'UP') { Write-Output "Canary $Image is ready on $CanaryPort"; exit 0 }
    } catch { Start-Sleep -Seconds 2 }
}
docker compose -p xiaohezi-canary -f $ComposeFile logs app
throw 'canary health check timed out; stable deployment was not changed'

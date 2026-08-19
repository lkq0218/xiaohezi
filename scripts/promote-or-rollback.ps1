param(
    [Parameter(Mandatory)][string]$CandidateImage,
    [Parameter(Mandatory)][string]$PreviousImage,
    [switch]$Promote,
    [string]$ComposeFile = "$PSScriptRoot/../infra/docker-compose.yml"
)
$ErrorActionPreference = 'Stop'
$env:APP_IMAGE = if ($Promote) { $CandidateImage } else { $PreviousImage }
docker compose -p xiaohezi -f $ComposeFile up -d --no-build app
if ($LASTEXITCODE -ne 0) { throw 'deployment command failed' }
for ($attempt = 1; $attempt -le 30; $attempt++) {
    try {
        $health = Invoke-RestMethod 'http://localhost:8080/actuator/health/readiness'
        if ($health.status -eq 'UP') {
            Write-Output "Deployed immutable image: $env:APP_IMAGE"
            exit 0
        }
    } catch { Start-Sleep -Seconds 2 }
}
if ($Promote) {
    Write-Warning "Candidate failed health checks; automatically rolling back to $PreviousImage"
    $env:APP_IMAGE = $PreviousImage
    docker compose -p xiaohezi -f $ComposeFile up -d --no-build app
}
throw 'deployment did not become healthy'

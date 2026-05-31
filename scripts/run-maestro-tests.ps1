if (!(Get-Command maestro -ErrorAction SilentlyContinue)) {
  Write-Host "Maestro is not installed or not on PATH."
  Write-Host "Install Maestro first (https://maestro.mobile.dev/), then run this script again."
  exit 1
}

Write-Host "Starting Maestro tests for com.fishlogapp.mobile..."
maestro test .maestro

# a4-run-all.ps1 - one command to get the OptiGridZA prototype ready for the Assignment 4 demo and evidence run.
# Run from the repo root, for example:
#   cd C:\Users\nathi\OneDrive\Desktop\OptiGridZA
#   Set-ExecutionPolicy -Scope Process Bypass
#   .\backend\scripts\a4-run-all.ps1            (add -Reset to wipe the database first)
param(
  [switch]$Reset,       # docker compose down -v first (clean database)
  [switch]$SkipBuild,   # reuse existing images (faster)
  [switch]$SkipTests    # only start the system and create the demo accounts
)
$ErrorActionPreference = 'Stop'
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$Backend  = Join-Path $RepoRoot 'backend'
$Frontend = Join-Path $RepoRoot 'frontend'
$Gateway  = 'http://127.0.0.1:8080'
$Evidence = Join-Path $RepoRoot ('a4-evidence-' + (Get-Date -Format 'yyyyMMdd-HHmm'))
New-Item -ItemType Directory -Force -Path $Evidence | Out-Null

function Step($n,$t){ Write-Host "`n=== Step ${n}: $t ===" -ForegroundColor Cyan }
function Native([scriptblock]$b){ $old=$ErrorActionPreference; $ErrorActionPreference='Continue'; try { & $b } finally { $ErrorActionPreference=$old } }

Step 1 'Check Docker Desktop is running'
Native { docker info *> $null }
if($LASTEXITCODE -ne 0){ throw 'Docker Desktop is not running. Start it, wait until it says "running", then run this script again.' }
Write-Host '  Docker is running.'

Step 2 'Apply the code patches (skipped if already applied)'
Push-Location $RepoRoot
foreach($p in 'admin-company-picker.patch','onnx-confidence-and-latency.patch'){
  $file = Join-Path $RepoRoot $p
  if(-not (Test-Path $file)){ Write-Host "  $p not found in the repo root - skipped"; continue }
  Native { git apply --check $file *> $null }
  if($LASTEXITCODE -eq 0){ Native { git apply $file }; Write-Host "  applied $p" }
  else {
    Native { git apply --check -R $file *> $null }
    if($LASTEXITCODE -eq 0){ Write-Host "  $p is already applied" }
    else { Write-Host "  WARNING: $p does not apply cleanly. Try: git apply --ignore-whitespace $p" -ForegroundColor Yellow }
  }
}
Pop-Location

Step 3 'Prepare backend\.env (live data switch on)'
$envFile = Join-Path $Backend '.env'
if(-not (Test-Path $envFile)){ Copy-Item (Join-Path $Backend '.env.example') $envFile; Write-Host '  created backend\.env from .env.example' }
$lines = @(Get-Content $envFile)
if($lines -match '^APP_DATA_FETCH_ENABLED='){ $lines = $lines -replace '^APP_DATA_FETCH_ENABLED=.*','APP_DATA_FETCH_ENABLED=true' } else { $lines += 'APP_DATA_FETCH_ENABLED=true' }
Set-Content -Path $envFile -Value $lines
function EnvVal($k){ $m = $lines | Where-Object { $_ -match "^$k=" } | Select-Object -First 1; if($m){ $m -replace "^$k=",'' } else { '' } }
$AdminEmail    = EnvVal 'BOOTSTRAP_ADMIN_EMAIL';    if(-not $AdminEmail){ $AdminEmail = 'admin@test.local' }
$AdminPassword = EnvVal 'BOOTSTRAP_ADMIN_PASSWORD'; if(-not $AdminPassword){ $AdminPassword = 'TestAdmin123!' }
Write-Host '  APP_DATA_FETCH_ENABLED=true'

Step 4 'Build and start the backend (first build can take 10 to 20 minutes)'
Push-Location $Backend
if($Reset){ Native { docker compose down -v } }
if($SkipBuild){ Native { docker compose up -d } } else { Native { docker compose up -d --build } }
$code = $LASTEXITCODE
Pop-Location
if($code -ne 0){ throw 'docker compose failed. If you saw an SSL handshake error, build one service at a time (docker compose build auth-service, and so on), then run this script again with -SkipBuild.' }

Step 5 'Wait until the gateway and auth service answer'
$deadline = (Get-Date).AddMinutes(15); $ready = $false
while((Get-Date) -lt $deadline){
  try {
    $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    Invoke-RestMethod "$Gateway/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body (@{email=$AdminEmail;password=$AdminPassword}|ConvertTo-Json) -WebSession $s | Out-Null
    $ready = $true; break
  } catch { Write-Host '  waiting for the services to start...'; Start-Sleep 10 }
}
if(-not $ready){ throw 'The gateway or auth service did not answer within 8 minutes. Run: docker compose ps   and   docker compose logs --tail 50 auth-service   in the backend folder.' }
Write-Host '  Gateway and auth service are up; admin login works.'
try { $apps = Invoke-RestMethod 'http://127.0.0.1:8761/eureka/apps' -Headers @{Accept='application/json'}; Write-Host ("  Eureka: {0} applications registered (expect 7)" -f @($apps.applications.application).Count) } catch { Write-Host '  (Eureka list not readable - not a problem)' }

Step 6 'Start the frontend in a new window'
Start-Process powershell -ArgumentList '-NoExit','-Command',"Set-Location '$Frontend'; npm install; npm run dev"
Write-Host '  Frontend window opened. When it prints a Local URL, open http://localhost:5173'

Step 7 'Create the demo company and one account per role'
& (Join-Path $PSScriptRoot 'demo-setup.ps1') -Gateway $Gateway -AdminEmail $AdminEmail -AdminPassword $AdminPassword *>&1 | Tee-Object -FilePath (Join-Path $Evidence 'demo-setup.txt')

if(-not $SkipTests){
  Step 8 'Run the automated API tests (about 5 minutes)'
  Push-Location $Evidence
  & (Join-Path $PSScriptRoot 'a4-tests.ps1') -Gateway $Gateway -AdminEmail $AdminEmail -AdminPassword $AdminPassword -BackendDir $Backend
  Pop-Location
}

Step 9 'Collect extra evidence'
Push-Location $Backend
Native { docker compose ps } | Out-File (Join-Path $Evidence 'docker-compose-ps.txt')
Native { docker exec postgres psql -U postgres -d optigridza_db -c "select email, left(password,7) as hash_prefix from auth.users limit 3;" } | Out-File (Join-Path $Evidence 'bcrypt-check.txt')
Native { docker compose logs --tail 300 etl-service } | Select-String 'initializ|fetch complete|failed' | Out-File (Join-Path $Evidence 'etl-log.txt')
Native { docker compose logs --tail 400 prediction-service } | Select-String 'onnx|inference_us' | Select-Object -First 40 | Out-File (Join-Path $Evidence 'prediction-log.txt')
Pop-Location
Native { git -C $RepoRoot log -1 --format='%h %ad %s' --date=short } | Out-File (Join-Path $Evidence 'commit.txt')

Write-Host "`nDONE. Evidence folder: $Evidence" -ForegroundColor Green
Write-Host 'Next: do the UI tests (steps in the guide), then send me a4-tests.csv and your UI results.'
Write-Host 'Accounts: admin@test.local / TestAdmin123!   demo-manager@optigrid.local / DemoManager!2026   (full list in the guide)'

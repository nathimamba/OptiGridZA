# demo-setup.ps1 - creates (or re-uses) the demo companies and one account per role, with FIXED passwords.
# Safe to run more than once. Run from PowerShell after "docker compose up -d --build" in backend/.
# LOCAL DEMO ONLY: these passwords are for the prototype on your laptop. Do not reuse them anywhere else.
param(
  [string]$Gateway       = 'http://127.0.0.1:8080',
  [string]$AdminEmail    = 'admin@test.local',
  [string]$AdminPassword = 'TestAdmin123!'
)
$ErrorActionPreference = 'Stop'
function New-S { New-Object Microsoft.PowerShell.Commands.WebRequestSession }
function Api($m,$p,$b,$s){
  $r = @{Uri="$Gateway$p";Method=$m;ContentType='application/json'}
  if($s){$r.WebSession=$s}
  if($null -ne $b){$r.Body=($b|ConvertTo-Json -Depth 6)}
  Invoke-RestMethod @r
}
function Flat($x){ $o = @(); foreach($i in @($x)){ $o += $i }; $o }   # same result in Windows PowerShell 5.1 and PowerShell 7
function Status($e){ if($e.Exception.Response){ [int]$e.Exception.Response.StatusCode } else { 0 } }

$admin = New-S

# Bootstrapping: Try to register the admin account first in case the database is completely empty
try {
    Api POST /api/v1/auth/register @{
        firstName = 'System'
        lastName  = 'Administrator'
        email     = $AdminEmail
        password  = $AdminPassword
        role      = 'SYSTEM_ADMIN'
    } $null | Out-Null
    Write-Host "Admin account initialized successfully." -ForegroundColor Green
} catch {
    if ((Status $_) -eq 409) {
        Write-Host "Admin account already exists, skipping initialization step." -ForegroundColor Yellow
    } else {
        Write-Host "Admin auto-registration skipped: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Now safely log in using the administrator credentials
Api POST /api/v1/auth/login @{email=$AdminEmail;password=$AdminPassword} $admin | Out-Null

function Ensure-Company($name,$address,$lat,$lon,$mail){
  $existing = (Flat (Api GET /api/v1/companies $null $admin)) | Where-Object { $_.name -eq $name } | Select-Object -First 1
  if($existing){ Write-Host "company exists : $name"; return $existing }
  $c = Api POST /api/v1/companies @{name=$name;address=$address;industryType='EDUCATION_DEMO';contactEmail=$mail;contactPhone='0000000000';latitude=$lat;longitude=$lon} $admin
  Write-Host "company created: $name"; return $c
}
$coA = Ensure-Company 'OptiGrid School Demo' 'Pretoria, Gauteng, South Africa' -25.7479 28.2293 'demo@optigrid.local'
$coB = Ensure-Company 'OptiGrid Other Demo'  'Durban, KwaZulu-Natal, South Africa' -29.8587 31.0218 'other@optigrid.local'
foreach($c in @($coA,$coB)){ try { Api POST /api/v1/simulation/battery/init @{companyId=$c.id} $admin | Out-Null } catch { } }

# Passwords meet the policy: at least 12 characters with upper, lower, digit and symbol.
$accounts = @(
  @{role='SYSTEM_ADMIN';    email=$AdminEmail;                         pw=$AdminPassword;       co=$null;  first='System';   last='Administrator'},
  @{role='ENERGY_MANAGER';  email='demo-manager@optigrid.local';       pw='DemoManager!2026';   co=$coA.id; first='Demo';     last='Manager'},
  @{role='BUSINESS_OWNER';  email='demo-owner@optigrid.local';         pw='DemoOwner!2026';     co=$coA.id; first='Demo';     last='Owner'},
  @{role='TECHNICIAN';      email='demo-technician@optigrid.local';    pw='DemoTech!2026';      co=$coA.id; first='Demo';     last='Technician'},
  @{role='VIEWER';          email='demo-viewer@optigrid.local';        pw='DemoViewer!2026';    co=$coA.id; first='Demo';     last='Viewer'},
  @{role='ENERGY_MANAGER';  email='demo-manager-b@optigrid.local';     pw='DemoManagerB!2026';  co=$coB.id; first='Other';    last='Manager'}
)
foreach($a in $accounts){
  if($a.role -eq 'SYSTEM_ADMIN'){ continue }
  try {
    Api POST /api/v1/auth/register @{firstName=$a.first;lastName=$a.last;email=$a.email;password=$a.pw;role=$a.role;companyId=$a.co} $admin | Out-Null
    Write-Host "user created   : $($a.email)"
  } catch {
    if((Status $_) -eq 409){ Write-Host "user exists    : $($a.email)" } else { throw }
  }
  Api POST /api/v1/auth/login @{email=$AdminEmail;password=$AdminPassword} $admin | Out-Null   # register may replace the admin cookie
}

Write-Host "`nVerifying every login..."
$rows = foreach($a in $accounts){
  $ok = 'FAIL'; try { $r = Api POST /api/v1/auth/login @{email=$a.email;password=$a.pw} (New-S); if($r.role -eq $a.role){ $ok='OK' } else { $ok="role is $($r.role)" } } catch { }
  [pscustomobject]@{Role=$a.role; Email=$a.email; Password=$a.pw; Company=$(if($a.co -eq $coA.id){'School Demo (A)'}elseif($a.co -eq $coB.id){'Other Demo (B)'}else{'none'}); Login=$ok}
}
$rows | Format-Table -AutoSize
"Company A id: $($coA.id)"
"Company B id: $($coB.id)"

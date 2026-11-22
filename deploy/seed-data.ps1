# seed-data.ps1 - realistic demo data for local use (same as seed-data.sh). Safe to run again.
param([string]$Gateway='http://127.0.0.1:8080',[string]$AdminEmail='admin@test.local',[string]$AdminPassword='TestAdmin123!',[switch]$Force)
$ErrorActionPreference='Stop'
$s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
function Api($m,$p,$b){ $r=@{Uri="$Gateway$p";Method=$m;ContentType='application/json';WebSession=$s}; if($null -ne $b){$r.Body=($b|ConvertTo-Json -Depth 6)}; Invoke-RestMethod @r }
Api POST /api/v1/auth/login @{email=$AdminEmail;password=$AdminPassword} | Out-Null
if(-not $Force -and (@(Api GET /api/v1/companies $null) | Where-Object { $_.name -eq 'Pretoria Bakery' })){ Write-Host 'Seed data already present. Use -Force to run anyway.'; exit 0 }
function Company($name,$addr,$lat,$lon,$industry){
  $c = @(Api GET /api/v1/companies $null) | Where-Object { $_.name -eq $name } | Select-Object -First 1
  if(-not $c){ $c = Api POST /api/v1/companies @{name=$name;address=$addr;industryType=$industry;contactEmail='demo@optigrid.local';contactPhone='0000000000';latitude=$lat;longitude=$lon}; Write-Host "company created: $name" } else { Write-Host "company exists : $name" }
  $c.id }
function User($f,$l,$email,$pw,$role,$cid){ try { Api POST /api/v1/auth/register @{firstName=$f;lastName=$l;email=$email;password=$pw;role=$role;companyId=$cid} | Out-Null; Write-Host "  user created: $email" } catch { Write-Host "  user exists : $email" } }
function Run($cid,$action,$kwh,$mode){ Api POST /api/v1/simulation/run @{companyId=$cid;action=$action;kwh=$kwh;mode=$mode} | Out-Null; Write-Host "  $action $kwh kWh"; Start-Sleep 3 }
function FirstOpen($cid){ $t=@(Api GET "/api/v1/tickets/company/$cid" $null) | Where-Object { $_.status -eq 'OPEN' } | Select-Object -First 1; if($t){$t.id} }

$A   = Company 'OptiGrid School Demo' 'Pretoria' -25.75 28.23 'EDUCATION_DEMO'
$BAK = Company 'Pretoria Bakery' 'Pretoria' -25.74 28.19 'RETAIL'
$COLD= Company 'Durban Cold Storage' 'Durban' -29.86 31.02 'LIGHT_MANUFACTURING'
$CLI = Company 'Cape Town Clinic' 'Cape Town' -33.92 18.42 'HEALTHCARE'

Write-Host 'Users'
User 'Bakery' 'Manager' 'bakery-manager@optigrid.local' 'BakeryMgr!2026' 'ENERGY_MANAGER' $BAK
User 'Cold' 'Manager' 'cold-manager@optigrid.local' 'ColdMgr!2026' 'ENERGY_MANAGER' $COLD
User 'Cold' 'Technician' 'cold-tech@optigrid.local' 'ColdTech!2026' 'TECHNICIAN' $COLD
User 'Clinic' 'Manager' 'clinic-manager@optigrid.local' 'ClinicMgr!2026' 'ENERGY_MANAGER' $CLI

Write-Host 'School Demo: history, then reset to 100%'
Run $A 'DISCHARGE' 3 'HYBRID'; Run $A 'CHARGE' 2 'HYBRID'; Run $A 'DISCHARGE' 8 'HYBRID'
$t = FirstOpen $A
if($t){ Api PUT "/api/v1/tickets/$t/assign" @{email='demo-technician@optigrid.local'} | Out-Null; Api PUT "/api/v1/tickets/$t/status" @{status='RESOLVED';resolutionNotes='Battery inspected, charge restored'} | Out-Null; Write-Host "  ticket $t resolved" }
foreach($a in @(Api GET "/api/v1/notifications/alerts/$A" $null) | Where-Object { -not $_.acknowledged }){ Api PUT "/api/v1/notifications/alerts/$($a.id)/acknowledge" $null | Out-Null }
Run $A 'CHARGE' 10 'HYBRID'; Run $A 'CHARGE' 10 'HYBRID'

Write-Host 'Pretoria Bakery: partly discharged'
Run $BAK 'DISCHARGE' 5 'HYBRID'

Write-Host 'Durban Cold Storage: two critical events, one ticket in progress, one open'
Run $COLD 'DISCHARGE' 8 'HYBRID'; Run $COLD 'CHARGE' 4 'HYBRID'; Run $COLD 'DISCHARGE' 7 'HYBRID'
$t = FirstOpen $COLD
if($t){ Api PUT "/api/v1/tickets/$t/assign" @{email='cold-tech@optigrid.local'} | Out-Null; Write-Host "  ticket $t in progress" }

Write-Host 'Prediction records'
foreach($id in @($A,$BAK,$COLD,$CLI)){ $r = Api GET "/api/v1/prediction/recommend/$id" $null; Write-Host ("  {0} {1} {2}" -f $r.action,$r.confidence,$r.operatingMode) }
Write-Host 'Done. New logins: bakery-manager / cold-manager / cold-tech / clinic-manager @optigrid.local'

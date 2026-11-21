# a4-tests.ps1 - automated evidence run for Assignment 4 (API level, same endpoints the UI calls).
# Test IDs A4-nn are new for Assignment 4; the Assignment 3 IDs (TC-nn, PT-nn) are referenced in the comments.
# Creates its own test companies/users (unique per run), logs in once per user, records timings.
# Output: a4-evidence.txt (transcript), a4-tests.csv (results table), summary printed at the end.
param(
  [string]$Gateway       = 'http://127.0.0.1:8080',
  [string]$AdminEmail    = 'admin@test.local',
  [string]$AdminPassword = 'TestAdmin123!',
  [string]$BackendDir    = ''      # optional: path to the backend folder, to read ONNX inference timings from the Docker logs
)
$ErrorActionPreference = 'Stop'
Start-Transcript -Path .\a4-evidence.txt -Force | Out-Null
$run = Get-Date -Format 'HHmmss'
function New-S { New-Object Microsoft.PowerShell.Commands.WebRequestSession }
function Api($m,$p,$b,$s){
  $r = @{Uri="$Gateway$p";Method=$m;ContentType='application/json'}
  if($s){$r.WebSession=$s}
  if($null -ne $b){$r.Body=($b|ConvertTo-Json -Depth 6)}
  # retry transient gateway/server errors (5xx, no response) up to 4 times
  for($try = 1; $try -le 4; $try++){
    try { return Invoke-RestMethod @r }
    catch {
      $c = if($_.Exception.Response){ [int]$_.Exception.Response.StatusCode } else { 0 }
      if((($c -ge 500) -or ($c -eq 0)) -and $try -lt 4){ Start-Sleep (5 * $try); continue }
      throw
    }
  }
}
function Code($m,$p,$b,$s){ try { Api $m $p $b $s | Out-Null; 200 } catch { if($_.Exception.Response){ [int]$_.Exception.Response.StatusCode } else { 'ERR' } } }
function Flat($x){ $o = @(); foreach($i in @($x)){ $o += $i }; $o }   # same result in Windows PowerShell 5.1 and PowerShell 7
function RawCode($url){ try { (Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10).StatusCode } catch { if($_.Exception.Response){ [int]$_.Exception.Response.StatusCode } else { 'ERR' } } }
function Login($email,$pw){ $s = New-S; Api POST /api/v1/auth/login @{email=$email;password=$pw} $s | Out-Null; $s }

$results = New-Object System.Collections.ArrayList
function T($id,$cat,$name,$expected,[scriptblock]$act){
  $sw = [Diagnostics.Stopwatch]::StartNew()
  try { $actual = & $act } catch { $actual = "EXC: " + $_.Exception.Message }
  $sw.Stop()
  $status = if("$actual" -eq "$expected"){'Pass'}else{'CHECK'}
  [void]$results.Add([pscustomobject]@{ID=$id;Category=$cat;Test=$name;Expected=$expected;Actual=$actual;Ms=[int]$sw.ElapsedMilliseconds;Status=$status})
}

# wait until every routed service answers through the gateway (anything except 503 counts as reachable)
$probe = New-S; Api POST /api/v1/auth/login @{email=$AdminEmail;password=$AdminPassword} $probe | Out-Null
foreach($p in '/api/v1/companies','/api/v1/simulation/battery/health/x','/api/v1/prediction/model-info','/api/v1/notifications/alerts/x','/api/v1/etl/tariff/current'){
  for($k=0; $k -lt 60; $k++){
    $c = try { (Invoke-WebRequest "$Gateway$p" -WebSession $probe -UseBasicParsing).StatusCode } catch { if($_.Exception.Response){ [int]$_.Exception.Response.StatusCode } else { 0 } }
    if($c -ne 503 -and $c -ne 0){ break }
    Write-Host "  waiting for $p (status $c)..."; Start-Sleep 10
  }
}

# ---------- setup ----------
$loginMs = [int](Measure-Command { $admin = Login $AdminEmail $AdminPassword }).TotalMilliseconds
$coA = Api POST /api/v1/companies @{name="A4 Co A $run";address='Pretoria';industryType='RETAIL';contactEmail='a@test.local';contactPhone='0000000000';latitude=-25.7;longitude=28.2} $admin
$coB = Api POST /api/v1/companies @{name="A4 Co B $run";address='Durban';industryType='RETAIL';contactEmail='b@test.local';contactPhone='0000000000';latitude=-29.8;longitude=31.0} $admin
foreach($c in @($coA,$coB)){ try { Api POST /api/v1/simulation/battery/init @{companyId=$c.id} $admin | Out-Null } catch {} }
$mgrA = New-S; Api POST /api/v1/auth/register @{firstName='M';lastName='A';email="mgr.a.$run@test.local";password='MgrA!Pass12345';companyId=$coA.id} $mgrA | Out-Null
$mgrB = New-S; Api POST /api/v1/auth/register @{firstName='M';lastName='B';email="mgr.b.$run@test.local";password='MgrB!Pass12345';companyId=$coB.id} $mgrB | Out-Null
Api POST /api/v1/auth/register @{firstName='V';lastName='Iewer';email="viewer.$run@test.local";password='Viewer!Pass123';role='VIEWER';companyId=$coA.id} $admin | Out-Null
Api POST /api/v1/auth/register @{firstName='T';lastName='Ech';email="tech.$run@test.local";password='Tech!Pass12345';role='TECHNICIAN'} $admin | Out-Null
$admin = Login $AdminEmail $AdminPassword      # register may replace the cookie, so sign the admin in again
$viewer = Login "viewer.$run@test.local" 'Viewer!Pass123'
$actions = 'CHARGE','DISCHARGE','HOLD','SOLAR_PRIORITY'
$initSoc = (Api GET "/api/v1/simulation/battery/health/$($coA.id)" $null $mgrA).soc

# ---------- tests ----------
T 'A4-01' 'Main workflow'     'Admin login'                              200 { Code POST /api/v1/auth/login @{email=$AdminEmail;password=$AdminPassword} (New-S) }
T 'A4-21' 'Invalid input'     'Wrong password'                           401 { Code POST /api/v1/auth/login @{email=$AdminEmail;password='wrong'} (New-S) }
T 'A4-22' 'Access control'    'Protected endpoint without session'       401 { Code GET "/api/v1/prediction/recommend/$($coA.id)" $null (New-S) }
T 'A4-06' 'Invalid input'     'Anonymous register asking SYSTEM_ADMIN'   'ENERGY_MANAGER' { (Api POST /api/v1/auth/register @{firstName='X';lastName='Y';email="sneaky.$run@test.local";password='Sneaky!Pass123';role='SYSTEM_ADMIN'} (New-S)).role }
T 'A4-24' 'Invalid input'     'Register with invalid email'              400 { Code POST /api/v1/auth/register @{firstName='A';lastName='B';email='not-an-email';password='Valid!Pass123'} (New-S) }
T 'A4-25' 'Invalid input'     'Register duplicate email'                 409 { Code POST /api/v1/auth/register @{firstName='A';lastName='B';email=$AdminEmail;password='Valid!Pass123'} (New-S) }
T 'A4-07' 'Access control'    'Viewer runs simulation'                   403 { Code POST /api/v1/simulation/run @{companyId=$coA.id;action='CHARGE';kwh=1;mode='HYBRID'} $viewer }
T 'A4-11' 'Tenant isolation'  'Manager A reads own battery health'       200 { Code GET "/api/v1/simulation/battery/health/$($coA.id)" $null $mgrA }
T 'A4-12' 'Tenant isolation'  'Manager A reads Company B battery health' 403 { Code GET "/api/v1/simulation/battery/health/$($coB.id)" $null $mgrA }
T 'A4-13' 'Tenant isolation'  'Manager A runs simulation on Company B'   403 { Code POST /api/v1/simulation/run @{companyId=$coB.id;action='CHARGE';kwh=1;mode='HYBRID'} $mgrA }
T 'A4-14' 'Tenant isolation'  'Manager A reads Company B alerts'         403 { Code GET "/api/v1/notifications/alerts/$($coB.id)" $null $mgrA }
T 'A4-08' 'Emerging tech'     'model-info reports modelLoaded'           'True' { [string](Api GET /api/v1/prediction/model-info $null $admin).modelLoaded }
T 'A4-09' 'Emerging tech'     'Recommendation returns a valid action'    'valid action' { $r = Api GET "/api/v1/prediction/recommend/$($coA.id)" $null $mgrA; if($actions -contains $r.action){'valid action'}else{$r.action} }
T 'A4-18' 'Data/API'          'Set demo load-shedding stage 4'           '4' { [string](Api POST "/api/v1/etl/demo/grid-status?stage=4" $null $admin).loadSheddingStage }
T 'A4-19' 'Emerging tech'     'Recommendation at stage 4: mode'          'OFF_GRID' { (Api GET "/api/v1/prediction/recommend/$($coA.id)" $null $mgrA).operatingMode }
T 'A4-23' 'Invalid input'     'Demo stage 99 is clamped'                 '8' { [string](Api POST "/api/v1/etl/demo/grid-status?stage=99" $null $admin).loadSheddingStage }
T 'A4-20' 'Data/API'          'Reset demo stage to 0'                    '0' { [string](Api POST "/api/v1/etl/demo/grid-status?stage=0" $null $admin).loadSheddingStage }
T 'A4-16' 'Data/API'          'Weather is live (not the fallback)'       'live' { $w = Api GET /api/v1/etl/weather $null $admin; if($w.weatherCondition -eq 'Unknown'){'FALLBACK'}else{'live'} }
T 'A4-17' 'Data/API'          'Tariff is not the default fallback'       'not default' { $t = Api GET /api/v1/etl/tariff/current $null $admin; if($t.periodStartHour -eq 0 -and $t.periodEndHour -eq 23){'DEFAULT'}else{'not default'} }

# Assignment 3 FR06 expects a new battery at 100% SOC (10 kWh, HYBRID)
T 'A4-31' 'Main workflow'     'New company battery initial SOC (A3 FR06 says 100)' 100 { [int]$initSoc }
# main workflow success path: discharge 8 kWh from 100% (10 kWh battery) -> about 11% SoC (CRITICAL, below the 20% alert threshold)
T 'A4-28' 'Main workflow'     'Manager discharges 8 kWh: health CRITICAL' 'CRITICAL' { Api POST /api/v1/simulation/run @{companyId=$coA.id;action='DISCHARGE';kwh=8;mode='HYBRID'} $mgrA | Out-Null; (Api GET "/api/v1/simulation/battery/health/$($coA.id)" $null $mgrA).healthStatus }
T 'A4-29' 'Main workflow'     'LOW_SOC alert created automatically (manager view)' 'alert created' { $n = 0; for($k=0; $k -lt 6 -and $n -eq 0; $k++){ if($k -gt 0){ Start-Sleep 3 }; $a = @(Api GET "/api/v1/notifications/alerts/$($coA.id)" $null $mgrA); $n = @($a | Where-Object { $_.alertType -eq 'LOW_SOC' }).Count; Write-Host "A4-29 try ${k}: alerts=$($a.Count) lowsoc=$n company=$($coA.id)" }; if($n -gt 0){'alert created'}else{'none'} }
T 'A4-30' 'Main workflow'     'Maintenance ticket created automatically (manager view)' 'ticket created' { $n = 0; for($k=0; $k -lt 6 -and $n -eq 0; $k++){ if($k -gt 0){ Start-Sleep 3 }; $t = @(Api GET "/api/v1/tickets/company/$($coA.id)" $null $mgrA); $n = @($t | Where-Object { $_ }).Count; Write-Host "A4-30 try ${k}: tickets=$n" }; if($n -gt 0){'ticket created'}else{'none'} }
T 'A4-29b' 'Main workflow'    'LOW_SOC alert exists (admin read-back)'   'alert created' { $a = @(Api GET "/api/v1/notifications/alerts/$($coA.id)" $null $admin); if(@($a | Where-Object { $_.alertType -eq 'LOW_SOC' }).Count -gt 0){'alert created'}else{'none'} }
T 'A4-30b' 'Main workflow'    'Ticket exists (admin read-back)'          'ticket created' { $t = @(Api GET "/api/v1/tickets/company/$($coA.id)" $null $admin); if(@($t | Where-Object { $_ }).Count -gt 0){'ticket created'}else{'none'} }
T 'A4-26' 'Invalid input'     'Recommendation for unknown company'       404 { Code GET "/api/v1/prediction/recommend/00000000-0000-0000-0000-000000000000" $null $admin }
T 'A4-27' 'Invalid input'     'Negative kWh rejected by the server'      400 { Code POST /api/v1/simulation/run @{companyId=$coB.id;action='CHARGE';kwh=-2;mode='HYBRID'} $mgrB }
T 'A4-34' 'Emerging tech'     'After the discharge (SOC about 11%, stage 0) the model recommends CHARGE' 'CHARGE' { (Api GET "/api/v1/prediction/recommend/$($coA.id)" $null $mgrA).action }
T 'A4-04' 'Data/API'          'Assign a technician to a company (admin)'  200 { Code POST "/api/v1/companies/$($coA.id)/users" @{email="tech.$run@test.local";role='TECHNICIAN'} $admin }

T 'A4-32' 'Emerging tech'     'Confidence comes from the model, not the 0.6 default' 'model value' { $r = Api GET "/api/v1/prediction/recommend/$($coA.id)" $null $mgrA; if([math]::Abs([double]$r.confidence - 0.6) -lt 0.0001){'DEFAULT 0.6'}else{'model value'} }
T 'A4-33' 'Data/API'          'Manual ETL fetch gives a fresh timestamp (A3 TC-04)' 'new timestamp' { $b=(Api GET /api/v1/etl/grid-status $null $admin).fetchedAt; Api POST /api/v1/etl/fetch $null $admin | Out-Null; Start-Sleep 2; $a=(Api GET /api/v1/etl/grid-status $null $admin).fetchedAt; if($a -ne $b){'new timestamp'}else{'unchanged'} }
# A3 TC-12 / NFR04: /actuator/health on every service (ports are published on 127.0.0.1)
$i = 40
foreach($svc in @(@('gateway',8080),@('auth',8081),@('company',8082),@('etl',8083),@('prediction',8084),@('simulation',8085),@('notification',8086),@('eureka',8761))){
  $i++; $port = $svc[1]
  T ("A4-$i") 'Supportability' ("/actuator/health on " + $svc[0] + " (:$port)") 200 { RawCode "http://127.0.0.1:$port/actuator/health" }
}

# ---------- timings ----------
$cold  = [int](Measure-Command { Api GET "/api/v1/prediction/recommend/$($coB.id)" $null $mgrB | Out-Null }).TotalMilliseconds
$times = 1..100 | ForEach-Object { (Measure-Command { Api GET "/api/v1/prediction/recommend/$($coB.id)" $null $mgrB | Out-Null }).TotalMilliseconds }
$sorted = $times | Sort-Object
$avg = [int]($times | Measure-Object -Average).Average
$p95 = [int]$sorted[94]
$healthTimes = 1..20 | ForEach-Object { (Measure-Command { Api GET "/api/v1/simulation/battery/health/$($coB.id)" $null $mgrB | Out-Null }).TotalMilliseconds }
$hAvg = [int]($healthTimes | Measure-Object -Average).Average

$results | Format-Table ID,Category,Test,Expected,Actual,Ms,Status -AutoSize -Wrap
$results | Export-Csv .\a4-tests.csv -NoTypeInformation
$pass = ($results | Where-Object Status -eq 'Pass').Count
"`nPASS $pass of $($results.Count); CHECK $($results.Count - $pass) (open each CHECK row: a different code can still be correct behaviour, or it is a finding)"
"Login: $loginMs ms | Recommendation cold: $cold ms | warm avg: $avg ms | p95: $p95 ms (100 calls) | battery health avg: $hAvg ms"

# ONNX inference timings (A3 NFR-01/06 and PT-01): needs the admin-confidence-latency patch applied to prediction-service
if($BackendDir){
  Push-Location $BackendDir
  $us = docker compose logs --tail 500 prediction-service 2>$null | Select-String 'inference_us=(\d+)' | ForEach-Object { [int]$_.Matches[0].Groups[1].Value }
  Pop-Location
  if($us){ $u = $us | Sort-Object; "ONNX inference: n=$($u.Count), median $($u[[int]($u.Count/2)]) us, p95 $($u[[int]($u.Count*0.95)]) us, max $($u[-1]) us (target 50000 us)" }
  else { "No inference_us lines found - apply onnx-confidence-and-latency.patch and rebuild prediction-service." }
}
Stop-Transcript | Out-Null
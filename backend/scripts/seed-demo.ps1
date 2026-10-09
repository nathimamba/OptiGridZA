$ErrorActionPreference = 'Stop'

$gateway = 'http://127.0.0.1:8080'
$adminEmail = 'demo-admin@optigrid.local'
$managerEmail = 'demo-manager@optigrid.local'
$companyName = 'OptiGrid School Demo'

function New-DemoPassword {
    $bytes = New-Object byte[] 24
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $random.GetBytes($bytes)
    } finally {
        $random.Dispose()
    }

    return [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function Invoke-DemoApi {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [object]$Body,
        [string]$Token
    )

    $request = @{
        Uri         = "$gateway$Path"
        Method      = $Method
        ContentType = 'application/json'
        ErrorAction = 'Stop'
    }
    if ($Token) {
        $request.Headers = @{ Authorization = "Bearer $Token" }
    }
    if ($null -ne $Body) {
        $request.Body = ConvertTo-Json $Body -Depth 8
    }

    try {
        return Invoke-RestMethod @request
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 409 -or $statusCode -eq 400) {
            throw "Demo seeding stopped at $Method $Path. A demo account or company may already exist. Use a fresh local database or remove the previous demo records."
        }
        throw "Demo seeding failed at $Method $Path`: $($_.Exception.Message)"
    }
}

$adminPassword = New-DemoPassword
$managerPassword = New-DemoPassword

$admin = Invoke-DemoApi -Method POST -Path '/api/v1/auth/register' -Body @{
    firstName = 'School'
    lastName  = 'Administrator'
    email     = $adminEmail
    password  = $adminPassword
    role      = 'SYSTEM_ADMIN'
}

$company = Invoke-DemoApi -Method POST -Path '/api/v1/companies' -Token $admin.token -Body @{
    name         = $companyName
    address      = 'Pretoria, Gauteng, South Africa'
    industryType = 'EDUCATION_DEMO'
    contactEmail = $managerEmail
    contactPhone = '0000000000'
    latitude     = -25.7479
    longitude    = 28.2293
}

$manager = Invoke-DemoApi -Method POST -Path '/api/v1/auth/register' -Body @{
    firstName = 'School'
    lastName  = 'Energy Manager'
    email     = $managerEmail
    password  = $managerPassword
    role      = 'ENERGY_MANAGER'
    companyId = $company.id
}

$null = Invoke-DemoApi -Method POST -Path '/api/v1/simulation/battery/init' -Token $manager.token -Body @{
    companyId = $company.id
}

$null = Invoke-DemoApi -Method POST -Path '/api/v1/simulation/run' -Token $manager.token -Body @{
    companyId = $company.id
    action    = 'DISCHARGE'
    kwh       = 3.5
    mode      = 'HYBRID'
}
$null = Invoke-DemoApi -Method POST -Path '/api/v1/simulation/run' -Token $manager.token -Body @{
    companyId = $company.id
    action    = 'CHARGE'
    kwh       = 4.5
    mode      = 'SOLAR_PRIORITY'
}

1..3 | ForEach-Object {
    $null = Invoke-DemoApi -Method GET -Path "/api/v1/prediction/recommend/$($company.id)" -Token $manager.token
}

Write-Output 'Local synthetic demo data created.'
Write-Output "Company: $($company.name) ($($company.id))"
Write-Output "Admin login: $adminEmail"
Write-Output "Admin password: $adminPassword"
Write-Output "Energy manager login: $managerEmail"
Write-Output "Energy manager password: $managerPassword"
Write-Output 'Use the energy manager account to present the company dashboard, battery, prediction history, and generated alert/ticket.'

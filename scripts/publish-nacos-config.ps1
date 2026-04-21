$ErrorActionPreference = "Stop"

$NacosBaseUrl = $env:NACOS_BASE_URL
if ([string]::IsNullOrWhiteSpace($NacosBaseUrl)) {
    $NacosBaseUrl = "http://localhost:8848"
}

$Group = $env:NACOS_GROUP
if ([string]::IsNullOrWhiteSpace($Group)) {
    $Group = "HOMEWORK5_GROUP"
}

$ConfigDir = Join-Path $PSScriptRoot "..\\docker\\nacos\\config"
$files = Get-ChildItem -Path $ConfigDir -Filter "*.yaml" | Sort-Object Name

foreach ($file in $files) {
    $dataId = $file.Name
    $content = Get-Content -Path $file.FullName -Raw
    Write-Host "Publishing $dataId to Nacos group $Group"
    Invoke-RestMethod `
        -Method Post `
        -Uri "$NacosBaseUrl/nacos/v1/cs/configs" `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{
            dataId = $dataId
            group = $Group
            content = $content
            type = "yaml"
        } | Out-Null
}

Write-Host "Nacos config publish finished."

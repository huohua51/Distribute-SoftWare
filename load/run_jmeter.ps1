$ErrorActionPreference = "Stop"

$JMeterHome = $env:JMETER_HOME
if ([string]::IsNullOrWhiteSpace($JMeterHome)) {
    throw "请先设置 JMETER_HOME 环境变量。"
}

$ResultsDir = Join-Path $PSScriptRoot "results"
New-Item -ItemType Directory -Force -Path $ResultsDir | Out-Null

& "$JMeterHome\\bin\\jmeter.bat" `
    -n `
    -t "$PSScriptRoot\\jmeter_test_plan.jmx" `
    -l "$ResultsDir\\result.jtl" `
    -e `
    -o "$ResultsDir\\report"

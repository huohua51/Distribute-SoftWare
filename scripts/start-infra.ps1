docker compose up -d mysql redis kafka shardingsphere-proxy nacos sentinel-dashboard
& "$PSScriptRoot\\publish-nacos-config.ps1"

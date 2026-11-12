#Requires -Version 7.0
<#
.SYNOPSIS
Menyiapkan Lemonade Server untuk Empati: model, alias, dan pemeriksaan kesiapan.

.DESCRIPTION
Menarik model yang dipakai Empati, memasang empat alias kontrak, lalu memastikan
server menjawab di port 13305. Jalankan dengan -DryRun untuk melihat perintah
tanpa mengeksekusi apa pun.

.PARAMETER DryRun
Cetak perintah saja, tanpa memanggil lemonade.

.PARAMETER ExposeLan
Set host=0.0.0.0 supaya perangkat fisik bisa terhubung. Membuka /internal/* ke
jaringan, jadi pakai hanya di jaringan tepercaya dan set LEMONADE_API_KEY.

.PARAMETER SkipPull
Lewati unduhan model, misalnya ketika model sudah ada di mesin demo.
#>
[CmdletBinding()]
param(
    [switch]$DryRun,
    [switch]$ExposeLan,
    [switch]$SkipPull
)

$ErrorActionPreference = 'Stop'

$llm = 'Qwen3.5-4B-MTP-GGUF'
$embed = 'nomic-embed-text-v1-GGUF'
$stt = 'Whisper-Large-v3-Turbo'
$tts = 'kokoro-v1'

$aliases = [ordered]@{
    'empati-llm'   = $llm
    'empati-embed' = $embed
    'empati-stt'   = $stt
    'empati-tts'   = $tts
}

function Invoke-Step {
    param(
        [Parameter(Mandatory)][string]$Description,
        [Parameter(Mandatory)][string[]]$Command,
        [Parameter(Mandatory)][scriptblock]$Action
    )

    Write-Host ""
    Write-Host "== $Description" -ForegroundColor Cyan
    Write-Host ("   " + ($Command -join ' ')) -ForegroundColor DarkGray
    if ($DryRun) { return }
    & $Action
}

if (-not $DryRun) {
    $lemonade = Get-Command lemonade -ErrorAction SilentlyContinue
    if (-not $lemonade) {
        throw "Perintah 'lemonade' tidak ada di PATH. Pasang Lemonade Server dulu, lalu jalankan skrip ini lagi."
    }
}

if ($ExposeLan) {
    Invoke-Step -Description 'Membuka server ke jaringan lokal' `
        -Command @('lemonade', 'config', 'set', 'host=0.0.0.0') `
        -Action { lemonade config set host=0.0.0.0 }
}

if (-not $SkipPull) {
    foreach ($model in @($llm, $embed, $stt, $tts)) {
        Invoke-Step -Description "Menarik model $model" `
            -Command @('lemonade', 'pull', $model) `
            -Action { lemonade pull $model }
    }
}

foreach ($alias in $aliases.Keys) {
    $target = $aliases[$alias]
    Invoke-Step -Description "Memasang alias $alias" `
        -Command @('lemonade', 'alias', 'add', $alias, $target) `
        -Action { lemonade alias add $alias $target }
}

Invoke-Step -Description 'Memeriksa /v1/models' `
    -Command @('curl', 'http://localhost:13305/v1/models') `
    -Action {
        $response = Invoke-RestMethod -Uri 'http://localhost:13305/v1/models' -TimeoutSec 15
        $ids = $response.data | ForEach-Object { $_.id }
        $missing = @($aliases.Keys | Where-Object { $_ -notin $ids })
        if ($missing.Count -gt 0) {
            throw "Alias belum terdaftar: $($missing -join ', ')"
        }
        Write-Host "   Alias siap: $($aliases.Keys -join ', ')" -ForegroundColor Green
    }

Write-Host ""
if ($DryRun) {
    Write-Host 'DryRun selesai. Tidak ada perintah yang dijalankan.' -ForegroundColor Yellow
} else {
    Write-Host 'Lemonade siap dipakai Empati di port 13305.' -ForegroundColor Green
}

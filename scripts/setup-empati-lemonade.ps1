#Requires -Version 7.0
<#
.SYNOPSIS
Menyiapkan Lemonade Server untuk Empati: host, model, alias, dan benchmark.

.DESCRIPTION
Mengatur host server, menarik model LLM utama dan cadangan, model embedding,
Whisper untuk transkripsi, serta Kokoro untuk suara. Setelah itu memasang lima
alias kontrak, memeriksa /v1/models, lalu menjalankan benchmark.

Jalankan dengan -DryRun untuk melihat perintah tanpa mengeksekusi apa pun.

.PARAMETER DryRun
Cetak perintah saja, tanpa memanggil lemonade.

.PARAMETER LocalOnly
Pertahankan host=127.0.0.1. Tanpa switch ini, server dibuka di 0.0.0.0 agar
perangkat fisik bisa terhubung.

.PARAMETER SkipPull
Lewati unduhan model, misalnya ketika model sudah ada di mesin demo.

.PARAMETER SkipBench
Lewati benchmark di akhir penyiapan.

.PARAMETER BaseUrl
Alamat server yang diperiksa dan diukur. Bawaan http://localhost:13305.
#>
[CmdletBinding()]
param(
    [switch]$DryRun,
    [switch]$LocalOnly,
    [switch]$SkipPull,
    [switch]$SkipBench,
    [string]$BaseUrl = 'http://localhost:13305'
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

$llm = 'Qwen3-4B-GGUF'
$llmBackup = 'Qwen3-1.7B-GGUF'
$embed = 'nomic-embed-text-v1-GGUF'
$stt = 'Whisper-Large-v3-Turbo'
$tts = 'kokoro-v1'

$aliases = [ordered]@{
    'empati-llm'        = $llm
    'empati-llm-backup' = $llmBackup
    'empati-embed'      = $embed
    'empati-stt'        = $stt
    'empati-tts'        = $tts
}

$bindHost = if ($LocalOnly) { '127.0.0.1' } else { '0.0.0.0' }

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

Invoke-Step -Description "Mengatur host Lemonade ke $bindHost" `
    -Command @('lemonade', 'config', 'set', "host=$bindHost") `
    -Action { lemonade config set "host=$bindHost" }

if (-not $LocalOnly) {
    Write-Host "   Server terbuka ke LAN. Batasi akses jaringan dan set LEMONADE_API_KEY di server." -ForegroundColor Yellow
}

if (-not $SkipPull) {
    foreach ($model in @($llm, $llmBackup, $embed, $stt, $tts)) {
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

Invoke-Step -Description "Memeriksa $BaseUrl/v1/models" `
    -Command @('curl', "$BaseUrl/v1/models") `
    -Action {
        $response = Invoke-RestMethod -Uri "$BaseUrl/v1/models" -TimeoutSec 15
        $ids = $response.data | ForEach-Object { $_.id }
        $missing = @($aliases.Keys | Where-Object { $_ -notin $ids })
        if ($missing.Count -gt 0) {
            throw "Alias belum terdaftar: $($missing -join ', ')"
        }
        Write-Host "   Alias siap: $($aliases.Keys -join ', ')" -ForegroundColor Green
    }

if (-not $SkipBench) {
    Invoke-Step -Description 'Menjalankan benchmark bawaan Lemonade' `
        -Command @('lemonade', 'bench') `
        -Action {
            try {
                lemonade bench
            } catch {
                Write-Warning "lemonade bench tidak tersedia di mesin ini. Memakai scripts/bench-empati.ps1."
            }
        }

    Invoke-Step -Description 'Mengukur endpoint Empati' `
        -Command @('pwsh', '-File', (Join-Path $scriptRoot 'bench-empati.ps1'), '-BaseUrl', $BaseUrl) `
        -Action {
            & (Join-Path $scriptRoot 'bench-empati.ps1') -BaseUrl $BaseUrl
        }
}

Write-Host ""
if ($DryRun) {
    Write-Host 'DryRun selesai. Tidak ada perintah yang dijalankan.' -ForegroundColor Yellow
} else {
    Write-Host "Lemonade siap dipakai Empati di $BaseUrl." -ForegroundColor Green
}

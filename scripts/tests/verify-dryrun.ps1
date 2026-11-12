#Requires -Version 7.0
<#
.SYNOPSIS
Memeriksa keluaran DryRun skrip penyiapan dan benchmark Empati.

.DESCRIPTION
Menjalankan setup-empati-lemonade.ps1 dan bench-empati.ps1 dengan -DryRun,
lalu memastikan host, model, alias, benchmark, port 13305, dan ketiadaan jejak
Ollama sesuai kontrak. Dipakai sebagai pemeriksaan cepat sebelum demo.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

$setupOutput = & (Join-Path $scriptRoot 'setup-empati-lemonade.ps1') -DryRun 6>&1 | Out-String
$benchOutput = & (Join-Path $scriptRoot 'bench-empati.ps1') -DryRun 6>&1 | Out-String
$combined = $setupOutput + "`n" + $benchOutput

$failures = @()

foreach ($alias in @('empati-llm', 'empati-llm-backup', 'empati-embed', 'empati-stt', 'empati-tts')) {
    if ($combined -notmatch [regex]::Escape($alias)) {
        $failures += "alias $alias tidak muncul di keluaran DryRun"
    }
}

if ($setupOutput -notmatch 'host=0\.0\.0\.0') {
    $failures += 'setup tidak mengatur host=0.0.0.0'
}

foreach ($model in @('Qwen3-4B-GGUF', 'Qwen3-1.7B-GGUF', 'nomic-embed-text-v1-GGUF', 'Whisper-Large-v3-Turbo', 'kokoro-v1')) {
    if ($setupOutput -notmatch [regex]::Escape($model)) {
        $failures += "model $model tidak disebut saat penyiapan"
    }
}

if ($setupOutput -notmatch 'lemonade bench') {
    $failures += 'setup tidak menjalankan lemonade bench'
}

if ($combined -notmatch '13305') {
    $failures += 'port 13305 tidak muncul di keluaran DryRun'
}

if ($combined -match '11434' -or $combined -match '/api/chat') {
    $failures += 'keluaran DryRun masih menyebut Ollama (11434 atau /api/chat)'
}

if ($setupOutput -notmatch 'lemonade alias add') {
    $failures += 'setup tidak mencetak perintah lemonade alias add'
}

if ($benchOutput -notmatch 'chat' -or $benchOutput -notmatch 'embeddings') {
    $failures += 'benchmark tidak mencakup chat dan embeddings'
}

if ($failures.Count -gt 0) {
    Write-Host 'Pemeriksaan DryRun gagal:' -ForegroundColor Red
    $failures | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

Write-Host 'Pemeriksaan DryRun lulus: host, lima alias, model, bench, port 13305, tanpa jejak Ollama.' -ForegroundColor Green

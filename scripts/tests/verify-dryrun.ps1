#Requires -Version 7.0
<#
.SYNOPSIS
Memeriksa keluaran DryRun kedua skrip otomatis Empati.

.DESCRIPTION
Menjalankan setup-lemonade.ps1 dan bench-empati.ps1 dengan -DryRun, lalu
memastikan perintah yang dicetak memakai port 13305, keempat alias, dan tidak
pernah menyebut 11434. Dipakai sebagai pemeriksaan cepat sebelum demo.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

$setupOutput = & (Join-Path $scriptRoot 'setup-lemonade.ps1') -DryRun 6>&1 | Out-String
$benchOutput = & (Join-Path $scriptRoot 'bench-empati.ps1') -DryRun 6>&1 | Out-String
$combined = $setupOutput + "`n" + $benchOutput

$failures = @()

foreach ($alias in @('empati-llm', 'empati-embed', 'empati-stt', 'empati-tts')) {
    if ($combined -notmatch [regex]::Escape($alias)) {
        $failures += "alias $alias tidak muncul di keluaran DryRun"
    }
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

Write-Host 'Pemeriksaan DryRun lulus: alias lengkap, port 13305, tanpa jejak Ollama.' -ForegroundColor Green

#Requires -Version 7.0
<#
.SYNOPSIS
Validates DryRun output of Empati setup and benchmark scripts.

.DESCRIPTION
Runs setup-empati-lemonade.ps1 and bench-empati.ps1 with -DryRun and asserts
that host binding, all five contract aliases, models, benchmark calls, port 13305,
and zero legacy Ollama traces conform to specification.
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
        $failures += "missing expected alias '$alias' in DryRun output"
    }
}

if ($setupOutput -notmatch 'host=0\.0\.0\.0') {
    $failures += 'setup did not set host=0.0.0.0'
}

foreach ($model in @('Qwen3-4B-GGUF', 'Qwen3-1.7B-GGUF', 'nomic-embed-text-v1-GGUF', 'Whisper-Large-v3-Turbo', 'kokoro-v1')) {
    if ($setupOutput -notmatch [regex]::Escape($model)) {
        $failures += "missing expected model '$model' during setup"
    }
}

if ($setupOutput -notmatch 'lemonade bench') {
    $failures += 'setup did not invoke lemonade bench'
}

if ($combined -notmatch '13305') {
    $failures += 'port 13305 missing from DryRun output'
}

if ($combined -match '11434' -or $combined -match '/api/chat') {
    $failures += 'DryRun output still contains legacy Ollama traces (11434 or /api/chat)'
}

if ($setupOutput -notmatch 'lemonade alias add') {
    $failures += 'setup did not output lemonade alias add commands'
}

if ($benchOutput -notmatch 'chat' -or $benchOutput -notmatch 'embeddings') {
    $failures += 'benchmark dry-run missing chat or embeddings coverage'
}

if ($failures.Count -gt 0) {
    Write-Host 'DryRun verification failed:' -ForegroundColor Red
    $failures | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

Write-Host 'DryRun check passed: host binding, 5 aliases, models, bench, port 13305, zero Ollama traces.' -ForegroundColor Green

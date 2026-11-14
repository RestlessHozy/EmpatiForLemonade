#Requires -Version 7.0
<#
.SYNOPSIS
Provisions AMD Lemonade Server for Empati: host binding, models, contract aliases, and benchmarks.

.DESCRIPTION
Configures the server host binding, pulls the primary and backup chat LLMs,
embedding model, Whisper STT model, and Kokoro TTS model, registers all five
contract aliases, verifies /v1/models, and runs latency benchmarks.

Pass -DryRun to preview every command without executing anything.

.PARAMETER DryRun
Print planned commands only without invoking lemonade.

.PARAMETER LocalOnly
Keep host=127.0.0.1 instead of exposing host=0.0.0.0 on the LAN for physical phones.

.PARAMETER SkipPull
Skip pulling model weights (useful when models are already cached locally).

.PARAMETER SkipBench
Skip running benchmarks at the end of setup.

.PARAMETER BaseUrl
Target Lemonade Server URL for verification and benchmarks. Defaults to http://localhost:13305.
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

# Add default user-local Lemonade installation bin directory to PATH when missing
$lemonadeBin = "$env:LOCALAPPDATA\lemonade_server\bin"
if ((Test-Path (Join-Path $lemonadeBin 'lemonade.exe')) -and ($env:PATH -notlike "*$lemonadeBin*")) {
    $env:PATH = "$lemonadeBin;$env:PATH"
}

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
        throw "'lemonade' was not found on PATH. Install AMD Lemonade Server first, then re-run this script."
    }
}

Invoke-Step -Description "Setting Lemonade host binding to $bindHost" `
    -Command @('lemonade', 'config', 'set', "host=$bindHost") `
    -Action { lemonade config set "host=$bindHost" }

if (-not $LocalOnly) {
    Write-Host "   Server bound to LAN (0.0.0.0). Restrict firewall access and set LEMONADE_API_KEY in untrusted networks." -ForegroundColor Yellow
}

if (-not $SkipPull) {
    foreach ($model in @($llm, $llmBackup, $embed, $stt, $tts)) {
        Invoke-Step -Description "Pulling model $model" `
            -Command @('lemonade', 'pull', $model) `
            -Action { lemonade pull $model }
    }
}

foreach ($alias in $aliases.Keys) {
    $target = $aliases[$alias]
    Invoke-Step -Description "Registering alias $alias -> $target" `
        -Command @('lemonade', 'alias', 'add', $alias, $target) `
        -Action { lemonade alias add $alias $target }
}

Invoke-Step -Description "Verifying aliases at $BaseUrl/v1/models" `
    -Command @('curl', "$BaseUrl/v1/models") `
    -Action {
        $response = Invoke-RestMethod -Uri "$BaseUrl/v1/models" -TimeoutSec 15
        $ids = $response.data | ForEach-Object { $_.id }
        $missing = @($aliases.Keys | Where-Object { $_ -notin $ids })
        if ($missing.Count -gt 0) {
            throw "Missing expected aliases in /v1/models: $($missing -join ', ')"
        }
        Write-Host "   All aliases ready: $($aliases.Keys -join ', ')" -ForegroundColor Green
    }

if (-not $SkipBench) {
    Invoke-Step -Description 'Running built-in Lemonade benchmark' `
        -Command @('lemonade', 'bench') `
        -Action {
            try {
                lemonade bench
            } catch {
                Write-Warning "lemonade bench is unavailable on this host. Proceeding with scripts/bench-empati.ps1."
            }
        }

    Invoke-Step -Description 'Benchmarking Empati contract endpoints' `
        -Command @('pwsh', '-File', (Join-Path $scriptRoot 'bench-empati.ps1'), '-BaseUrl', $BaseUrl) `
        -Action {
            & (Join-Path $scriptRoot 'bench-empati.ps1') -BaseUrl $BaseUrl
        }
}

Write-Host ""
if ($DryRun) {
    Write-Host 'DryRun complete. No commands were executed.' -ForegroundColor Yellow
} else {
    Write-Host "Lemonade Server is ready for Empati at $BaseUrl." -ForegroundColor Green
}

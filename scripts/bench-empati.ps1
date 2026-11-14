#Requires -Version 7.0
<#
.SYNOPSIS
Measures chat, embedding, transcription, and TTS latency against AMD Lemonade Server.

.DESCRIPTION
Sends repeated benchmark requests to port 13305 using Empati's contract aliases
and reports median response times in milliseconds. Generates a 1-second 16 kHz
mono silent WAV automatically if -WavPath is omitted.
#>
[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:13305',
    [int]$Repeats = 3,
    [string]$WavPath,
    [string]$ReportPath = 'docs/BENCHMARK_RESULTS.md',
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')

if (-not $DryRun -and -not (Get-Command lemonade -ErrorAction SilentlyContinue)) {
    throw "'lemonade' was not found on PATH. Install AMD Lemonade Server first, then re-run this script."
}

function Measure-Endpoint {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][scriptblock]$Call
    )

    if ($DryRun) {
        Write-Host "[dry-run] $Name"
        return [pscustomobject]@{ Name = $Name; MedianMs = 0; Ok = 0; Total = 0 }
    }

    $samples = @()
    $ok = 0
    for ($i = 0; $i -lt $Repeats; $i++) {
        $watch = [System.Diagnostics.Stopwatch]::StartNew()
        try {
            & $Call | Out-Null
            $ok++
        } catch {
            Write-Warning "$Name failed on attempt $($i + 1): $($_.Exception.Message)"
        } finally {
            $watch.Stop()
        }
        $samples += $watch.Elapsed.TotalMilliseconds
    }

    $sorted = $samples | Sort-Object
    $median = if ($sorted.Count -gt 0) {
        if ($sorted.Count % 2 -eq 1) { $sorted[[int][math]::Floor($sorted.Count / 2)] }
        else { ($sorted[$sorted.Count / 2 - 1] + $sorted[$sorted.Count / 2]) / 2 }
    } else { 0 }

    [pscustomobject]@{
        Name     = $Name
        MedianMs = [math]::Round($median, 1)
        Ok       = $ok
        Total    = $Repeats
    }
}

function New-SilentWav {
    param([int]$Seconds = 1, [int]$SampleRate = 16000)

    $dataSize = $Seconds * $SampleRate * 2
    $buffer = New-Object byte[] (44 + $dataSize)

    function Write-Ascii([byte[]]$target, [int]$offset, [string]$text) {
        for ($i = 0; $i -lt $text.Length; $i++) { $target[$offset + $i] = [byte][char]$text[$i] }
    }
    function Write-Int([byte[]]$target, [int]$offset, [int]$value) {
        [BitConverter]::GetBytes([int]$value).CopyTo($target, $offset)
    }
    function Write-Short([byte[]]$target, [int]$offset, [int]$value) {
        [BitConverter]::GetBytes([int16]$value).CopyTo($target, $offset)
    }

    Write-Ascii $buffer 0 'RIFF'
    Write-Int $buffer 4 (36 + $dataSize)
    Write-Ascii $buffer 8 'WAVE'
    Write-Ascii $buffer 12 'fmt '
    Write-Int $buffer 16 16
    Write-Short $buffer 20 1
    Write-Short $buffer 22 1
    Write-Int $buffer 24 $SampleRate
    Write-Int $buffer 28 ($SampleRate * 2)
    Write-Short $buffer 32 2
    Write-Short $buffer 34 16
    Write-Ascii $buffer 36 'data'
    Write-Int $buffer 40 $dataSize

    $path = Join-Path ([System.IO.Path]::GetTempPath()) 'empati-silence.wav'
    [System.IO.File]::WriteAllBytes($path, $buffer)
    return $path
}

if ($DryRun) {
    Write-Host "[dry-run] POST $BaseUrl/v1/chat/completions (alias empati-llm)"
    Write-Host "[dry-run] POST $BaseUrl/v1/embeddings (alias empati-embed)"
    Write-Host "[dry-run] POST $BaseUrl/v1/audio/transcriptions (alias empati-stt)"
    Write-Host "[dry-run] POST $BaseUrl/v1/audio/speech (alias empati-tts)"
    Write-Host "[dry-run] repeats=$Repeats"
    return
}

$chatBody = @{
    model           = 'empati-llm'
    messages        = @(@{ role = 'user'; content = 'Sebutkan satu kebiasaan kecil untuk menjaga tekanan darah.' })
    max_tokens      = 64
    enable_thinking = $false
    stream          = $false
} | ConvertTo-Json -Depth 5

$embedBody = @{
    model = 'empati-embed'
    input = @('tekanan darah tinggi dan garam')
} | ConvertTo-Json -Depth 5

$ttsBody = @{
    model           = 'empati-tts'
    input           = 'Hasil skrining Anda menunjukkan indikator sedang.'
    response_format = 'mp3'
} | ConvertTo-Json -Depth 5

if (-not $WavPath) {
    $WavPath = New-SilentWav
    Write-Host "Generated silent test WAV: $WavPath" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "Warming up chat model before measurement..." -ForegroundColor DarkGray
try {
    Invoke-RestMethod -Uri "$BaseUrl/v1/chat/completions" -Method Post -ContentType 'application/json' -Body $chatBody -TimeoutSec 180 | Out-Null
} catch {
    Write-Warning "Warmup request failed: $($_.Exception.Message)"
}

$results = @()
$results += Measure-Endpoint -Name 'chat' -Call {
    Invoke-RestMethod -Uri "$BaseUrl/v1/chat/completions" -Method Post -ContentType 'application/json' -Body $chatBody -TimeoutSec 120
}
$results += Measure-Endpoint -Name 'embeddings' -Call {
    Invoke-RestMethod -Uri "$BaseUrl/v1/embeddings" -Method Post -ContentType 'application/json' -Body $embedBody -TimeoutSec 120
}
$results += Measure-Endpoint -Name 'transcription' -Call {
    $form = @{
        model    = 'empati-stt'
        language = 'id'
        file     = Get-Item $WavPath
    }
    Invoke-RestMethod -Uri "$BaseUrl/v1/audio/transcriptions" -Method Post -Form $form -TimeoutSec 120
}
$results += Measure-Endpoint -Name 'speech' -Call {
    Invoke-WebRequest -Uri "$BaseUrl/v1/audio/speech" -Method Post -ContentType 'application/json' -Body $ttsBody -OutFile (Join-Path ([System.IO.Path]::GetTempPath()) 'empati-bench.mp3') -TimeoutSec 120
}

Write-Host ""
Write-Host "Empati benchmark — Lemonade $BaseUrl — $Repeats repeats" -ForegroundColor Cyan
$results | Format-Table Name, MedianMs, Ok, Total -AutoSize

$failed = @($results | Where-Object { $_.Ok -lt $_.Total })

$lines = @(
    '# Empati Benchmark Results',
    '',
    "- Date: $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss zzz'))",
    "- Base URL: $BaseUrl",
    "- Repeats per endpoint: $Repeats",
    '- Hardware: fill manually (CPU, GPU/NPU, unified RAM, GPU driver, Lemonade version)',
    '',
    '| Endpoint | Median (ms) | Succeeded |',
    '|---|---|---|'
)
foreach ($row in $results) {
    $lines += "| $($row.Name) | $($row.MedianMs) | $($row.Ok)/$($row.Total) |"
}
$lines += ''
$lines += 'Figures represent median latencies measured after a single chat warmup call. Populate hardware details above for reproducibility.'

$reportDir = Split-Path -Parent $ReportPath
if ($reportDir -and -not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
}
[System.IO.File]::WriteAllText((Join-Path (Get-Location) $ReportPath), ($lines -join [Environment]::NewLine))
Write-Host "Summary written to $ReportPath" -ForegroundColor DarkGray

if ($failed.Count -gt 0) {
    Write-Warning "One or more endpoints failed: $($failed.Name -join ', ')"
    exit 1
}

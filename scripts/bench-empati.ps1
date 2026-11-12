#Requires -Version 7.0
<#
.SYNOPSIS
Mengukur latensi chat, embedding, transkripsi, dan TTS pada Lemonade Server.

.DESCRIPTION
Menjalankan beberapa permintaan ke port 13305 memakai alias Empati, lalu
menampilkan ringkasan dalam milidetik. Untuk transkripsi, skrip membuat WAV
hening 1 detik bila tidak ada -WavPath, jadi pengukuran tetap bisa dijalankan
tanpa rekaman asli.
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
    throw "Perintah 'lemonade' tidak ada di PATH. Pasang Lemonade Server dulu, lalu jalankan skrip ini lagi."
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
            Write-Warning "$Name gagal pada percobaan $($i + 1): $($_.Exception.Message)"
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

    Write-Ascii $buffer 0 'RIFF'
    Write-Int $buffer 4 (36 + $dataSize)
    Write-Ascii $buffer 8 'WAVE'
    Write-Ascii $buffer 12 'fmt '
    Write-Int $buffer 16 16
    Write-Int $buffer 20 1
    Write-Int $buffer 22 1
    Write-Int $buffer 24 $SampleRate
    Write-Int $buffer 28 ($SampleRate * 2)
    Write-Int $buffer 32 2
    Write-Int $buffer 34 16
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
    model      = 'empati-llm'
    messages   = @(@{ role = 'user'; content = 'Sebutkan satu kebiasaan kecil untuk menjaga tekanan darah.' })
    max_tokens = 64
    stream     = $false
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
    Write-Host "Membuat WAV hening: $WavPath" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "Memanaskan model sebelum pengukuran..." -ForegroundColor DarkGray
try {
    Invoke-RestMethod -Uri "$BaseUrl/v1/chat/completions" -Method Post -ContentType 'application/json' -Body $chatBody -TimeoutSec 180 | Out-Null
} catch {
    Write-Warning "Pemanasan gagal: $($_.Exception.Message)"
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
Write-Host "Empati benchmark, Lemonade $BaseUrl, $Repeats percobaan" -ForegroundColor Cyan
$results | Format-Table Name, MedianMs, Ok, Total -AutoSize

$failed = @($results | Where-Object { $_.Ok -lt $_.Total })

$lines = @(
    '# Hasil benchmark Empati',
    '',
    "- Tanggal: $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss zzz'))",
    "- Base URL: $BaseUrl",
    "- Percobaan per endpoint: $Repeats",
    '- Perangkat: isi manual (CPU, GPU/NPU, RAM, driver)',
    '',
    '| Endpoint | Median (ms) | Berhasil |',
    '|---|---|---|'
)
foreach ($row in $results) {
    $lines += "| $($row.Name) | $($row.MedianMs) | $($row.Ok)/$($row.Total) |"
}
$lines += ''
$lines += 'Angka adalah median setelah satu pemanasan. Isi kolom perangkat dari mesin yang dipakai.'

$reportDir = Split-Path -Parent $ReportPath
if ($reportDir -and -not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
}
[System.IO.File]::WriteAllText((Join-Path (Get-Location) $ReportPath), ($lines -join [Environment]::NewLine))
Write-Host "Ringkasan ditulis ke $ReportPath" -ForegroundColor DarkGray

if ($failed.Count -gt 0) {
    Write-Warning "Sebagian endpoint gagal: $($failed.Name -join ', ')"
    exit 1
}

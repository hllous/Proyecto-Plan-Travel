param(
    [string]$LocalPropertiesPath = "local.properties",
    [string]$OutputSqlPath = "",
    [int]$DelayMs = 150,
    [switch]$OnlyMissing = $true,
    [switch]$EmitRegionFallbacks = $true,
    [string]$SupabaseCliPath = "supabase",
    [string]$GooglePlacesApiKey
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ── Read local config if available ────────────────────────────────────────────
$propsByKey = @{}
if (Test-Path $LocalPropertiesPath) {
    Get-Content $LocalPropertiesPath | Where-Object { $_ -match "=" } |
        ForEach-Object {
            $k, $v = $_ -split "=", 2
            $propsByKey[$k.Trim()] = $v.Trim()
        }
}

if (-not $GooglePlacesApiKey) {
    $GooglePlacesApiKey = $propsByKey["GOOGLE_PLACES_API_KEY"]
}

# ── SQL output path ───────────────────────────────────────────────────────────
if (-not $OutputSqlPath) {
    $ts = Get-Date -Format "yyyyMMddHHmmss"
    $OutputSqlPath = "supabase/migrations/${ts}_destination_photos.sql"
}

# ── Helpers ───────────────────────────────────────────────────────────────────

function Normalize-Text([string]$s) {
    $nfd = $s.Normalize([Text.NormalizationForm]::FormD)
    $sb  = [Text.StringBuilder]::new()
    foreach ($c in $nfd.ToCharArray()) {
        if ([Globalization.CharUnicodeInfo]::GetUnicodeCategory($c) -ne [Globalization.UnicodeCategory]::NonSpacingMark) {
            [void]$sb.Append($c)
        }
    }
    return $sb.ToString().ToLowerInvariant().Trim()
}

function Get-WikiThumbnail([string]$Title) {
    $encoded = [Uri]::EscapeDataString($Title)
    try {
        $r = Invoke-RestMethod `
            -Uri "https://es.wikipedia.org/api/rest_v1/page/summary/$encoded" `
            -Headers @{ "User-Agent" = "PlanTravelApp/1.0 (batch-enrichment)" } `
            -TimeoutSec 8 `
            -ErrorAction Stop
        if ($r.type -eq "disambiguation") { return $null }
        return $r.thumbnail.source
    } catch { return $null }
}

function Get-WikiThumbnailByGeosearch([double]$Lat, [double]$Lng, [string]$DestName) {
    $coordStr = "$Lat|$Lng"
    try {
        $r = Invoke-RestMethod `
            -Uri "https://es.wikipedia.org/w/api.php?action=query&list=geosearch&gscoord=$coordStr&gsradius=10000&gslimit=5&format=json" `
            -Headers @{ "User-Agent" = "PlanTravelApp/1.0 (batch-enrichment)" } `
            -TimeoutSec 8 `
            -ErrorAction Stop
    } catch { return $null }

    $results = $r.query.geosearch
    if (-not $results -or $results.Count -eq 0) { return $null }

    $destNorm = Normalize-Text $DestName
    $startsWithMatch = $null
    $containsMatch   = $null
    $firstTitle      = $results[0].title

    foreach ($item in $results) {
        $norm = Normalize-Text $item.title
        if (-not $startsWithMatch -and $norm.StartsWith($destNorm)) { $startsWithMatch = $item.title }
        if (-not $containsMatch   -and $norm.Contains($destNorm))   { $containsMatch   = $item.title }
    }

    $best = if ($startsWithMatch) { $startsWithMatch } elseif ($containsMatch) { $containsMatch } else { $firstTitle }
    return Get-WikiThumbnail $best
}

function Invoke-SupabaseQueryRows([string]$Sql) {
    $raw = & $SupabaseCliPath --output json --log-level error db query --linked $Sql | Out-String
    if ($LASTEXITCODE -ne 0) {
        throw "supabase db query failed."
    }

    $jsonStart = $raw.IndexOf("{")
    if ($jsonStart -lt 0) {
        throw "supabase db query returned no JSON payload."
    }

    $depth = 0
    $jsonEnd = -1
    for ($i = $jsonStart; $i -lt $raw.Length; $i++) {
        if ($raw[$i] -eq "{") { $depth++ }
        elseif ($raw[$i] -eq "}") {
            $depth--
            if ($depth -eq 0) {
                $jsonEnd = $i
                break
            }
        }
    }
    if ($jsonEnd -lt 0) {
        throw "supabase db query JSON payload was incomplete."
    }

    $payload = $raw.Substring($jsonStart, ($jsonEnd - $jsonStart + 1)) | ConvertFrom-Json
    return @($payload.rows)
}

function Get-GoogleDestinationPhoto([string]$Name, [string]$Province) {
    if ([string]::IsNullOrWhiteSpace($GooglePlacesApiKey)) { return $null }

    $body = @{
        textQuery = "$Name, $Province, Argentina"
        languageCode = "es"
        regionCode = "AR"
        includedType = "locality"
    } | ConvertTo-Json -Depth 5

    try {
        $response = Invoke-RestMethod `
            -Uri "https://places.googleapis.com/v1/places:searchText" `
            -Method Post `
            -ContentType "application/json" `
            -Headers @{
                "X-Goog-Api-Key" = $GooglePlacesApiKey
                "X-Goog-FieldMask" = "places.id,places.displayName,places.photos,places.formattedAddress"
            } `
            -Body $body `
            -TimeoutSec 10 `
            -ErrorAction Stop
    } catch { return $null }

    $placesProp = $response.PSObject.Properties["places"]
    $places = if ($placesProp) { @($placesProp.Value) } else { @() }

    foreach ($place in $places) {
        $photosProp = $place.PSObject.Properties["photos"]
        $photoName = if ($photosProp) {
            @($photosProp.Value) | Select-Object -First 1 -ExpandProperty name
        } else {
            $null
        }
        if (-not [string]::IsNullOrWhiteSpace($photoName)) {
            return $photoName
        }
    }

    return $null
}

function Resolve-Photo([string]$Name, [string]$Province, [double]$Lat, [double]$Lng) {
    $googlePhotoName = Get-GoogleDestinationPhoto -Name $Name -Province $Province
    if ($googlePhotoName) {
        return @{
            source = "google"
            url = "https://places.googleapis.com/v1/$googlePhotoName/media?maxWidthPx=800&key=$GooglePlacesApiKey"
            title = $null
        }
    }

    $wikiUrl = Get-WikiThumbnail $Name
    if ($wikiUrl) {
        return @{ source = "wikipedia"; url = $wikiUrl; title = $Name }
    }

    $wikiUrl = Get-WikiThumbnail "$Name ($Province)"
    if ($wikiUrl) {
        return @{ source = "wikipedia"; url = $wikiUrl; title = $Name }
    }

    $wikiUrl = Get-WikiThumbnailByGeosearch -Lat $Lat -Lng $Lng -DestName $Name
    if ($wikiUrl) {
        return @{ source = "wikipedia"; url = $wikiUrl; title = $Name }
    }

    return $null
}

function Escape-Sql([string]$s) { return $s.Replace("'", "''") }

function Get-RegionFallbackToken([string]$Region) {
    switch ($Region) {
        "Patagonia" { return "fallback://region/patagonia" }
        "Cuyo" { return "fallback://region/cuyo" }
        "Noroeste" { return "fallback://region/noroeste" }
        "Litoral" { return "fallback://region/litoral" }
        "Buenos Aires" { return "fallback://region/buenos_aires" }
        "Córdoba" { return "fallback://region/cordoba" }
        default { return "fallback://region/argentina" }
    }
}

# ── Fetch destinations from linked Supabase DB ────────────────────────────────

$photoFilterSql = if ($OnlyMissing) { "and display_photo_url is null" } else { "" }
$sql = @"
select id, name, province, region, lat, lng
from public.destinations
where is_active = true
  $photoFilterSql
order by population desc
limit 1000
"@

Write-Host "Fetching destinations from linked Supabase DB..."
$destinations = Invoke-SupabaseQueryRows $sql

Write-Host "Found $($destinations.Count) destinations to enrich."

# ── Main enrichment loop ──────────────────────────────────────────────────────

$sqlLines = [Collections.Generic.List[string]]::new()
$sqlLines.Add("-- Generated by tools/destinations/fetch_wikipedia_photos.ps1 on $(Get-Date -Format 'yyyy-MM-dd HH:mm')")
$sqlLines.Add("-- Destination photo enrichment for $($destinations.Count) destinations")
$sqlLines.Add("")

$found   = 0
$fallback = 0
$missing = 0
$index   = 0

foreach ($dest in $destinations) {
    $index++
    Write-Host "[$index/$($destinations.Count)] $($dest.name), $($dest.province)..." -NoNewline

    $resolved = Resolve-Photo -Name $dest.name -Province $dest.province -Lat ([double]$dest.lat) -Lng ([double]$dest.lng)

    if ($resolved) {
        $found++
        $safeId = Escape-Sql $dest.id
        $safeUrl = Escape-Sql $resolved.url
        $safeName = Escape-Sql $dest.name
        if ($resolved.source -eq "google") {
            $sqlLines.Add("UPDATE public.destinations SET google_photo_url = '$safeUrl', display_photo_url = '$safeUrl', updated_at = now() WHERE id = '$safeId';")
        } else {
            $sqlLines.Add("UPDATE public.destinations SET wikipedia_photo_url = '$safeUrl', display_photo_url = '$safeUrl', wikipedia_title = '$safeName', updated_at = now() WHERE id = '$safeId';")
        }
        Write-Host " OK $($resolved.source): $($resolved.url.Substring(0, [Math]::Min(60, $resolved.url.Length)))"
    } elseif ($EmitRegionFallbacks) {
        $fallback++
        $safeId = Escape-Sql $dest.id
        $fallbackUrl = Escape-Sql (Get-RegionFallbackToken $dest.region)
        $sqlLines.Add("UPDATE public.destinations SET display_photo_url = '$fallbackUrl', updated_at = now() WHERE id = '$safeId';")
        Write-Host " OK fallback: $fallbackUrl"
    } else {
        $missing++
        Write-Host " -- no photo found"
    }

    Start-Sleep -Milliseconds $DelayMs
}

# ── Write SQL file ────────────────────────────────────────────────────────────

$sqlLines.Add("")
$sqlLines.Add("-- Results: $found remote photos found, $fallback region fallbacks assigned, $missing destinations still missing")

$dir = Split-Path -Parent $OutputSqlPath
if ($dir) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }

[IO.File]::WriteAllLines($OutputSqlPath, $sqlLines, [Text.UTF8Encoding]::new($false))

Write-Host ""
Write-Host "Done. $found / $($destinations.Count) destinations got photos."
Write-Host "SQL written to: $OutputSqlPath"
Write-Host "Apply with: supabase db push or via Supabase MCP apply_migration"

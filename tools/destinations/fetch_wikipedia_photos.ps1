param(
    [string]$LocalPropertiesPath = "local.properties",
    [string]$OutputSqlPath = "",
    [int]$DelayMs = 150,
    [switch]$OnlyMissing = $true,
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
    $GooglePlacesApiKey = $propsByKey["MAPS_PLATFORM_API_KEY"]
    if (-not $GooglePlacesApiKey) {
        $GooglePlacesApiKey = $propsByKey["GOOGLE_PLACES_API_KEY"]
    }
}

# ── SQL output path ───────────────────────────────────────────────────────────
if (-not $OutputSqlPath) {
    $ts = Get-Date -Format "yyyyMMddHHmmss"
    $OutputSqlPath = "supabase/migrations/${ts}_destination_photos_backfill.sql"
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

function Measure-Haversine([double]$Lat1, [double]$Lon1, [double]$Lat2, [double]$Lon2) {
    $r = 6371.0
    $dLat = [Math]::PI / 180 * ($Lat2 - $Lat1)
    $dLon = [Math]::PI / 180 * ($Lon2 - $Lon1)
    $a = [Math]::Sin($dLat / 2) * [Math]::Sin($dLat / 2) +
         [Math]::Cos([Math]::PI / 180 * $Lat1) * [Math]::Cos([Math]::PI / 180 * $Lat2) *
         [Math]::Sin($dLon / 2) * [Math]::Sin($dLon / 2)
    return $r * 2 * [Math]::Atan2([Math]::Sqrt($a), [Math]::Sqrt(1 - $a))
}

# Returns $true if the Wikipedia article JSON is geographically relevant to the destination.
# Accepts if: article coordinates are within 500 km, OR description/extract mentions Argentina
# or the destination's province (when no coordinates are present).
function Test-WikiArticleRelevant($ArticleJson, [double]$DestLat, [double]$DestLng, [string]$Province) {
    $coordsProp = $ArticleJson.PSObject.Properties["coordinates"]
    if ($coordsProp -and $coordsProp.Value) {
        $latProp = $coordsProp.Value.PSObject.Properties["lat"]
        $lonProp = $coordsProp.Value.PSObject.Properties["lon"]
        if ($latProp -and $lonProp) {
            $dist = Measure-Haversine $DestLat $DestLng ([double]$latProp.Value) ([double]$lonProp.Value)
            return $dist -le 500.0
        }
    }
    # No coordinates: require article text to mention Argentina or province.
    $descProp    = $ArticleJson.PSObject.Properties["description"]
    $extractProp = $ArticleJson.PSObject.Properties["extract"]
    $descVal    = if ($descProp)    { $descProp.Value }    else { "" }
    $extractVal = if ($extractProp) { $extractProp.Value } else { "" }
    $text = "$descVal $extractVal".ToLower()
    $provNorm = (Normalize-Text $Province).ToLower()
    return $text -like "*argentina*" -or $text -like "*$provNorm*"
}

# Fetches the Wikipedia summary for $Title, validates geographic relevance, and
# returns the thumbnail URL — or $null if invalid, SVG, or disambiguation.
function Get-WikiThumbnail([string]$Title, [double]$DestLat, [double]$DestLng, [string]$Province) {
    $encoded = [Uri]::EscapeDataString($Title)
    try {
        $r = Invoke-RestMethod `
            -Uri "https://es.wikipedia.org/api/rest_v1/page/summary/$encoded" `
            -Headers @{ "User-Agent" = "PlanTravelApp/1.0 (batch-enrichment)" } `
            -TimeoutSec 8 `
            -ErrorAction Stop
    } catch { return $null }

    if ($r.type -eq "disambiguation") { return $null }

    if (-not (Test-WikiArticleRelevant $r $DestLat $DestLng $Province)) { return $null }

    $thumbProp = $r.PSObject.Properties["thumbnail"]
    $url = if ($thumbProp -and $thumbProp.Value) { $thumbProp.Value.source } else { $null }
    if ([string]::IsNullOrWhiteSpace($url)) { return $null }

    # SVG thumbnails are location maps, flags, or diagrams — not destination photos.
    if ($url -like "*.svg*") { return $null }

    return $url
}

function Get-WikiThumbnailByGeosearch([double]$Lat, [double]$Lng, [string]$DestName, [string]$Province) {
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

    foreach ($item in $results) {
        $norm = Normalize-Text $item.title
        if (-not $startsWithMatch -and $norm.StartsWith($destNorm)) {
            $startsWithMatch = $item.title
        }
    }

    # Only use a geosearch hit if it starts with the destination name.
    # containsMatch / firstTitle may be unrelated POIs (stations, museums) near the town.
    if (-not $startsWithMatch) { return $null }
    return Get-WikiThumbnail $startsWithMatch $Lat $Lng $Province
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
        textQuery    = "$Name, $Province, Argentina"
        languageCode = "es"
        regionCode   = "AR"
    } | ConvertTo-Json -Depth 5

    try {
        $response = Invoke-RestMethod `
            -Uri "https://places.googleapis.com/v1/places:searchText" `
            -Method Post `
            -ContentType "application/json" `
            -Headers @{
                "X-Goog-Api-Key"   = $GooglePlacesApiKey
                "X-Goog-FieldMask" = "places.id,places.displayName,places.photos,places.formattedAddress,places.primaryType,places.types"
            } `
            -Body $body `
            -TimeoutSec 10 `
            -ErrorAction Stop
    } catch { return $null }

    $localityTypes = @("locality", "postal_town", "administrative_area_level_3", "sublocality")
    $placesProp = $response.PSObject.Properties["places"]
    $places = if ($placesProp) { @($placesProp.Value) } else { @() }

    foreach ($place in $places) {
        # Filter to locality-like types client-side (no server-side includedType restriction).
        $allTypes = @()
        if ($place.PSObject.Properties["primaryType"]) { $allTypes += $place.primaryType }
        if ($place.PSObject.Properties["types"]) { $allTypes += @($place.types) }
        $isLocality = @($allTypes | Where-Object { $localityTypes -contains $_ }).Count -gt 0
        if (-not $isLocality) { continue }

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
            url    = $googlePhotoName
            title  = $null
        }
    }

    $wikiUrl = Get-WikiThumbnail $Name $Lat $Lng $Province
    if ($wikiUrl) {
        return @{ source = "wikipedia"; url = $wikiUrl; title = $Name }
    }

    $wikiUrl = Get-WikiThumbnail "$Name ($Province)" $Lat $Lng $Province
    if ($wikiUrl) {
        return @{ source = "wikipedia"; url = $wikiUrl; title = $Name }
    }

    $wikiUrl = Get-WikiThumbnailByGeosearch -Lat $Lat -Lng $Lng -DestName $Name -Province $Province
    if ($wikiUrl) {
        return @{ source = "wikipedia"; url = $wikiUrl; title = $Name }
    }

    return $null
}

function Escape-Sql([string]$s) { return $s.Replace("'", "''") }

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
$missing = 0
$index   = 0

foreach ($dest in $destinations) {
    $index++
    Write-Host "[$index/$($destinations.Count)] $($dest.name), $($dest.province)..." -NoNewline

    $resolved = Resolve-Photo -Name $dest.name -Province $dest.province -Lat ([double]$dest.lat) -Lng ([double]$dest.lng)

    if ($resolved) {
        $found++
        $safeId   = Escape-Sql $dest.id
        $safeUrl  = Escape-Sql $resolved.url
        $safeName = Escape-Sql $dest.name
        if ($resolved.source -eq "google") {
            $sqlLines.Add("UPDATE public.destinations SET google_photo_url = '$safeUrl', display_photo_url = '$safeUrl', updated_at = now() WHERE id = '$safeId';")
        } else {
            $sqlLines.Add("UPDATE public.destinations SET wikipedia_photo_url = '$safeUrl', display_photo_url = '$safeUrl', wikipedia_title = '$safeName', updated_at = now() WHERE id = '$safeId';")
        }
        Write-Host " OK $($resolved.source): $($resolved.url.Substring(0, [Math]::Min(60, $resolved.url.Length)))"
    } else {
        $missing++
        Write-Host " -- no photo found"
    }

    Start-Sleep -Milliseconds $DelayMs
}

# ── Write SQL file ────────────────────────────────────────────────────────────

$sqlLines.Add("")
$sqlLines.Add("-- Results: $found photos found, $missing destinations with no photo found")

$dir = Split-Path -Parent $OutputSqlPath
if ($dir) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }

[IO.File]::WriteAllLines($OutputSqlPath, $sqlLines, [Text.UTF8Encoding]::new($false))

Write-Host ""
Write-Host "Done. $found / $($destinations.Count) destinations got photos."
Write-Host "SQL written to: $OutputSqlPath"
Write-Host "Apply with: supabase db push or apply via Supabase MCP apply_migration"

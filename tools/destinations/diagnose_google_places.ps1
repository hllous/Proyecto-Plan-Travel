param(
    [string]$LocalPropertiesPath = "local.properties",
    [string]$GooglePlacesApiKey
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ── Load API key ──────────────────────────────────────────────────────────────
if (-not $GooglePlacesApiKey) {
    if (Test-Path $LocalPropertiesPath) {
        Get-Content $LocalPropertiesPath | Where-Object { $_ -match "=" } | ForEach-Object {
            $k, $v = $_ -split "=", 2
            if ($k.Trim() -eq "GOOGLE_PLACES_API_KEY") { $GooglePlacesApiKey = $v.Trim() }
        }
    }
}
if ([string]::IsNullOrWhiteSpace($GooglePlacesApiKey)) {
    Write-Error "No API key found. Pass -GooglePlacesApiKey or set GOOGLE_PLACES_API_KEY in local.properties."
    exit 1
}
Write-Host "API key loaded (length=$($GooglePlacesApiKey.Length), prefix=$($GooglePlacesApiKey.Substring(0,6))...)" -ForegroundColor Cyan

# ── Test destinations: large city, medium city, edge case name, small town ────
$testDestinations = @(
    @{ Name = "Bariloche";     Province = "Río Negro";          Lat = -41.1335; Lng = -71.3103 }
    @{ Name = "Córdoba";       Province = "Córdoba";            Lat = -31.4201; Lng = -64.1888 }
    @{ Name = "Cervantes";     Province = "Río Negro";          Lat = -39.283;  Lng = -66.988  }
    @{ Name = "Goya";          Province = "Corrientes";         Lat = -29.1414; Lng = -59.2631 }
    @{ Name = "Montecarlo";    Province = "Misiones";           Lat = -26.571;  Lng = -54.762  }
    @{ Name = "San Clemente del Tuyú"; Province = "Buenos Aires"; Lat = -36.362; Lng = -56.726 }
    @{ Name = "Villa Pehuenia"; Province = "Neuquén";           Lat = -38.875;  Lng = -71.222  }
    @{ Name = "Corralito";     Province = "Córdoba";            Lat = -31.997;  Lng = -63.867  }
)

$localityTypes = @("locality", "postal_town", "administrative_area_level_3", "sublocality",
                   "administrative_area_level_2", "town", "village")

function Invoke-PlacesSearch([string]$Query, [bool]$WithIncludedType) {
    $bodyObj = @{
        textQuery    = $Query
        languageCode = "es"
        regionCode   = "AR"
    }
    if ($WithIncludedType) { $bodyObj["includedType"] = "locality" }
    $body = $bodyObj | ConvertTo-Json -Depth 5

    try {
        $r = Invoke-RestMethod `
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
        return $r
    } catch {
        return @{ error = $_.Exception.Message }
    }
}

function Summarize-Response($resp, [string]$label) {
    if ($resp -is [hashtable] -and $resp.ContainsKey("error")) {
        Write-Host "    [$label] ERROR: $($resp.error)" -ForegroundColor Red
        return
    }
    $placesProp = $resp.PSObject.Properties["places"]
    $places = if ($placesProp) { @($placesProp.Value) } else { @() }
    if ($places.Count -eq 0) {
        Write-Host "    [$label] 0 results" -ForegroundColor Yellow
        return
    }
    Write-Host "    [$label] $($places.Count) result(s):" -ForegroundColor Green
    foreach ($p in $places) {
        $name    = if ($p.PSObject.Properties["displayName"]) { $p.displayName.text } else { "?" }
        $addr    = if ($p.PSObject.Properties["formattedAddress"]) { $p.formattedAddress } else { "?" }
        $ptype   = if ($p.PSObject.Properties["primaryType"]) { $p.primaryType } else { "(none)" }
        $types   = if ($p.PSObject.Properties["types"]) { ($p.types -join ", ") } else { "(none)" }
        $photos  = if ($p.PSObject.Properties["photos"]) { @($p.photos).Count } else { 0 }
        $isLoc   = ($localityTypes | Where-Object { $ptype -eq $_ -or $types -like "*$_*" }).Count -gt 0
        $locMark = if ($isLoc) { "[LOCALITY ✓]" } else { "[NON-LOCALITY ✗]" }
        $photoMark = if ($photos -gt 0) { "[PHOTO ✓ x$photos]" } else { "[NO PHOTO ✗]" }
        Write-Host "      • $name | $addr" -ForegroundColor White
        Write-Host "        primaryType=$ptype | types=$types" -ForegroundColor Gray
        Write-Host "        $locMark $photoMark" -ForegroundColor $(if ($isLoc -and $photos -gt 0) { "Green" } else { "Yellow" })
    }
}

# ── Run tests ─────────────────────────────────────────────────────────────────

$summary = [Collections.Generic.List[PSCustomObject]]::new()

foreach ($dest in $testDestinations) {
    $query = "$($dest.Name), $($dest.Province), Argentina"
    Write-Host ""
    Write-Host "━━━ $query ━━━" -ForegroundColor Cyan

    $withFilter    = Invoke-PlacesSearch -Query $query -WithIncludedType $true
    $withoutFilter = Invoke-PlacesSearch -Query $query -WithIncludedType $false
    Start-Sleep -Milliseconds 200

    Summarize-Response $withFilter    "WITH    includedType=locality"
    Summarize-Response $withoutFilter "WITHOUT includedType=locality"

    # Determine if removing the filter would help
    $withPlaces    = if ($withFilter    -is [hashtable]) { 0 } else { $p = $withFilter.PSObject.Properties["places"]; if ($p) { @($p.Value).Count } else { 0 } }
    $withoutPlaces = if ($withoutFilter -is [hashtable]) { 0 } else { $p = $withoutFilter.PSObject.Properties["places"]; if ($p) { @($p.Value).Count } else { 0 } }

    $summary.Add([PSCustomObject]@{
        Destination    = $dest.Name
        Province       = $dest.Province
        WithFilter     = $withPlaces
        WithoutFilter  = $withoutPlaces
        FilterBlocking = ($withPlaces -eq 0 -and $withoutPlaces -gt 0)
    })
}

# ── Summary table ─────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "━━━ SUMMARY ━━━" -ForegroundColor Cyan
Write-Host ("{0,-30} {1,-18} {2,6} {3,8} {4,-15}" -f "Destination", "Province", "With", "Without", "Filter blocking?")
Write-Host ("{0,-30} {1,-18} {2,6} {3,8} {4,-15}" -f "---", "---", "---", "---", "---")
foreach ($row in $summary) {
    $color = if ($row.FilterBlocking) { "Yellow" } elseif ($row.WithFilter -eq 0 -and $row.WithoutFilter -eq 0) { "Red" } else { "White" }
    Write-Host ("{0,-30} {1,-18} {2,6} {3,8} {4,-15}" -f $row.Destination, $row.Province, $row.WithFilter, $row.WithoutFilter, $row.FilterBlocking) -ForegroundColor $color
}

$filterBlockingCount = ($summary | Where-Object { $_.FilterBlocking }).Count
$totalNoPhotos       = ($summary | Where-Object { $_.WithoutFilter -eq 0 }).Count
Write-Host ""
Write-Host "includedType=locality is BLOCKING results for $filterBlockingCount / $($summary.Count) destinations." -ForegroundColor $(if ($filterBlockingCount -gt 0) { "Yellow" } else { "Green" })
Write-Host "Destinations with 0 results even WITHOUT filter: $totalNoPhotos / $($summary.Count)" -ForegroundColor $(if ($totalNoPhotos -gt 0) { "Red" } else { "Green" })

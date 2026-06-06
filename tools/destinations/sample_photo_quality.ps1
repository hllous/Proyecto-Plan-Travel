param(
    [string]$LocalPropertiesPath = "local.properties"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Sample destinations: large cities, medium towns, edge-case names
$samples = @(
    @{ Name = "Bariloche";           Province = "Río Negro";    Lat = -41.1335; Lng = -71.3103; Notes = "large, famous" }
    @{ Name = "Ushuaia";             Province = "Tierra del Fuego"; Lat = -54.8019; Lng = -68.3030; Notes = "large, southernmost" }
    @{ Name = "El Chaltén";          Province = "Santa Cruz";   Lat = -49.33;   Lng = -72.89;   Notes = "medium, trekking hub" }
    @{ Name = "Cervantes";           Province = "Río Negro";    Lat = -39.283;  Lng = -66.988;  Notes = "edge case: same name as Spanish author" }
    @{ Name = "Goya";                Province = "Corrientes";   Lat = -29.1414; Lng = -59.2631; Notes = "edge case: same name as Spanish painter" }
    @{ Name = "Montecarlo";          Province = "Misiones";     Lat = -26.571;  Lng = -54.762;  Notes = "edge case: same name as Monaco" }
    @{ Name = "General Roca";        Province = "Río Negro";    Lat = -39.033;  Lng = -67.583;  Notes = "medium, named person" }
    @{ Name = "San Martín de los Andes"; Province = "Neuquén"; Lat = -40.157;  Lng = -71.353;  Notes = "medium, named person" }
    @{ Name = "Villa La Angostura";  Province = "Neuquén";      Lat = -40.762;  Lng = -71.649;  Notes = "small, scenic" }
    @{ Name = "Gaiman";              Province = "Chubut";       Lat = -43.285;  Lng = -65.493;  Notes = "small, Welsh colony" }
    @{ Name = "Corralito";           Province = "Córdoba";      Lat = -31.997;  Lng = -63.867;  Notes = "edge case: same name as 2001 crisis" }
    @{ Name = "Lima";                Province = "Buenos Aires"; Lat = -33.744;  Lng = -59.707;  Notes = "edge case: same name as Lima, Peru" }
    @{ Name = "Reconquista";         Province = "Santa Fe";     Lat = -29.145;  Lng = -59.643;  Notes = "edge case: same word as Spanish history" }
)

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
    $a = [Math]::Sin($dLat/2)*[Math]::Sin($dLat/2) +
         [Math]::Cos([Math]::PI/180*$Lat1)*[Math]::Cos([Math]::PI/180*$Lat2)*
         [Math]::Sin($dLon/2)*[Math]::Sin($dLon/2)
    return $r * 2 * [Math]::Atan2([Math]::Sqrt($a), [Math]::Sqrt(1-$a))
}

function Get-WikiSummary([string]$Title, [double]$DestLat, [double]$DestLng, [string]$Province) {
    $encoded = [Uri]::EscapeDataString($Title)
    try {
        $r = Invoke-RestMethod `
            -Uri "https://es.wikipedia.org/api/rest_v1/page/summary/$encoded" `
            -Headers @{ "User-Agent" = "PlanTravelApp/1.0 (sample-analysis)" } `
            -TimeoutSec 8 -ErrorAction Stop
    } catch { return $null }

    if ($r.type -eq "disambiguation") { return @{ status = "disambiguation"; title = $Title; description = "(disambiguation page)"; distKm = $null } }

    # Geo-validate
    $coordsProp = $r.PSObject.Properties["coordinates"]
    $withinRange = $false
    $distKm = $null
    if ($coordsProp -and $coordsProp.Value) {
        $latP = $coordsProp.Value.PSObject.Properties["lat"]
        $lonP = $coordsProp.Value.PSObject.Properties["lon"]
        if ($latP -and $lonP) {
            $distKm = [Math]::Round((Measure-Haversine $DestLat $DestLng ([double]$latP.Value) ([double]$lonP.Value)), 0)
            $withinRange = $distKm -le 500
        }
    } else {
        $descProp    = $r.PSObject.Properties["description"]
        $extractProp = $r.PSObject.Properties["extract"]
        $text = "$(if ($descProp) { $descProp.Value } else { '' }) $(if ($extractProp) { $extractProp.Value } else { '' })".ToLower()
        $provNorm = (Normalize-Text $Province).ToLower()
        $withinRange = ($text -like "*argentina*" -or $text -like "*$provNorm*")
    }

    $thumbProp = $r.PSObject.Properties["thumbnail"]
    $thumbUrl  = if ($thumbProp -and $thumbProp.Value) { $thumbProp.Value.source } else { $null }
    $isSvg     = $thumbUrl -and ($thumbUrl -like "*.svg*")

    $descProp2 = $r.PSObject.Properties["description"]
    $desc = if ($descProp2) { $descProp2.Value } else { "" }

    return @{
        status       = if (-not $withinRange) { "geo-rejected" } elseif ($isSvg) { "svg-rejected" } elseif (-not $thumbUrl) { "no-thumbnail" } else { "ok" }
        title        = $Title
        description  = $desc
        thumbnailUrl = $thumbUrl
        distKm       = $distKm
        withinRange  = $withinRange
    }
}

# ── Run analysis ──────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "━━━ SAMPLE PHOTO QUALITY ANALYSIS (Wikipedia) ━━━" -ForegroundColor Cyan
Write-Host "Checking Wikipedia sources for $($samples.Count) sample Argentine destinations"
Write-Host ""

foreach ($s in $samples) {
    Write-Host "┌─ $($s.Name), $($s.Province) [$($s.Notes)]" -ForegroundColor Cyan

    # Try 3 strategies: direct, parenthetical, (skip geosearch for brevity)
    $result = Get-WikiSummary $s.Name $s.Lat $s.Lng $s.Province
    Start-Sleep -Milliseconds 100

    if ($result -and $result.status -eq "ok") {
        $url = $result.thumbnailUrl
        $isCoa = $url -like "*coat*" -or $url -like "*escudo*" -or $url -like "*Escudo*" -or $url -like "*Shield*" -or $url -like "*bandera*" -or $url -like "*flag*" -or $url -like "*Flag*" -or $url -like "*Map*" -or $url -like "*map*" -or $url -like "*mapa*"
        $quality = if ($isCoa) { "⚠ likely coat-of-arms/map" } else { "✓ likely photo" }
        Write-Host "│  DIRECT      [$($result.status.ToUpper())] $quality" -ForegroundColor $(if ($isCoa) { "Yellow" } else { "Green" })
        Write-Host "│  URL: $url"
        Write-Host "│  Description: $($result.description)"
        if ($result.distKm) { Write-Host "│  Distance from dest: $($result.distKm) km" }
    } elseif ($result) {
        Write-Host "│  DIRECT      [$($result.status.ToUpper())] $($result.description) $(if ($result.distKm) { "($($result.distKm) km)" })" -ForegroundColor Yellow

        # Try parenthetical form
        $paren = Get-WikiSummary "$($s.Name) ($($s.Province))" $s.Lat $s.Lng $s.Province
        Start-Sleep -Milliseconds 100
        if ($paren -and $paren.status -eq "ok") {
            $url = $paren.thumbnailUrl
            $isCoa = $url -like "*coat*" -or $url -like "*escudo*" -or $url -like "*Escudo*" -or $url -like "*Shield*" -or $url -like "*bandera*" -or $url -like "*flag*" -or $url -like "*Flag*" -or $url -like "*Map*" -or $url -like "*map*" -or $url -like "*mapa*"
            $quality = if ($isCoa) { "⚠ likely coat-of-arms/map" } else { "✓ likely photo" }
            Write-Host "│  PARENTHETICAL [$($paren.status.ToUpper())] $quality" -ForegroundColor $(if ($isCoa) { "Yellow" } else { "Green" })
            Write-Host "│  URL: $url"
            Write-Host "│  Description: $($paren.description)"
        } else {
            $parenStatus = if ($paren) { $paren.status } else { "not-found" }
            Write-Host "│  PARENTHETICAL [$($parenStatus.ToUpper())] no usable result" -ForegroundColor Red
        }
    } else {
        Write-Host "│  DIRECT      [NOT FOUND]" -ForegroundColor Red
    }
    Write-Host "└─"
    Write-Host ""
}

Write-Host "Open URLs in a browser to evaluate actual image quality." -ForegroundColor Cyan
Write-Host "Google Places photos require billing to be enabled (quota: 100/day free tier)." -ForegroundColor Yellow

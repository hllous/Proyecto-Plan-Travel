# Tiered Places API Strategy for Destination Recommendations

Place Recommendations for a Trip Destination are sourced from a tiered set of external APIs rather than curated static data or AI generation.

**Primary: Google Places API.** Rich, worldwide POI data with categories, ratings, and photos. Covered by Google Cloud free tier credits for MVP usage volume.

**Fallback 1: OpenTripMap.** Free, no API key cost, open travel data. Used if Google Places credits are exhausted or if we choose to stop using them.

**Fallback 2: Foursquare Places API.** Free tier (1,000 calls/day). Used as a secondary fallback.

AI-generated recommendations (Gemini, OpenAI) were rejected because they require paid API access beyond the free tier, which is not acceptable for this project.

# MercadoPago deep link instead of OAuth/Checkout Pro

Payment Deep Links are constructed from each user's stored MP Alias using a `mercadopago://` URI scheme, opening the MercadoPago app pre-filled with recipient and amount. Payment confirmation remains a manual Payment Status acknowledgement.

Full MP OAuth (Checkout Pro with webhook confirmation) was considered and deferred to v3. Checkout Pro requires each creditor to connect their MP account via OAuth, an Edge Function to generate payment preferences server-side, and a public webhook endpoint to auto-confirm Payment Status. The deep-link approach delivers a genuinely useful "tap to pay" UX with zero OAuth infrastructure and no MP MCP setup required. The trade-off is that payment confirmation stays manual — acceptable for MVP given that the group already shares trust.

# Email Confirmation Disabled for MVP

Email confirmation is disabled in the Supabase Auth dashboard for the MVP. When a user registers, they receive a session immediately without needing to click a confirmation link.

## Context

The PRD (#9) explicitly scopes out email verification: "Email verification (can be disabled in Supabase dashboard for MVP)." With email confirmation enabled, `supabase.auth.signUpWith(Email)` returns a user object but no session — `sessionStatus` stays `NotAuthenticated`, `observeUserId()` emits `null`, and the app silently bounces back to the login screen with no user-visible feedback. Implementing the "check your inbox" interstitial screen is non-trivial and was not in scope.

## Decision

**Supabase Auth dashboard → Authentication → Settings → "Enable email confirmations" → OFF.**

This setting is per-project and lives outside version control. Any new Supabase project or environment reset must re-apply this setting before the app can be used.

## Consequences

- Users can register and immediately use the app.
- Invalid or mistyped email addresses are accepted at registration (no ownership verification).
- **Before production launch**, email confirmation must be re-enabled and the app must handle the `confirmation_sent_at` / pending-confirmation state with a dedicated screen and a resend-email action.

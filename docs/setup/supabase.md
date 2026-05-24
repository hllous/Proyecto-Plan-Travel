# Supabase Project Setup

## Project

| Key | Value |
|-----|-------|
| Project ref | `kkliecfohacopgtlfylv` |
| Region | (default) |
| MCP URL | `https://mcp.supabase.com/mcp?project_ref=kkliecfohacopgtlfylv` |

## local.properties

These keys are required to build the app. They are gitignored. See `local.properties.example` for the template.

```
SUPABASE_URL=https://kkliecfohacopgtlfylv.supabase.co
SUPABASE_PUBLISHABLE_KEY=<publishable key from Supabase dashboard>
```

The publishable key (`sb_publishable_...`) is found in the Supabase dashboard under **Project Settings → API → Project API keys → Publishable**.

## Schema

The full initial schema is at `supabase/migrations/20260524000000_initial_schema.sql`. It creates six tables with RLS:

- `profiles` — User display name, phone, avatar (linked to `auth.users`)
- `travel_groups` — Group name and admin
- `group_members` — User ↔ Group membership with role (ADMIN / USER)
- `invite_tokens` — Time-limited join codes
- `expense_items` — Shared costs per group
- `item_assignments` — Per-member quantity allocations

To apply the schema to a new project:
```
supabase link --project-ref <ref>
supabase db push
```

## Dashboard settings applied manually

These settings live outside version control and must be re-applied to any new project or environment:

| Setting | Location | Value | Reason |
|---------|----------|-------|--------|
| Email confirmations | Auth → Settings → Email | **OFF** | See ADR-0005 — app has no pending-confirmation screen for MVP |

## Supabase MCP (for AI agents)

The MCP server is configured in `.mcp.json` at the project root:

```json
{
  "mcpServers": {
    "supabase": {
      "type": "http",
      "url": "https://mcp.supabase.com/mcp?project_ref=kkliecfohacopgtlfylv"
    }
  }
}
```

To authenticate in a new session:
1. Run `claude /mcp` in a regular terminal (not the IDE extension)
2. Select `supabase · needs authentication`
3. Press Enter and complete the browser OAuth flow

Once authenticated, the agent can query tables, inspect RLS, create confirmed test users, and run SQL directly against the live project.

## Agent skills

Supabase agent skills are installed at `.agents/skills/supabase/`. They are loaded automatically when the `supabase` skill is invoked.

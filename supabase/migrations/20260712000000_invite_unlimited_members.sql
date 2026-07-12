-- Invite codes/QR must work for unlimited members, not just the first joiner.
-- consumeInvite() no longer deletes the invite token after a successful join,
-- so the "member delete" policy (added to support that single-use behavior)
-- is no longer needed and is overly permissive. Only group admins should be
-- able to delete/revoke an invite (via deleteInvite()).

drop policy "invite_tokens: member delete" on public.invite_tokens;

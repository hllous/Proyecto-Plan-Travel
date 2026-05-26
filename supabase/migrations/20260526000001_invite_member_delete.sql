-- Allow any group member to delete invite tokens for their group.
-- This is needed because consumeInvite() runs as the joining USER (not an admin),
-- so the existing "admin delete" policy blocks the post-join cleanup.
-- With this policy, the token is consumed (deleted) as soon as someone joins,
-- preventing repeated use of the same link within the 24-hour expiry window.

create policy "invite_tokens: member delete"
  on public.invite_tokens for delete
  to authenticated
  using (public.is_group_member(group_id));

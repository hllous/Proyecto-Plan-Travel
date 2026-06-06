ALTER TABLE expense_groups
    ADD COLUMN paid_by_member_id UUID REFERENCES group_members(id) ON DELETE SET NULL;

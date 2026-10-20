-- Replace BOTH the UUID and email with YOUR verified account before executing as project owner.
-- Find the UUID in Supabase Authentication > Users or the app's Hesap Kimliğini Kopyala button.
-- A placeholder/nonexistent UUID fails safely; no arbitrary first phone becomes an administrator.
begin;
insert into nobet_private.bootstrap_owner(singleton,user_id,email)
values(true,'00000000-0000-0000-0000-000000000000'::uuid,lower('SIZIN_EPOSTA_ADRESINIZ@example.com'))
on conflict(singleton) do update set user_id=excluded.user_id,email=excluded.email;
commit;

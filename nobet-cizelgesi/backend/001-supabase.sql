-- Supabase SQL Editor: run once in a new project. No API/server secret belongs in the APK.
begin;
create schema if not exists nobet_private;
revoke all on schema nobet_private from public, anon, authenticated;
create table if not exists nobet_private.bootstrap_owner (
  singleton boolean primary key default true check(singleton), user_id uuid not null references auth.users(id), email text not null
);
create table if not exists public.nobet_teams (
  id uuid primary key default gen_random_uuid(), name text not null check(length(name) between 1 and 100),
  owner_id uuid not null unique references auth.users(id),
  payload jsonb, revision bigint not null default 0,
  updated_at timestamptz not null default now(), updated_by uuid references auth.users(id)
);
create table if not exists public.nobet_members (
  team_id uuid not null references public.nobet_teams(id) on delete cascade,
  user_id uuid not null references auth.users(id), role text not null check(role in ('admin','viewer')),
  primary key(team_id,user_id)
);
create table if not exists public.nobet_invites (
  team_id uuid not null references public.nobet_teams(id) on delete cascade,
  email text not null, role text not null check(role in ('admin','viewer')),
  invited_by uuid not null references auth.users(id), primary key(team_id,email)
);
create table if not exists nobet_private.requests (
  team_id uuid not null references public.nobet_teams(id) on delete cascade,
  request_id uuid not null, user_id uuid not null references auth.users(id), payload_hash text not null,
  primary key(team_id,request_id)
);
-- No direct REST table writes. Even an authenticated administrator uses the guarded RPCs.
alter table public.nobet_teams enable row level security;
alter table public.nobet_members enable row level security;
alter table public.nobet_invites enable row level security;
revoke all on public.nobet_teams,public.nobet_members,public.nobet_invites from public,anon,authenticated;
revoke all on all tables in schema nobet_private from public,anon,authenticated;

create or replace function public.nobet_role(p_team uuid) returns text
language sql stable security definer set search_path=public,pg_temp as $$
 select m.role from public.nobet_members m where m.team_id=p_team and m.user_id=auth.uid()
$$;

create or replace function public.nobet_head(p_team uuid) returns jsonb
language plpgsql stable security definer set search_path=public,pg_temp as $$
declare r text; result jsonb;
begin
 r:=public.nobet_role(p_team);
 if r is null then raise exception 'NOBET_FORBIDDEN' using errcode='42501'; end if;
 select jsonb_build_object('id',id,'name',name,'role',r,'revision',revision,'updated_at',updated_at,'updated_by',updated_by)
 into result from public.nobet_teams where id=p_team;
 return result;
end $$;

create or replace function public.nobet_snapshot(p_team uuid) returns jsonb
language plpgsql security definer set search_path=public,pg_temp as $$
declare r text; result jsonb;
begin
 r:=public.nobet_role(p_team);
 if r is null then raise exception 'NOBET_FORBIDDEN' using errcode='42501'; end if;
 select jsonb_build_object('id',t.id,'name',t.name,'role',r,'revision',t.revision,
  'payload',t.payload,'updated_at',t.updated_at,'updated_by',t.updated_by) into result
 from public.nobet_teams t where t.id=p_team;
 return result;
end $$;

create or replace function public.nobet_my_teams() returns jsonb
language sql stable security definer set search_path=public,pg_temp as $$
 select coalesce(jsonb_agg(jsonb_build_object('id',t.id,'name',t.name,'role',m.role) order by t.name),'[]'::jsonb)
 from public.nobet_teams t join public.nobet_members m on m.team_id=t.id where m.user_id=auth.uid()
$$;

create or replace function public.nobet_create_team(p_name text) returns uuid
language plpgsql security definer set search_path=public,pg_temp as $$
declare email_value text; team uuid;
begin
 select lower(u.email) into email_value from auth.users u where u.id=auth.uid() and u.email_confirmed_at is not null;
 if email_value is null or not exists(select 1 from nobet_private.bootstrap_owner b where b.user_id=auth.uid() and lower(b.email)=email_value)
 then raise exception 'NOBET_OWNER_REQUIRED' using errcode='42501'; end if;
 if length(trim(p_name)) not between 1 and 100 then raise exception 'NOBET_INVALID'; end if;
 insert into public.nobet_teams(name,owner_id,updated_by) values(trim(p_name),auth.uid(),auth.uid()) returning id into team;
 insert into public.nobet_members values(team,auth.uid(),'admin');
 return team;
end $$;

create or replace function public.nobet_accept_invitations() returns void
language plpgsql security definer set search_path=public,pg_temp as $$
declare email_value text; invitation record;
begin
 select lower(u.email) into email_value from auth.users u where u.id=auth.uid() and u.email_confirmed_at is not null;
 if email_value is null then raise exception 'NOBET_EMAIL_UNVERIFIED' using errcode='42501'; end if;
 for invitation in select i.* from public.nobet_invites i where i.email=email_value order by i.team_id loop
   perform 1 from public.nobet_teams where id=invitation.team_id for update;
   select i.* into invitation from public.nobet_invites i where i.team_id=invitation.team_id and i.email=email_value for update;
   if not found then continue; end if;
   -- A revoked/non-admin inviter cannot grant authority later.
   if exists(select 1 from public.nobet_members m where m.team_id=invitation.team_id and m.user_id=invitation.invited_by and m.role='admin') then
    insert into public.nobet_members values(invitation.team_id,auth.uid(),invitation.role) on conflict do nothing;
   end if;
   delete from public.nobet_invites where team_id=invitation.team_id and email=invitation.email;
 end loop;
end $$;

create or replace function public.nobet_publish(p_team uuid,p_expected bigint,p_payload jsonb,p_request uuid) returns jsonb
language plpgsql security definer set search_path=public,pg_temp as $$
declare current_revision bigint; previous nobet_private.requests%rowtype;
begin
 select revision into current_revision from public.nobet_teams where id=p_team for update;
 if public.nobet_role(p_team) is distinct from 'admin' then raise exception 'NOBET_FORBIDDEN' using errcode='42501'; end if;
 if p_request is null or p_expected is null or not coalesce(jsonb_typeof(p_payload)='object'
  and p_payload->>'version'='2' and jsonb_typeof(p_payload->'people')='array'
  and jsonb_typeof(p_payload->'places')='array' and jsonb_typeof(p_payload->'leaves')='array'
  and jsonb_typeof(p_payload->'weeks')='array',false)
  or octet_length(p_payload::text)>2097152 then raise exception 'NOBET_INVALID'; end if;
 select * into previous from nobet_private.requests where team_id=p_team and request_id=p_request;
 if found then
   if previous.user_id<>auth.uid() or previous.payload_hash<>md5(p_payload::text) then raise exception 'NOBET_INVALID'; end if;
   return public.nobet_snapshot(p_team);
 end if;
 if current_revision<>p_expected then raise exception 'NOBET_CONFLICT' using errcode='40001'; end if;
 update public.nobet_teams set payload=p_payload,revision=revision+1,updated_at=now(),updated_by=auth.uid() where id=p_team;
 insert into nobet_private.requests values(p_team,p_request,auth.uid(),md5(p_payload::text));
 return public.nobet_snapshot(p_team);
end $$;

create or replace function public.nobet_members_list(p_team uuid) returns jsonb
language plpgsql security definer set search_path=public,pg_temp as $$
begin
 if public.nobet_role(p_team) is distinct from 'admin' then raise exception 'NOBET_FORBIDDEN' using errcode='42501'; end if;
 return jsonb_build_object('members',(select coalesce(jsonb_agg(jsonb_build_object('id',m.user_id,'email',u.email,
  'name',coalesce(u.raw_user_meta_data->>'full_name',''),'role',m.role) order by u.email),'[]'::jsonb)
  from public.nobet_members m join auth.users u on u.id=m.user_id where m.team_id=p_team),
  'invites',(select coalesce(jsonb_agg(jsonb_build_object('email',i.email,'role',i.role) order by i.email),'[]'::jsonb)
  from public.nobet_invites i where i.team_id=p_team));
end $$;

create or replace function public.nobet_invite(p_team uuid,p_email text,p_role text) returns void
language plpgsql security definer set search_path=public,pg_temp as $$
begin
 perform 1 from public.nobet_teams where id=p_team for update;
 if public.nobet_role(p_team) is distinct from 'admin' then raise exception 'NOBET_FORBIDDEN' using errcode='42501'; end if;
 if p_email is null or length(p_email)>254 or p_email !~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$' or p_role not in ('admin','viewer') or p_role is null
 then raise exception 'NOBET_INVALID'; end if;
 if exists(select 1 from public.nobet_members m join auth.users u on u.id=m.user_id where m.team_id=p_team and lower(u.email)=lower(trim(p_email)))
 then raise exception 'NOBET_ALREADY_MEMBER'; end if;
 insert into public.nobet_invites values(p_team,lower(trim(p_email)),p_role,auth.uid())
 on conflict(team_id,email) do update set role=excluded.role,invited_by=excluded.invited_by;
end $$;

create or replace function public.nobet_set_role(p_team uuid,p_user uuid,p_role text) returns void
language plpgsql security definer set search_path=public,pg_temp as $$
begin
 perform 1 from public.nobet_teams where id=p_team for update;
 if public.nobet_role(p_team) is distinct from 'admin' then raise exception 'NOBET_FORBIDDEN' using errcode='42501'; end if;
 if p_role is null or p_role not in ('admin','viewer') then raise exception 'NOBET_INVALID'; end if;
 if p_role<>'admin' and exists(select 1 from public.nobet_members where team_id=p_team and user_id=p_user and role='admin')
 and (select count(*) from public.nobet_members where team_id=p_team and role='admin')<=1 then raise exception 'NOBET_LAST_ADMIN'; end if;
 update public.nobet_members set role=p_role where team_id=p_team and user_id=p_user;
 if not found then raise exception 'NOBET_NOT_FOUND'; end if;
 -- Pending invitations made by a demoted administrator must be reviewed by a current admin.
 if p_role<>'admin' then delete from public.nobet_invites where team_id=p_team and invited_by=p_user; end if;
end $$;

create or replace function public.nobet_remove_member(p_team uuid,p_user uuid) returns void
language plpgsql security definer set search_path=public,pg_temp as $$
begin
 perform 1 from public.nobet_teams where id=p_team for update;
 if public.nobet_role(p_team) is distinct from 'admin' then raise exception 'NOBET_FORBIDDEN' using errcode='42501'; end if;
 if exists(select 1 from public.nobet_members where team_id=p_team and user_id=p_user and role='admin')
 and (select count(*) from public.nobet_members where team_id=p_team and role='admin')<=1 then raise exception 'NOBET_LAST_ADMIN'; end if;
 delete from public.nobet_invites where team_id=p_team and invited_by=p_user;
 delete from public.nobet_members where team_id=p_team and user_id=p_user;
end $$;

create or replace function public.nobet_cancel_invite(p_team uuid,p_email text) returns void
language plpgsql security definer set search_path=public,pg_temp as $$
begin
 perform 1 from public.nobet_teams where id=p_team for update;
 if public.nobet_role(p_team) is distinct from 'admin' then raise exception 'NOBET_FORBIDDEN' using errcode='42501'; end if;
 delete from public.nobet_invites where team_id=p_team and email=lower(trim(p_email));
end $$;

-- Explicitly scope permissions to this application's functions; leave other project functions alone.
do $$ declare f record; begin
 for f in select p.oid::regprocedure signature from pg_proc p join pg_namespace n on n.oid=p.pronamespace
 where n.nspname='public' and p.proname in ('nobet_role','nobet_head','nobet_snapshot','nobet_my_teams','nobet_create_team',
 'nobet_accept_invitations','nobet_publish','nobet_members_list','nobet_invite','nobet_set_role','nobet_remove_member','nobet_cancel_invite') loop
 execute format('revoke all on function %s from public,anon,authenticated',f.signature);
 execute format('grant execute on function %s to authenticated',f.signature);
 end loop;
end $$;
commit;

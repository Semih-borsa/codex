#!/usr/bin/env python3
"""Isolated PostgreSQL RPC/privilege tests, not a live Supabase/Auth integration test.
Run only in a disposable PostgreSQL container: python3 run.py CONTAINER_NAME.
The fixture REPLACES the disposable database's public/auth/nobet_private schemas.
"""
from pathlib import Path
import concurrent.futures
import json
import subprocess
import sys

container=sys.argv[1]
checks=0

def sql(statement, user=None, error=None):
    if user:
        statement="begin;set local role authenticated;select set_config('request.jwt.claim.sub','%s',true);%s;commit;"%(user,statement)
    result=subprocess.run(['docker','exec','-i','-u','postgres',container,'psql','-X','-qAt','-v','ON_ERROR_STOP=1'],input=statement,text=True,capture_output=True)
    if error:
        assert result.returncode!=0 and error in result.stderr,(error,result.stderr)
    else:
        assert result.returncode==0,result.stderr
    return result.stdout.strip().splitlines()[-1] if result.stdout.strip() else ''

def check(name, action):
    global checks
    action();checks+=1;print('PASS',name,flush=True)

def same(value,expected): assert value==expected,(value,expected)

owner='00000000-0000-0000-0000-000000000001'
viewer='00000000-0000-0000-0000-000000000002'
admin='00000000-0000-0000-0000-000000000003'
outsider='00000000-0000-0000-0000-000000000004'
unverified='00000000-0000-0000-0000-000000000005'
second='00000000-0000-0000-0000-000000000006'
sql("""drop schema if exists nobet_private cascade;drop schema if exists public cascade;drop schema if exists auth cascade;
create schema public;create schema auth;
do $$begin if not exists(select 1 from pg_roles where rolname='anon') then create role anon nologin;end if;
if not exists(select 1 from pg_roles where rolname='authenticated') then create role authenticated nologin;end if;end $$;
grant usage on schema public,auth to authenticated,anon;
create table auth.users(id uuid primary key,email text,email_confirmed_at timestamptz,raw_user_meta_data jsonb default '{}'::jsonb);
create function auth.uid() returns uuid language sql stable as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
""")
for user,email,confirmed in [(owner,'owner@example.test',True),(viewer,'viewer@example.test',True),(admin,'admin@example.test',True),(outsider,'outsider@example.test',True),(unverified,'unverified@example.test',False),(second,'second@example.test',True)]:
    sql("insert into auth.users values('%s','%s',%s,'{\"role\":\"admin\"}');"%(user,email,'now()' if confirmed else 'null'))
sql((Path(__file__).parents[1]/'001-supabase.sql').read_text())
sql("insert into nobet_private.bootstrap_owner values(true,'00000000-0000-0000-0000-000000000001','owner@example.test');")
check('Anonymous cannot call RPC',lambda:sql('set role anon;select public.nobet_my_teams();',error='permission denied'))
check('Verified outsider cannot claim first administrator',lambda:sql("select public.nobet_create_team('Hijack')",outsider,'NOBET_OWNER_REQUIRED'))
def check_unverified_owner():
    sql("update nobet_private.bootstrap_owner set user_id='%s',email='unverified@example.test'"%unverified)
    sql("select public.nobet_create_team('Hijack')",unverified,'NOBET_OWNER_REQUIRED')
    sql("update nobet_private.bootstrap_owner set user_id='%s',email='owner@example.test'"%owner)
check('Unverified pinned owner does not bootstrap',check_unverified_owner)
team=sql("select public.nobet_create_team('Test Ekibi')",owner)
check('Owner receives admin role on initial empty revision',lambda:same(json.loads(sql("select public.nobet_snapshot('%s')"%team,owner))['role'],'admin'))
check('Outsider cannot read another team',lambda:sql("select public.nobet_snapshot('%s')"%team,outsider,'NOBET_FORBIDDEN'))
check('Administrator cannot directly write tables',lambda:sql("update public.nobet_teams set revision=999",owner,'permission denied'))
check('Members cannot read bootstrap owner configuration',lambda:sql('select * from nobet_private.bootstrap_owner',owner,'permission denied'))
sql("select public.nobet_invite('%s','viewer@example.test','viewer')"%team,owner)
sql('select public.nobet_accept_invitations()',viewer)
check('Invited viewer reads team and cannot self-promote via auth metadata',lambda:same(json.loads(sql("select public.nobet_snapshot('%s')"%team,viewer))['role'],'viewer'))
payload={'version':2,'people':[],'places':[],'leaves':[],'weeks':[]}
body=json.dumps(payload)
request='10000000-0000-0000-0000-000000000001'
publish=lambda rev,req,data=body:"select public.nobet_publish('%s',%d,'%s'::jsonb,'%s')"%(team,rev,data,req)
check('Polling metadata has no roster payload',lambda:same('payload' in json.loads(sql("select public.nobet_head('%s')"%team,viewer)),False))
check('Outsider cannot read polling metadata',lambda:sql("select public.nobet_head('%s')"%team,outsider,'NOBET_FORBIDDEN'))
check('Viewer cannot publish',lambda:sql(publish(0,request),viewer,'NOBET_FORBIDDEN'))
check('Viewer cannot grant authority',lambda:sql("select public.nobet_set_role('%s','%s','admin')"%(team,viewer),viewer,'NOBET_FORBIDDEN'))
check('Viewer cannot list account emails',lambda:sql("select public.nobet_members_list('%s')"%team,viewer,'NOBET_FORBIDDEN'))
check('Viewer cannot invite',lambda:sql("select public.nobet_invite('%s','x@example.test','admin')"%team,viewer,'NOBET_FORBIDDEN'))
sql("select public.nobet_invite('%s','unverified@example.test','admin')"%team,owner)
check('Unverified invitation cannot join',lambda:sql('select public.nobet_accept_invitations()',unverified,'NOBET_EMAIL_UNVERIFIED'))
sql('select public.nobet_accept_invitations()',outsider)
check('Wrong account cannot accept another email invitation',lambda:same(json.loads(sql('select public.nobet_my_teams()',outsider)),[]))
sql("select public.nobet_invite('%s','admin@example.test','admin')"%team,owner)
sql('select public.nobet_accept_invitations()',admin)
check('Invited admin can invite another administrator',lambda:sql("select public.nobet_invite('%s','future@example.test','admin')"%team,admin))
check('Admin role changes are server-side',lambda:sql("select public.nobet_set_role('%s','%s','admin')"%(team,viewer),admin))
sql("select public.nobet_set_role('%s','%s','viewer')"%(team,viewer),owner)
check('Published state increments revision',lambda:same(json.loads(sql(publish(0,request),owner))['revision'],1))
check('Stale administrator cannot overwrite',lambda:sql(publish(0,'10000000-0000-0000-0000-000000000002'),admin,'NOBET_CONFLICT'))
check('Retry is idempotent',lambda:same(json.loads(sql(publish(0,request),owner))['revision'],1))
changed=json.dumps({**payload,'holidays':[]})
check('Request id cannot be reused with other data',lambda:sql(publish(0,request,changed),owner,'NOBET_INVALID'))
check('Missing payload fields rejected',lambda:sql(publish(1,'10000000-0000-0000-0000-000000000003','{}'),owner,'NOBET_INVALID'))

def concurrent_publish():
    def one(i):
        cmd=publish(1,'20000000-0000-0000-0000-%012d'%i)
        try:return ('ok',json.loads(sql(cmd,owner if i==1 else admin))['revision'])
        except AssertionError as e:
            assert 'NOBET_CONFLICT' in str(e),str(e)
            return ('conflict',None)
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool: results=list(pool.map(one,[1,2]))
    same(sorted(x[0] for x in results),['conflict','ok'])
    same(json.loads(sql("select public.nobet_snapshot('%s')"%team,owner))['revision'],2)
check('Concurrent two-connection save has exactly one winner',concurrent_publish)
check('Successful old retry reads latest without rolling back new revision',lambda:same(json.loads(sql(publish(0,request),owner))['revision'],2))
sql("select public.nobet_set_role('%s','%s','viewer')"%(team,admin),owner)
check('Demoting inviter cancels pending authority grants',lambda:same(json.loads(sql("select public.nobet_members_list('%s')"%team,owner))['invites'],[{'email':'unverified@example.test','role':'admin'}]))
check('Last admin cannot downgrade self',lambda:sql("select public.nobet_set_role('%s','%s','viewer')"%(team,owner),owner,'NOBET_LAST_ADMIN'))
check('Last admin cannot remove self',lambda:sql("select public.nobet_remove_member('%s','%s')"%(team,owner),owner,'NOBET_LAST_ADMIN'))
sql("select public.nobet_remove_member('%s','%s')"%(team,viewer),owner)
check('Removed account loses read access',lambda:sql("select public.nobet_snapshot('%s')"%team,viewer,'NOBET_FORBIDDEN'))
check('Removed account cannot regain consumed invitation',lambda:(sql('select public.nobet_accept_invitations()',viewer),same(json.loads(sql('select public.nobet_my_teams()',viewer)),[])))
sql("update nobet_private.bootstrap_owner set user_id='00000000-0000-0000-0000-000000000006',email='second@example.test';")
other=sql("select public.nobet_create_team('Başka Ekip')",second)
check('Even another team admin cannot read this team',lambda:sql("select public.nobet_snapshot('%s')"%team,second,'NOBET_FORBIDDEN'))
check('Cross-team admin cannot change membership',lambda:sql("select public.nobet_set_role('%s','%s','admin')"%(other,owner),owner,'NOBET_FORBIDDEN'))
print('PostgreSQL checks: %d passed, 0 failed'%checks)

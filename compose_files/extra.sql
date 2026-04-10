
-- NOTE: when other code that will expect that to be OpenDCS-Postgres
-- Is merged in this needs to be updated... or removed, the migration should
-- really do this.
merge into tsdb_property p
using (select 'editDatabaseType' as name) prop
on (p.prop_name = prop.name)
when matched then
    update set prop_value = 'OPENTSDB'
when not matched then
    insert (prop_name, prop_value) values (prop.name, 'OPENTSDB')
;

insert into identity_provider(name,type,config) values
 ('oidc-pkce', 'OpenIdConnect', '{"clientId": "opendcs-public", "wellKnown": "http://localhost:7100/auth/realms/opendcs/.well-known/openid-configuration", "redirectUri": "http://localhost:5173/oidc-callback"}'::json),
 ('oidc-secret', 'OpenIdConnect', '{"clientId": "opendcs", "clientSecret": "test-secret-value","wellKnown": "http://localhost:7100/auth/realms/opendcs/.well-known/openid-configuration", "redirectUri": "http://localhost:5173/odcsapi/oidc-callback"}'::json)
on conflict(name) do update set type=excluded.type, config=excluded.config;
;

insert into opendcs_user(email) values ('test_user@example.com') on conflict do nothing;
insert into user_roles(user_id, role_id) values ((select id from opendcs_user where email = 'test_user@example.com'), 2) on conflict do nothing;
insert into user_roles(user_id, role_id) values ((select id from opendcs_user where email = 'test_user@example.com'), 3) on conflict do nothing;

insert into opendcs_user_password(user_id, password)
     values ((select id from opendcs_user where email = 'test_user@example.com'), '$argon2id$v=19$m=15360,t=2,p=1$bThReUZrZ0xTcjZDbzJUMA$jX7w7uTol8RON0fw3SXqghIh48jam7it6gXkg7Ul3VU') on conflict do nothing;

insert into user_identity_provider(user_id, subject, identity_provider_id) values

((select id from opendcs_user where email = 'test_user@example.com'), 'test_user', (select id from identity_provider where type = 'BuiltIn')),
((select id from opendcs_user where email = 'test_user@example.com'), '45ee99c4-3dc8-444d-81d8-2c669a148bff', (select id from identity_provider where name = 'oidc-pkce')),
((select id from opendcs_user where email = 'test_user@example.com'), '45ee99c4-3dc8-444d-81d8-2c669a148bff', (select id from identity_provider where name = 'oidc-secret'))
on conflict do nothing;

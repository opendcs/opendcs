create or replace trigger identity_provider_updated_at_trigger
	before insert on identity_provider
	for each row
	execute procedure update_updated_at();

create or replace trigger opendcs_user_password_updated_at_trigger
	before insert on opendcs_user_password
	for each row
	execute procedure update_updated_at();

create or replace trigger opendcs_user_updated_at_trigger
	before insert on opendcs_user
	for each row
	execute procedure update_updated_at();

create or replace trigger opendcs_role_updated_at_trigger
	before insert on opendcs_role
	for each row
	execute procedure update_updated_at();
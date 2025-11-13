----------------------------------------------------------------------------
-- Update the updated_at column
----------------------------------------------------------------------------
create or replace function update_updated_at () returns trigger as
$$
BEGIN
	NEW.updated_at = now();
    return NEW;
END;
$$ language plpgsql;


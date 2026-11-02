----------------------------------------------------------------------------
-- Update the updated_at column
----------------------------------------------------------------------------
create or replace function update_updated_at () returns trigger as
$$
BEGIN
    -- use clock_timestamp instead of now. now retains the value at the start of a transaction
    -- since we do a lot of tests in a transactin, including updates, we want to make
    -- sure this value will change.
	NEW.updated_at = clock_timestamp();
    return NEW;
END;
$$ language plpgsql;

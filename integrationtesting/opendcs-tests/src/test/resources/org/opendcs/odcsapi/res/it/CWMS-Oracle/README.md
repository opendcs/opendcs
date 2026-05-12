*WARNING* about the country field.

The CWMS "store_location2" method that is used to actually save or update a location (Site in OpenDCS)
Will except either a full nation name OR a 2-letter nation code.

However, the retrieval of the location will always return the full name.

Whatever input data is used, make sure the output check is against the full name.
package opendcs.opentsdb.hydrojson.beans;

/**
 * Used for POST decode payload. Contains the ApiRawMessage and the ApiPlatformConfig
 * to use for decoding.
 * @author mmaloney
 *
 */
public class DecodeRequest
{
	ApiRawMessage rawmsg = null;
	ApiPlatformConfig config = null;
	
	public ApiPlatformConfig getConfig()
	{
		return config;
	}
	public void setConfig(ApiPlatformConfig config)
	{
		this.config = config;
	}
	public ApiRawMessage getRawmsg()
	{
		return rawmsg;
	}
	public void setRawmsg(ApiRawMessage rawmsg)
	{
		this.rawmsg = rawmsg;
	}

}

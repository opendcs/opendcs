package opendcs.opentsdb.hydrojson.beans;

/** Used to encapsulate a raw message returned by GET message or sent to POST decode */
public class ApiRawMessage
{
	private String base64 = null;

	public String getBase64()
	{
		return base64;
	}

	public void setBase64(String base64)
	{
		this.base64 = base64;
	}

}

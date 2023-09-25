package org.opendcs.odcsapi.errorhandling;

public class WebAppException 
	extends Exception 
{
	private static final long serialVersionUID = 5143111031434975319L;

	/** HTTP Status Code */
	private Integer status = 0;

	/** detailed error msg */
	private String errMessage = "";	
	
	public WebAppException(Integer status, String errMessage)
	{
		super();
		this.status = status;
		this.errMessage = errMessage;
	}

	public WebAppException() { }

	public int getStatus() {
		return status;
	}

	public String getErrMessage() {
		return errMessage;
	}

	public void setErrMessage(String errMessage) { this.errMessage = errMessage; }

	public void setStatus(Integer status)
	{
		this.status = status;
	}

}

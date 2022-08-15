package opendcs.opentsdb.hydrojson.errorhandling;

import java.lang.reflect.InvocationTargetException;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class ErrorMessage 
{
	
	/** contains the same HTTP Status code returned by the server */
	@XmlElement(name = "status")
	int status;
	
	/** extra information that might useful for developers */
	@XmlElement(name = "errMessage")
	String errMessage;	

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getErrMessage() {
		return errMessage;
	}

	public void setErrMessage(String errMessage) { this.errMessage = errMessage; }

	public ErrorMessage(WebAppException ex)
	{
		this.status = ex.getStatus();
		this.errMessage = ex.getErrMessage();
	}
	
	public ErrorMessage(NotFoundException ex)
	{
		this.status = Response.Status.NOT_FOUND.getStatusCode();
		this.errMessage = ex.getMessage();
	}

	public ErrorMessage() {}
}

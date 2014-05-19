package decodes.drgsinfogui;

import java.util.Comparator;


/**
 * This class holds the DRGS Receiver Identification
 * information
 *
 */
public class DrgsReceiverIdent
{
	private String code;
	private String description; 
	private String location;
	private String contact;
	private String emailAddr;
	private String phoneNum;
	
	public DrgsReceiverIdent()
	{
		code = null;
		description = null;
		location = null;
		contact = null;
		emailAddr = null;
		phoneNum = null;
	}
	
	public DrgsReceiverIdent(String code, String description, String location,
			String contact, String emailAddr, String phoneNum)
	{
		this.code = code;
		this.description = description;
		this.location = location;
		this.contact = contact;
		this.emailAddr = emailAddr;
		this.phoneNum = phoneNum;
	}

	public String getCode()
	{
		return code;
	}

	public void setCode(String code)
	{
		this.code = code;
	}

	public String getContact()
	{
		return contact;
	}

	public void setContact(String contact)
	{
		this.contact = contact;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getEmailAddr()
	{
		return emailAddr;
	}

	public void setEmailAddr(String emailAddr)
	{
		this.emailAddr = emailAddr;
	}

	public String getLocation()
	{
		return location;
	}

	public void setLocation(String location)
	{
		this.location = location;
	}

	public String getPhoneNum()
	{
		return phoneNum;
	}

	public void setPhoneNum(String phoneNum)
	{
		this.phoneNum = phoneNum;
	}	
}

//Sort by code
class CodeColumnComparator implements Comparator
{
	public CodeColumnComparator()
	{	
	}
	public int compare(Object dr1, Object dr2)
	{
		if (dr1 == dr2)
			return 0;
		DrgsReceiverIdent d1 = (DrgsReceiverIdent) dr1;
		DrgsReceiverIdent d2 = (DrgsReceiverIdent) dr2;
		String code1 = d1.getCode();
		String code2 = d2.getCode();
		if (code1 == null)
			code1 = "";
		if (code2 == null)
			code2 = "";
		
		return code1.compareToIgnoreCase(code2);
	}
}

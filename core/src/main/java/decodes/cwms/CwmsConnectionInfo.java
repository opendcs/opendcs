package decodes.cwms;

import java.sql.Connection;

import decodes.sql.DbKey;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfo;

/**
 * Bean returned by CwmsTimeSeriesDb.getDbConnection().
 */
public class CwmsConnectionInfo
{
	private DbKey dbOfficeCode = DbKey.NullKey;
	private ConnectionLoginInfo loginInfo = null;
	private String dbOfficePrivilege = null;

	public CwmsConnectionInfo()
	{
	}

	public DbKey getDbOfficeCode()
	{
		return dbOfficeCode;
	}

	public void setDbOfficeCode(DbKey dbOfficeCode)
	{
		this.dbOfficeCode = dbOfficeCode;
	}

	public void setLoginInfo(ConnectionLoginInfo loginInfo)
	{
		this.loginInfo = loginInfo;
	}

	public ConnectionLoginInfo getLoginInfo()
	{
		return loginInfo;
	}

	public String getDbOfficePrivilege()
	{
		return dbOfficePrivilege;
	}

	public void setDbOfficePrivilege(String dbOfficePrivilege)
	{
		this.dbOfficePrivilege = dbOfficePrivilege;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("CwmsConnectionInfo{");
		builder.append("priv=").append(this.dbOfficePrivilege).append(",")
		       .append("OfficeCode=").append(dbOfficeCode.getValue()).append(",")
			   .append("loginInfo=");
		if (loginInfo != null)
		{
			String url = loginInfo.getUrl();
			String user = loginInfo.getUser();
			String office = loginInfo.getUserOfficeId();
			builder.append("LoginInfo{")
				   .append("url=").append(url).append(",")
				   .append("user=").append(user).append(",")
				   .append("officeId=").append(office);
			builder.append("}");
		}
		else
		{
			builder.append("{uninitialized}");
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public boolean equals(Object other) {
		if(!(other instanceof CwmsConnectionInfo))
		{
			return false;
		}
		else
		{
			CwmsConnectionInfo oc = (CwmsConnectionInfo)other;
			return this.dbOfficeCode == oc.dbOfficeCode 
				&& this.dbOfficePrivilege.equals(oc.dbOfficePrivilege)
				&& this.loginInfo.getUrl().equals(oc.loginInfo.getUrl())
				&& this.loginInfo.getUser().equals(oc.getLoginInfo().getUser());
		}		
	}

}

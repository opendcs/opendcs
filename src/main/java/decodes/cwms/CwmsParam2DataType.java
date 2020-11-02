/*
 * $Id$
 * 
 * $Log$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.cwms;

import java.sql.ResultSet;

import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.tsdb.TsdbAppTemplate;

/**
 * Copies CWMS Parameters to DECODES Data Type records.
 * @author mmaloney, Mike Maloney, Cove Software LLC
 */
public class CwmsParam2DataType extends TsdbAppTemplate
{
	public CwmsParam2DataType()
	{
		super("util.log");
	}

	@Override
	protected void runApp() throws Exception
	{
		String q = "select distinct parameter_id from cwms_v_parameter";
		ResultSet rs = theDb.doQuery(q);
		int numNew = 0;
		while(rs != null && rs.next())
		{
			String dtCode = rs.getString(1);
			DataType dt = DataType.getDataType(
				Constants.datatype_CWMS, dtCode);
			if (dt.getId().isNull())
			{
				System.out.println("New CWMS Data Type: " + dt.getCode());
				numNew++;
			}
		}
		if (numNew > 0)
		{
			System.out.println("Writing " + numNew + " CWMS data types to DECODES db.");
			Database.getDb().dataTypeSet.write();
		}
	}
	
	/** Main method */
	public static void main(String[] args)
		throws Exception
	{
		CwmsParam2DataType app = new CwmsParam2DataType();
			app.execute(args);
	}
}

/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.opendcs.odcsapi.beans.ApiAlgorithm;
import org.opendcs.odcsapi.beans.ApiConfigScriptSensor;
import org.opendcs.odcsapi.beans.ApiDataType;
import org.opendcs.odcsapi.beans.ApiSeason;
import org.opendcs.odcsapi.beans.ApiUnit;
import org.opendcs.odcsapi.beans.ApiUnitConverter;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;

public class ApiUnitDAO
	extends ApiDaoBase
{
	public static String module = "ApiUnitDAO";

	public ApiUnitDAO(DbInterface dbi)
	{
		super(dbi, module);
	}


	public ArrayList<ApiUnit> getUnitList() 
		throws DbException
	{
		ArrayList<ApiUnit> ret = new ArrayList<ApiUnit>();
		
		String q = "select UNITABBR, NAME, FAMILY, MEASURES"
			+ " from ENGINEERINGUNIT order by unitabbr";
		
		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				ApiUnit du = new ApiUnit();
				du.setAbbr(rs.getString(1));
				du.setName(rs.getString(2));
				du.setFamily(rs.getString(3));
				du.setMeasures(rs.getString(4));
				ret.add(du);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}


	public ArrayList<ApiDataType> getDataTypeList(String standard)
		throws DbException
	{
		if (standard != null)
		    standard = getSingleWord(standard);
		ArrayList<ApiDataType> ret = new ArrayList<ApiDataType>();
		
		String q = "select ID, STANDARD, CODE, DISPLAY_NAME from DATATYPE";
		if (standard != null)
		{
			q = q + " where lower(STANDARD) = ?";
		}
		q = q + " order by STANDARD, CODE";
		try
		{
			ResultSet rs;
			Connection conn = null;
			if (standard != null)
			{
				rs = doQueryPs(conn, q, standard.toLowerCase());
			}
			else
			{
				rs = doQuery(q);
			}
			while (rs.next())
			{
				ApiDataType dt = new ApiDataType();
				dt.setId(rs.getLong(1));
				dt.setStandard(rs.getString(2));
				dt.setCode(rs.getString(3));
				dt.setDisplayName(rs.getString(4));
				
				ret.add(dt);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	/**
	 * Return a list of converters.
	 * @return
	 * @throws DbException
	 */
	public ArrayList<ApiUnitConverter> getConverterList(boolean includeRaw)
		throws DbException
	{
		String q = "select uc.ID, uc.FROMUNITSABBR, uc.TOUNITSABBR, uc.ALGORITHM,"
				+ " uc.A, uc.B, uc.C, uc.D, uc.E, uc.F"
				+ " from UNITCONVERTER uc";
		if (!includeRaw)
		{
			q = q + " where lower(uc.FROMUNITSABBR) != ?";
		}
		
		ArrayList<ApiUnitConverter> ret = new ArrayList<ApiUnitConverter>();
		try 
		{
			ResultSet rs;
			Connection conn = null;
			if (!includeRaw)
			{
				rs = doQueryPs(conn, q, "raw");
			}
			else
			{
				rs = doQuery2(q);
			}
			while(rs.next())
			{
				ApiUnitConverter uc = new ApiUnitConverter();
				
				uc.setUcId(rs.getLong(1));
				uc.setFromAbbr(rs.getString(2));
				uc.setToAbbr(rs.getString(3));
				uc.setAlgorithm(rs.getString(4));
				double d = rs.getDouble(5);
				if (!rs.wasNull())
					uc.setA(d);
				d = rs.getDouble(6);
				if (!rs.wasNull())
					uc.setB(d);
				d = rs.getDouble(7);
				if (!rs.wasNull())
					uc.setC(d);
				d = rs.getDouble(8);
				if (!rs.wasNull())
					uc.setD(d);
				d = rs.getDouble(9);
				if (!rs.wasNull())
					uc.setE(d);
				d = rs.getDouble(10);
				if (!rs.wasNull())
					uc.setF(d);
				ret.add(uc);
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
		return ret;
	}


	public void writeEU(ApiUnit eu, String fromabbr)
		throws DbException, WebAppException, SQLException
	{
	    System.out.println("Before get abbr");
		String abbr = eu.getAbbr();
		System.out.println("After get abbr.");
		if (abbr == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "Engingeering Unit Abbreviation cannot be null.");
		//abbr = abbr.trim();

		//String q = "select UNITABBR from ENGINEERINGUNIT where lower(UNITABBR) = "
		//	+ sqlString(abbr.toLowerCase());
		String q = "select UNITABBR from ENGINEERINGUNIT where lower(UNITABBR) = ?";
		String abbrLower = abbr.toLowerCase();
		
        System.out.println("Before Try");
        try
        {
            Connection conn = null;
            ResultSet rs = doQueryPs(conn, q, abbrLower);
            boolean abbrExists = true;
            if (!rs.next())
                abbrExists = false;
            
            String theType = "update";
            if (fromabbr == null)
            {
                System.out.println("In the from abbr is null");
                theType = "new";
                if (abbrExists)
                {
                    System.out.println("Error 1");
                    throw new WebAppException(ErrorCodes.IO_ERROR,
                            "You are trying to create an engineering unit, but that unit already exists.");
                }
                else
                {
                    System.out.println("Insert 1");
                    q = "insert into ENGINEERINGUNIT values (?,?,?,?)";
                    doModifyV(q, eu.getAbbr(), eu.getName(), eu.getFamily(), eu.getMeasures());
                }
            }
            else
            {
                System.out.println("from abbr not null");
                q = "select UNITABBR from ENGINEERINGUNIT where lower(UNITABBR) = ?";
                rs = doQueryPs(conn, q, fromabbr.toLowerCase());
                boolean fromAbbrExists = true;
                if (!rs.next())
                    fromAbbrExists = false;
                if (!fromAbbrExists)
                {
                    System.out.println("Error 2");
                    throw new WebAppException(ErrorCodes.IO_ERROR,
                            "You are trying to update an engineering unit, but the one you are updating from does not exist.");
                }
                else if (abbrExists && !fromabbr.toLowerCase().contentEquals(abbrLower))
                {
                    System.out.println("Error 3");
                    System.out.println("ABBR Lower: " + abbrLower);
                    System.out.println("From ABBR Lower: " + fromabbr.toLowerCase());
                    throw new WebAppException(ErrorCodes.IO_ERROR,
                            "You are trying to create an engineering unit, but that unit you are updating to already exists.");
                }
                else
                {
                    System.out.println("Update 1");
                    q = "update ENGINEERINGUNIT set UNITABBR = ?, NAME = ?, FAMILY = ?, MEASURES = ? "
                            + "where lower(UNITABBR) = ?";
                    doModifyV(q, eu.getAbbr(), eu.getName(), eu.getFamily(), eu.getMeasures(), fromabbr.toLowerCase());
                }
            }
        }
        finally
        {
            System.out.println("This is complete.");
        }
	}
	
	public void deleteEU(String abbr)
		throws DbException
	{
		if (abbr == null)
			return;
		String q = "delete from ENGINEERINGUNIT where lower(UNITABBR) = ?";
		doModifyV(q, abbr.toLowerCase());
	}


	public void writeEUConv(ApiUnitConverter euc)
		throws DbException, WebAppException
	{
		if (euc.getFromAbbr() == null || euc.getToAbbr() == null)
			throw new WebAppException(ErrorCodes.MISSING_ID,
				"Both from and to unit abbreviations are required for unit converter.");
		
		String q = null;
		if (euc.getUcId() != null)
		{
			q = "update UNITCONVERTER set FROMUNITSABBR = ?, TOUNITSABBR = ?,"
				+ "ALGORITHM = ?, A = ?, B = ?, C = ?, D = ?, E = ?, F = ? "
				+ "where ID = ?";
			if (doModifyV(q, euc.getFromAbbr(), euc.getToAbbr(),
				euc.getAlgorithm(), euc.getA(), euc.getB(), euc.getC(),
				euc.getD(), euc.getE(), euc.getF(), euc.getUcId()) == 0)
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
					"No matching unit conerter with ID=" + euc.getUcId());
		}
		else // write new converter
		{
			q = "insert into UNITCONVERTER values(?,?,?,?,?,?,?,?,?,?)";
			doModifyV(q, getKey("UNITCONVERTER"), euc.getFromAbbr(),
				euc.getToAbbr(), euc.getAlgorithm(), euc.getA(), 
				euc.getB(), euc.getC(), euc.getD(), euc.getE(), euc.getF());
		}
	}

	public void deleteEUConv(Long id) 
		throws DbException
	{
		String q = "delete from UNITCONVERTER where id = ?";
		doModifyV(q, id);
	}
}

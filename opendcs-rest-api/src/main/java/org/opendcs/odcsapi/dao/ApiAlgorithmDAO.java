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

import org.opendcs.odcsapi.beans.ApiAlgorithmRef;
import org.opendcs.odcsapi.beans.ApiAlgoParm;
import org.opendcs.odcsapi.beans.ApiAlgorithm;
import org.opendcs.odcsapi.beans.ApiAlgorithmScript;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.Base64;

/**
 */
public class ApiAlgorithmDAO
	extends ApiDaoBase
{
	String q = "";
	
	public ApiAlgorithmDAO(DbInterface dbi)
	{
		super(dbi, "ApiAlgorithmDao");
	}

	public ArrayList<ApiAlgorithmRef> getAlgorithmRefs()
		throws DbException
	{
		ArrayList<ApiAlgorithmRef> ret = new ArrayList<ApiAlgorithmRef>();

		q =" select a.algorithm_id, a.algorithm_name, a.exec_class, a.cmmnt "
			+ "from CP_ALGORITHM a "
			+ "order by a.ALGORITHM_ID";
				
		try
		{
			ResultSet rs = doQuery(q);
			while(rs.next())
			{
				ApiAlgorithmRef ref = new ApiAlgorithmRef();
				ref.setAlgorithmId(rs.getLong(1));
				ref.setAlgorithmName(rs.getString(2));
				ref.setExecClass(rs.getString(3));
				ref.setDescription(rs.getString(4));
				ret.add(ref);
			}
			
			q = "select a.algorithm_id, count(1) as CompsUsingAlgo "
				+ "from cp_algorithm a, cp_computation b "
				+ "where a.algorithm_id = b.algorithm_id "
				+ "group by a.algorithm_id";
			while(rs.next())
			{
				long algoId = rs.getLong(1);
				for(ApiAlgorithmRef ref : ret)
					if (ref.getAlgorithmId() == algoId)
					{
						ref.setNumCompsUsing(rs.getInt(2));
						break;
					}
			}
			return ret;
		}
		catch(Exception ex)
		{
			throw new DbException(module, ex, "getAlgorithmRefs error in query '" + q + "': " + ex);
		}
	}

	public ApiAlgorithm getAlgorithm(Long algoId)
		throws DbException, WebAppException
	{
		ApiAlgorithm ret = new ApiAlgorithm();
		q =" select a.algorithm_id, a.algorithm_name, a.exec_class, a.cmmnt "
				+ "from CP_ALGORITHM a "
				+ "where a.algorithm_id = ?";
					
		try
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, algoId);
			if(rs.next())
			{
				ret.setAlgorithmId(rs.getLong(1));
				ret.setName(rs.getString(2));
				ret.setExecClass(rs.getString(3));
				ret.setDescription(rs.getString(4));
			}
			else
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
					"No Computation Algorithm with id=" + algoId);

				
			//q = "select count(*) from cp_computation "
			//	+ "where algorithm_id = " + algoId;
			if (rs.next())
				ret.setNumCompsUsing(rs.getInt(2));
			
			q = "select ALGO_ROLE_NAME, PARM_TYPE from CP_ALGO_TS_PARM "
				+ "where ALGORITHM_ID = ?";
			
			rs =doQueryPs(conn, q, algoId);
			while(rs.next())
			{
				ApiAlgoParm parm = new ApiAlgoParm();
				parm.setRoleName(rs.getString(1));
				parm.setParmType(rs.getString(2));
				ret.getParms().add(parm);
			}

			q = "select PROP_NAME, PROP_VALUE from CP_ALGO_PROPERTY "
					+ "where ALGORITHM_ID = ?";
			rs = doQueryPs(conn, q, algoId);
			while(rs.next())
				ret.getProps().setProperty(rs.getString(1), rs.getString(2));
			
			q = "select SCRIPT_TYPE, SCRIPT_DATA from CP_ALGO_SCRIPT "
				+ "where ALGORITHM_ID = ? order by SCRIPT_TYPE, BLOCK_NUM";
			rs = doQueryPs(conn, q, algoId);
			ApiAlgorithmScript script = null;
			while(rs.next())
			{
				String s = rs.getString(1);
				if (s == null || s.length() == 0)
					continue;
				char scriptType = s.charAt(0);
				String b64 = rs.getString(2);
				String scriptData = new String(Base64.decodeBase64(b64.getBytes()));
				
				// If new script
				if (script == null || script.getScriptType() != scriptType)
				{
					script = new ApiAlgorithmScript();
					script.setScriptType(scriptType);
					script.setText(scriptData);
					ret.getAlgoScripts().add(script);
				}
				else // tack this block onto existing script
					script.setText(script.getText() + scriptData);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
	}

	public ApiAlgorithm writeAlgorithm(ApiAlgorithm algo)
		throws DbException, WebAppException, SQLException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		
		//q = "select algorithm_id from CP_ALGORITHM where lower(ALGORITHM_NAME) = " 
		//		+ sqlString(algo.getName().toLowerCase());
		
		q = "select algorithm_id from CP_ALGORITHM where lower(ALGORITHM_NAME) = ?"; 
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(algo.getName().toLowerCase());
		if (algo.getAlgorithmId() != null)
		{
			q = q + " and algorithm_id != ?";
			args.add(algo.getAlgorithmId());
		}
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, args.toArray());
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write algorithm with name '" + algo.getName() 
					+ "' because another compuation with id=" + rs.getLong(1) 
					+ " also has that name.");
			
			if (algo.getAlgorithmId() == null)
				insert(algo);
			else
				update(algo);
		}
		catch (SQLException ex)
		{
			throw new DbException(module, ex, "writeAlgorithm Error in query '" + q + "': " + ex);
		}

		return algo;
	}

	private void insert(ApiAlgorithm algo)
		throws DbException
	{
		algo.setAlgorithmId(getKey("CP_ALGORITHM"));
		
		q = "insert into CP_ALGORITHM(algorithm_id, algorithm_name, "
			+ "exec_class, cmmnt) "
			+ "values(?, ?, ?, ?)";
		doModifyV(q, algo.getAlgorithmId(), algo.getName(), algo.getExecClass(), algo.getDescription());

		writeSubordinates(algo);
	}

	private void update(ApiAlgorithm algo)
			throws DbException
		{
			deleteSubordinates(algo.getAlgorithmId());
			q = "update CP_ALGORITHM set " 
				+ "algorithm_name = ?, "
				+ "exec_class = ?, "
				+ "cmmnt = ?"
				+ " where ALGORITHM_ID = ?";
			doModifyV(q, algo.getName(), algo.getExecClass(), algo.getDescription(), algo.getAlgorithmId());
			writeSubordinates(algo);
		}

			
		private void deleteSubordinates(Long algorithmId)
			throws DbException
		{
			q = "delete from CP_ALGO_PROPERTY where ALGORITHM_ID = ?";
			doModifyV(q, algorithmId);
			q = "delete from CP_ALGO_SCRIPT where ALGORITHM_ID = ?";
			doModifyV(q, algorithmId);
			q = "delete from CP_ALGO_TS_PARM where ALGORITHM_ID = ?";
			doModifyV(q, algorithmId);
		}
	
		
	private void writeSubordinates(ApiAlgorithm algo)
			throws DbException
		{
			q = "";
			for (ApiAlgoParm parm : algo.getParms())
			{
				q = "insert into CP_ALGO_TS_PARM(ALGORITHM_ID, ALGO_ROLE_NAME, PARM_TYPE) "
					+ "values(?, ?, ?)";
				doModifyV(q, algo.getAlgorithmId(), parm.getRoleName(), parm.getParmType());
			}
			
			for(Object k : algo.getProps().keySet())
			{
				String n = (String)k;
				String v = algo.getProps().getProperty(n);

				q = "insert into CP_ALGO_PROPERTY(ALGORITHM_ID, PROP_NAME, PROP_VALUE)  "
					+ "values(?, ?, ?)";
				doModifyV(q, algo.getAlgorithmId(), n, v);
			}
			
			for(ApiAlgorithmScript script : algo.getAlgoScripts())
			{
				String text = script.getText();
				if (text == null || text.length() == 0)
					continue;
				// Have to convert to Base64 to preserve quotes, newlines, etc.
				String b64 = new String(Base64.encodeBase64(text.getBytes()));
				int blockNum = 1;
				while(b64 != null)
				{
					String block = b64.length() < 4000 ? b64 : b64.substring(0, 4000);
					b64 = (block.equals(b64)) ? null : b64.substring(4000);
					q = "INSERT INTO CP_ALGO_SCRIPT(ALGORITHM_ID, SCRIPT_TYPE, BLOCK_NUM, SCRIPT_DATA) "
						+ " VALUES(?, ?, ?, ?, ?)";
					doModifyV(q, algo.getAlgorithmId(), script.getScriptType(), (blockNum++), block);
				}
			}
		}
	
	public void deleteAlgorithm(Long algorithmId)
		throws DbException, WebAppException, SQLException
	{
		q = "select count(*) from cp_computation "
				+ "where algorithm_id = ?";
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, algorithmId);
		
		try
		{
			if (rs.next())
			{
				int n = rs.getInt(1);
				if (n > 0)
					throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
						"Cannot delete algorithm with id=" + algorithmId
						+ " because it is used by " + n + " compuations.");
			}
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "deleteAlgorithm error in query '" + q + "': " + ex);
		}
		
		deleteSubordinates(algorithmId);
		q = "delete from CP_ALGORITHM where ALGORITHM_ID = ?";
		doModifyV(q, algorithmId);
	}
}

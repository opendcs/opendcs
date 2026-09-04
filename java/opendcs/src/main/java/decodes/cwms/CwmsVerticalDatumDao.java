/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.cwms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.Locale;

import org.opendcs.database.OracleSqlExceptionHelper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;

import decodes.tsdb.DbIoException;

/**
 * DAO for CWMS vertical datum operations, specifically
 * cwms_loc.get_vertical_datum_offset.
 */
public class CwmsVerticalDatumDao extends DaoBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private final String defaultOfficeId;

	public CwmsVerticalDatumDao(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "CwmsVerticalDatumDao");
		if (!(tsdb instanceof CwmsTimeSeriesDb))
		{
			throw new IllegalArgumentException(
				"CwmsVerticalDatumDao requires a CwmsTimeSeriesDb");
		}
		this.defaultOfficeId = ((CwmsTimeSeriesDb) tsdb).getDbOfficeId();
	}

	/**
	 * Calls cwms_loc.get_vertical_datum_offset and returns the numeric offset.
	 *
	 * @param locationId CWMS location ID (p_location_id)
	 * @param datum1     input vertical datum (p_vertical_datum_id_1)
	 * @param datum2     output vertical datum (p_vertical_datum_id_2)
	 * @param datetime   effective time for offset selection (p_datetime)
	 * @param timeZone   timezone for the effective time; null to let CWMS use the location timezone
	 * @param unit       units of the input value (p_unit)
	 * @param officeIdOverride optional office ID override (p_office_id), null to use DB office
	 */
	public double getVerticalDatumOffset(
		String locationId,
		String datum1,
		String datum2,
		Date datetime,
		String timeZone,
		String unit,
		String officeIdOverride
	) throws DbIoException, NoVerticalDatumMappingException
	{
		if (locationId == null || locationId.trim().isEmpty())
		{
			throw new DbIoException("locationId is required for vertical datum offset lookup.");
		}
		if (datum1 == null || datum2 == null)
		{
			throw new DbIoException("datum1 and datum2 are required for vertical datum offset lookup.");
		}
		if (unit == null || unit.trim().isEmpty())
		{
			throw new DbIoException("unit is required for vertical datum offset lookup.");
		}

		String d1 = datum1.trim().toUpperCase(Locale.US);
		String d2 = datum2.trim().toUpperCase(Locale.US);
		String officeId = officeIdOverride != null && !officeIdOverride.trim().isEmpty()
			? officeIdOverride.trim()
			: defaultOfficeId;

		String sql =
			"SELECT cwms_loc.get_vertical_datum_offset(" +
			"?, ?, ?, ?, ?, ?, ?) FROM dual";

		log.trace("CwmsVerticalDatumDao.getVerticalDatumOffset loc={}, {}->{} at {} unit={} office={}",
				  locationId, d1, d2, datetime, unit, officeId);

		try (Connection conn = getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql))
		{
			int idx = 1;

			// p_location_id
			stmt.setString(idx++, locationId);

			// p_vertical_datum_id_1, p_vertical_datum_id_2
			stmt.setString(idx++, d1);
			stmt.setString(idx++, d2);

			// p_datetime (full timestamp)
			if (datetime != null)
			{
				stmt.setTimestamp(idx++, new Timestamp(datetime.getTime()));
			}
			else
			{
				stmt.setNull(idx++, Types.TIMESTAMP);
			}

			// p_time_zone
			if (timeZone != null && !timeZone.trim().isEmpty())
			{
				stmt.setString(idx++, timeZone.trim());
			}
			else
			{
				stmt.setNull(idx++, Types.VARCHAR);
			}

			// p_unit
			stmt.setString(idx++, unit);

			// p_office_id
			if (officeId != null && !officeId.isEmpty())
			{
				stmt.setString(idx++, officeId);
			}
			else
			{
				stmt.setNull(idx++, Types.VARCHAR);
			}

			try (ResultSet rs = stmt.executeQuery())
			{
				if (!rs.next())
				{
					throw new DbIoException(
						"cwms_loc.get_vertical_datum_offset returned no rows " +
						"for location=" + locationId + ", " + d1 + "->" + d2);
				}

				double offset = rs.getDouble(1);
				if (rs.wasNull())
				{
					throw new DbIoException(
						"cwms_loc.get_vertical_datum_offset returned NULL " +
						"for location=" + locationId + ", " + d1 + "->" + d2);
				}

				return offset;
			}
		}
		catch (SQLException ex)
		{
			String msg = ex.getMessage();
			boolean userDefined = OracleSqlExceptionHelper.isUserDefinedError(ex);

			if (msg != null && msg.contains("Cannot convert between vertical datums"))
			{
				// Specific CWMS error: no mapping between requested datums
				throw new NoVerticalDatumMappingException(
					"Cannot convert between vertical datums " + d1 + " and " + d2 +
					" for location " + locationId + " (unit=" + unit + ", office=" + officeId + ")",
					ex);
			}

			if (userDefined)
			{
				// Other CWMS business-rule error
				throw new DbIoException(
					"CWMS user-defined error calling cwms_loc.get_vertical_datum_offset " +
					"for location=" + locationId + ", " + d1 + "->" + d2, ex);
			}

			// Connection/system-level or unknown error
			throw new DbIoException(
				"Database error calling cwms_loc.get_vertical_datum_offset " +
				"for location=" + locationId + ", " + d1 + "->" + d2, ex);
		}
	}
}

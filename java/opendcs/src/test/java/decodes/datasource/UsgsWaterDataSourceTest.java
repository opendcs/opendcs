/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
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
package decodes.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;
import java.util.Vector;

import org.junit.jupiter.api.Test;

import decodes.db.DataSource;
import decodes.db.NetworkList;

class UsgsWaterDataSourceTest
{
	/**
	 * Construct with null DataSource and null Database.
	 * The base class comment says super(null, null) is fine.
	 */
	private UsgsWaterDataSource createSource()
	{
		DataSource ds = new DataSource("test-usgs", "usgs-waterdata");
		return new UsgsWaterDataSource(ds, null);
	}

	@Test
	void testSupportsTimeRanges()
	{
		UsgsWaterDataSource src = createSource();
		assertTrue(src.supportsTimeRanges());
	}

	@Test
	void testCloseDoesNotThrow()
	{
		UsgsWaterDataSource src = createSource();
		assertDoesNotThrow(src::close);
	}

	@Test
	void testInitWithNullNetlists()
	{
		UsgsWaterDataSource src = createSource();
		Properties props = new Properties();
		DataSourceException ex = assertThrows(DataSourceException.class,
			() -> src.init(props, null, null, null));
		assertTrue(ex.getMessage().contains("No medium ids"));
	}

	@Test
	void testInitWithEmptyNetlists()
	{
		UsgsWaterDataSource src = createSource();
		Properties props = new Properties();
		Vector<NetworkList> netlists = new Vector<>();
		DataSourceException ex = assertThrows(DataSourceException.class,
			() -> src.init(props, null, null, netlists));
		assertTrue(ex.getMessage().contains("No medium ids"));
	}

	@Test
	void testInitWithEmptyNetworkList()
	{
		UsgsWaterDataSource src = createSource();
		Properties props = new Properties();
		Vector<NetworkList> netlists = new Vector<>();
		NetworkList nl = new NetworkList();
		nl.transportMediumType = "usgs-waterdata";
		netlists.add(nl);
		// NetworkList has no entries, so aggIds stays empty
		DataSourceException ex = assertThrows(DataSourceException.class,
			() -> src.init(props, null, null, netlists));
		assertTrue(ex.getMessage().contains("No medium ids"));
	}
}

/**
 * $Id$
 * 
 * $Log$
 *
 * Copyright 2014 Cove Software, LLC
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.dcpmon;

import java.util.ArrayList;
import java.util.HashSet;

import lrgs.common.DcpAddress;

import decodes.db.IncompleteDatabaseException;
import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.RoutingSpec;
import decodes.util.Pdt;
import decodes.util.PdtEntry;

public class DcpMonRoutingSpec extends RoutingSpec
{
	/**
	 * Constructs a DcpMonRoutingSpec from the 'dcpmon' routing spec
	 * stored in the database
	 * @param copyFrom the database routing spec
	 */
	public DcpMonRoutingSpec()
	{
		
	}
	
	@Override
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		// Base class will retrieve the netlists and prepare each of them.
		super.prepareForExec();
		
		if (DcpMonitorConfig.instance().allChannels
		 || networkLists.size() == 0)
		{
			// Remove all network lists from the object so that it's wide open.
			networkLists.clear();
		}
		else
		{
			// Figure out what GOES channels are covered by the network lists.
			HashSet<Integer> chanSet = new HashSet<Integer>();
			Pdt pdt = Pdt.instance();
			for(NetworkList nl : networkLists)
				if (nl.transportMediumType.toLowerCase().startsWith("goes"))
					for(NetworkListEntry nle : nl.values())
					{
						PdtEntry pdtEntry = pdt.find(new DcpAddress(nle.transportId));
						if (pdtEntry != null)
						{
							chanSet.add(pdtEntry.st_channel);
							chanSet.add(pdtEntry.rd_channel);
						}
					}
			chanSet.remove(0);
			
			// Clear the network lists. We will use channel set instead.
			networkLists.clear();
			
			// Remove the old channels from the search criteria in properties
			ArrayList<String> toRemove = new ArrayList<String>();
			for(Object keyobj : getProperties().keySet())
			{
				String key = (String)keyobj;
				if (key.toLowerCase().startsWith("sc:channel"))
					toRemove.add(key);
			}
			for(String key : toRemove)
				getProperties().remove(key);
			
			// Add the channels to the search criteria in properties.
			int idx = 0;
			for(Integer chan : chanSet)
				setProperty("sc:CHANNEL_" + idx++, ""+chan);
		}
	}
}

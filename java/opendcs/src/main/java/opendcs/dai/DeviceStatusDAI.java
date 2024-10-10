/*
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * 
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
package opendcs.dai;

import java.util.ArrayList;

import decodes.polling.DeviceStatus;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;

/**
 * Data Access Interface for Device Status Objects used by the DECODES Polling Module.
 */
public interface DeviceStatusDAI
{
	/** Retrieve all device status structures currently defined in the database */
	public ArrayList<DeviceStatus> listDeviceStatuses()
		throws DbIoException;
	
	/** 
	 * Retrieve a particular device status by name.
	 * @param portName unique name of the device
	 * @return DeviceStatus structure or null if none exists in the database.
	 * @throws DbIoException
	 */
	public DeviceStatus getDeviceStatus(String portName)
		throws DbIoException;

	/**
	 * Writes a device status structure to the database.
	 * @param deviceStatus the structure to write.
	 * @throws DbIoException
	 */
	public void writeDeviceStatus(DeviceStatus deviceStatus)
		throws DbIoException;

	/**
	 * Closes any resources opened by the DAO
	 */
	public void close();

}

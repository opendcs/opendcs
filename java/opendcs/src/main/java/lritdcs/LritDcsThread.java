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
package lritdcs;

import java.util.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

public abstract class LritDcsThread extends Thread implements Observer
{
	/// Stop the current execution, perhaps to reinit.
	protected boolean shutdownFlag;

	public LritDcsThread(String name)
	{
		super(name);
		shutdownFlag = false;
	}

	// Run method provided by sub-class. Should exit when shutdownFlag is set.

	public void shutdown()
	{
		shutdownFlag = true;
	}

	public boolean isShutdown() { return shutdownFlag; }

	/**
	  Overloaded from the Oberserver pattern, this method is called whenever
	  the configuration has changed. It calls getConfigValues.
	*/
	public void update(Observable obs, Object obj)
	{
		getConfigValues(LritDcsConfig.instance());
	}

	protected void registerForConfigUpdates()
	{
		LritDcsConfig.instance().addObserver(this);
	}

	protected abstract void getConfigValues(LritDcsConfig cfg);

	@Deprecated
	public void debug1(String msg)
	{
	}

	@Deprecated
	public void debug2(String msg)
	{
	}

	@Deprecated
	public void debug3(String msg)
	{
	}

	@Deprecated
	public void info(int evtnum, String msg)
	{
	}
	@Deprecated
	public void warning(int evtnum, String msg)
	{
	}
	@Deprecated
	public void failure(int evtnum, String msg)
	{
	}
	@Deprecated
	public void fatal(int evtnum, String msg)
	{
	}
}

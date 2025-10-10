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
package lrgs.lrgsmon;


/**
ThreadBase is the base class for threads in the LRGS Monitor
application. It provides convenience methods for logging and shutdown
control.
*/
public abstract class ThreadBase
	extends Thread
{
	/// Stop the current execution, perhaps to reinit.
	protected boolean shutdownFlag;

	/// Base class specifies name.
	public ThreadBase(String name)
	{
		super(name);
		shutdownFlag = false;
	}

	/// Call this method to shutdown this thread.
	public void shutdown()
	{
		shutdownFlag = true;
	}

	/// Returns true if this thread has been shutdown.
	public boolean isShutdown() { return shutdownFlag; }

}
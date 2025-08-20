/*
 * $Id$
 * 
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
package decodes.polling;


public interface StreamReaderOwner
{
	/**
	 * Called by the input StreamReader when an IOException has occurred.
	 * It means that the input stream from the station has failed.
	 * @param ex the exception thrown
	 */
	void inputError(Exception ex);
	
	/**
	 * Called by the input StreamReader when end of stream (-1) is received.
	 * This may be normal or abnormal.
	 */
	void inputClosed();
	
	public String getModule();

}

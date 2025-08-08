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
package decodes.consumer;

import java.util.Properties;
import decodes.decoder.DecodedMessage;

/**
  StringBufferConsumer writes data to a StringBuffer. It is used by GUI
  applications that want to display data in GUI components.
*/
public class StringBufferConsumer extends DataConsumer
{
	/** The string buffer */
	StringBuffer sb;
	/** The line separator to use */
	String lineSep;

	/**
	  Constructs new object with a specified StringBuffer.
	*/
	public StringBufferConsumer(StringBuffer sb)
	{
		super();
		this.sb = sb;
		lineSep = System.getProperty("line.separator");
		if (lineSep == null)
			lineSep = "\n";
	}

	/**
	  Opens and initializes the consumer.
	  @param consumerArg ignored
	  @param props ignored
	  @throws DataConsumerException if the consumer could not be initialized.
	*/
	public void open(String consumerArg, Properties props)
		throws DataConsumerException
	{
		sb.delete(0, sb.length());
	}

	/** Does nothing. */
	public void close()
	{
	}

	/** 
	  Zeros the string buffer.
	  @param msg ignored.
	*/
	public void startMessage(DecodedMessage msg)
	{
		sb.delete(0, sb.length());
	}

	/**
	  Appends a line to the buffer.
	  @param line the line
	*/
	public void println(String line)
	{
		sb.append(line);
		sb.append(lineSep);
	}

	/** Does nothing. */
	public void endMessage()
	{
		// does not need to delimit the end of a message.
	}

	/**
	  @return the StringBuffer.
	*/
	public StringBuffer getBuffer() { return sb; }

	/** @return "StringBuffer" */
	public String getActiveOutput() { return "StringBuffer"; }
	
	@Override
	public String getArgLabel()
	{
		return null;
	}

}


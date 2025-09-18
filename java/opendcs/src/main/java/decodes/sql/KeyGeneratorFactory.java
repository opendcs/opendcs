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
package decodes.sql;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.DatabaseException;

/**
This class contains a static method that can create a key-generator
given a class name.
*/
public class KeyGeneratorFactory
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/**
	 * Makes and returns a key generator, given a class name.
	 * Also calls the generator's init method with the passed connection object.
	 * @param clsname the class name.
	 * @return the key generator object.
	 */
	public static KeyGenerator makeKeyGenerator(String clsname)
		throws DatabaseException
	{
		log.trace("Making KeyGenerator for class '{}'", clsname);
		try
		{
			Class cls = 
			  Thread.currentThread().getContextClassLoader().loadClass(clsname);
			KeyGenerator keyGenerator = (KeyGenerator)cls.newInstance();
			return keyGenerator;
		}
		catch(ClassNotFoundException ex)
		{
			String err = "Cannot load KeyGenerator from class name '"
			  + clsname + "' (Check configuration and CLASSPATH setting)";
			throw new DatabaseException(err,ex);
		}
		catch(InstantiationException ex)
		{
			String err = "Cannot instantiate KeyGenerator of type '"
			  + clsname + "': (Check configuration and CLASSPATH setting)";
			throw new DatabaseException(err,ex);
		}
		catch(IllegalAccessException ex)
		{
			String err = "Cannot instantiate KeyGenerator of type '"
			  + clsname + "': (Does class have public no-arg constructor?)"; 
			throw new DatabaseException(err,ex);
		}
	}
}

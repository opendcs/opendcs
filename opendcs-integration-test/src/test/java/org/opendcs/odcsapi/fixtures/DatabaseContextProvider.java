/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
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
package org.opendcs.odcsapi.fixtures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.PreconditionViolationException;
import org.opendcs.fixtures.spi.ConfigurationProvider;

public class DatabaseContextProvider implements TestTemplateInvocationContextProvider
{

	@Override
	public boolean supportsTestTemplate(ExtensionContext context)
	{
		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context)
	{
		return Arrays.stream(DbType.values())
				.filter(d -> System.getProperty("opendcs.test.integration.db").contains(d.toString()))
				.map(DatabaseInvocationContext::new);
	}

	private static class DatabaseInvocationContext implements TestTemplateInvocationContext
	{
		private static final List<Extension> EXTENSIONS = new ArrayList<>();
		private final DbType dbType;

		public DatabaseInvocationContext(DbType dbType)
		{
			this.dbType = dbType;
		}

		@Override
		public String getDisplayName(int invocationIndex)
		{
			return dbType.toString();
		}

		@Override
		public List<Extension> getAdditionalExtensions()
		{
			if(EXTENSIONS.isEmpty())
			{
				try
				{
					EXTENSIONS.add(new DatabaseSetupExtension(dbType));
				}
				catch(Exception ex)
				{
					throw new PreconditionViolationException("Error creating configuration for db: " + dbType, ex);
				}
			}
			return EXTENSIONS;
		}

		private ConfigurationProvider getProvider()
		{
			ServiceLoader<ConfigurationProvider> loader = ServiceLoader.load(ConfigurationProvider.class);
			Iterator<ConfigurationProvider> configs = loader.iterator();

			ConfigurationProvider configProvider = null;
			while(configs.hasNext())
			{
				ConfigurationProvider configProviderTmp = configs.next();
				if (this.dbType.toString().equals(configProviderTmp.getImplementation()))
				{
					configProvider = configProviderTmp;
				}
			}
			if(configProvider == null)
			{
				throw new PreconditionViolationException("Invalid dbtype: " + dbType);
			}
			return configProvider;
		}
	}
}
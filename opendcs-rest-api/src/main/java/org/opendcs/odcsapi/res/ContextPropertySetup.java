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

package org.opendcs.odcsapi.res;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.opendcs.odcsapi.hydrojson.DbInterface;

@WebListener
public final class ContextPropertySetup implements ServletContextListener
{

	@Override
	public void contextInitialized(ServletContextEvent sce)
	{
		ServletContext servletContext = sce.getServletContext();
		//Move this information to the database. https://github.com/opendcs/rest_api/issues/191
		String officeId = servletContext.getInitParameter("opendcs.rest.api.cwms.office");
		if(officeId != null && !officeId.isEmpty())
		{
			DbInterface.decodesProperties.setProperty("CwmsOfficeId", officeId);
		}
		String databaseType = servletContext.getInitParameter("editDatabaseType");
		if(databaseType != null && !databaseType.isEmpty())
		{
			DbInterface.setDatabaseType(databaseType);
		}
		String authCheck = servletContext.getInitParameter("opendcs.rest.api.authorization.type");
		if(authCheck != null && !authCheck.isEmpty())
		{
			DbInterface.decodesProperties.setProperty("opendcs.rest.api.authorization.type", authCheck);
		}
		String expireDuration = servletContext.getInitParameter("opendcs.rest.api.authorization.expiration.duration");
		if(expireDuration != null && !expireDuration.isEmpty())
		{
			DbInterface.decodesProperties.setProperty("opendcs.rest.api.authorization.expiration.duration", expireDuration);
		}
		String openIdJwkSetUrl = servletContext.getInitParameter("opendcs.rest.api.authorization.jwt.jwkset.url");
		if(openIdJwkSetUrl != null && !openIdJwkSetUrl.isEmpty())
		{
			DbInterface.decodesProperties.setProperty("opendcs.rest.api.authorization.jwt.jwkset.url", openIdJwkSetUrl);
		}
		String openIdIssuerUrl = servletContext.getInitParameter("opendcs.rest.api.authorization.jwt.issuer.url");
		if(openIdIssuerUrl != null && !openIdIssuerUrl.isEmpty())
		{
			DbInterface.decodesProperties.setProperty("opendcs.rest.api.authorization.jwt.issuer.url", openIdIssuerUrl);
		}
	}
}

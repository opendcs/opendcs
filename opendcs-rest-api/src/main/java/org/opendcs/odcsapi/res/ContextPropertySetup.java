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
		initProp(servletContext, "opendcs.rest.api.cwms.office", "CwmsOfficeId", "OPENDCS_DB_OFFICE");
		initProp(servletContext, "opendcs.rest.api.authorization.type", "opendcs.rest.api.authorization.type", "OPENDCS_AUTHORIZATION_TYPE");
		initProp(servletContext, "opendcs.rest.api.authorization.expiration.duration", "opendcs.rest.api.authorization.expiration.duration", "OPENDCS_AUTHORIZATION_DURATION");
		initProp(servletContext, "opendcs.rest.api.authorization.jwt.jwkset.url", "opendcs.rest.api.authorization.jwt.jwkset.url", "OPENDCS_AUTHORIZATION_JWK_SET_URL");
		initProp(servletContext, "opendcs.rest.api.authorization.jwt.issuer.url", "opendcs.rest.api.authorization.jwt.issuer.url", "OPENDCS_AUTHORIZATION_JWK_ISSUER_URL");
	}

	private static void initProp(ServletContext servletContext, String sysParam, String decodesParam, String envParam)
	{
		String authCheck = servletContext.getInitParameter(sysParam);
		if(authCheck == null || authCheck.trim().isEmpty())
		{
			authCheck = System.getProperty(sysParam, System.getenv(envParam));
		}
		if(authCheck != null && !authCheck.isEmpty())
		{
			DbInterface.decodesProperties.setProperty(decodesParam, authCheck);
		}
	}
}

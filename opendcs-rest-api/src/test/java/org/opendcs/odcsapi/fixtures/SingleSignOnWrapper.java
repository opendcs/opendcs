/*
 *  Copyright 2024 OpenDCS Consortium and its Contributors
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

import java.io.IOException;
import java.security.Principal;
import javax.servlet.ServletException;

import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class SingleSignOnWrapper extends SingleSignOn{
    public void wrappedRegister(String ssoId, Principal principal, String authType,
    String username, String password) {
        this.register(ssoId, principal, authType, username, password);
    }

    @Override
    public void invoke(Request request, Response response)
        throws IOException, ServletException {
            super.invoke(request, response);
        }
}

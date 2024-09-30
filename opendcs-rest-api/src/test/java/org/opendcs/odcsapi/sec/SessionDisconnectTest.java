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

package org.opendcs.odcsapi.sec;

import java.util.Optional;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.appmon.ApiEventClient;
import org.opendcs.odcsapi.lrgsclient.ClientConnectionCache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
final class SessionDisconnectTest
{
	@Test
	void testSessionDisconnectClearsCache()
	{
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("test");
		ClientConnectionCache instance = ClientConnectionCache.getInstance();
		ApiEventClient mock = mock(ApiEventClient.class);
		when(mock.getAppId()).thenReturn(1L);
		instance.addApiEventClient(mock, session.getId());
		Optional<ApiEventClient> apiEventClient = instance.getApiEventClient(1L, session.getId());
		assertTrue(apiEventClient.isPresent(), "Session should be in cache");
		new SessionDisconnect().sessionDestroyed(new HttpSessionEvent(session));
		apiEventClient = instance.getApiEventClient(1L, session.getId());
		assertFalse(apiEventClient.isPresent(), "Session should be removed from cache");
	}
}

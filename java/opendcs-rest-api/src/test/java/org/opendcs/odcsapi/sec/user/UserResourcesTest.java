/*
 * Copyright 2025 OpenDCS Consortium and its Contributors
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.opendcs.odcsapi.sec.user;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.opendcs.database.model.UserBuilder;
import org.opendcs.odcsapi.beans.ApiPasswordChange;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.sec.OpenDcsPrincipal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class UserResourcesTest
{
	@Test
	void testUpdatePasswordThrowsWhenNoSessionEstablished()
	{
		UserResources resources = new UserResources();
		HttpServletRequest httpRequest = mock(HttpServletRequest.class);
		when(httpRequest.getSession(false)).thenReturn(null);
		resources.httpRequest = httpRequest;

		ApiPasswordChange passwordChange = new ApiPasswordChange("old-password", "new-password");

		WebAppException ex = assertThrows(WebAppException.class, () -> resources.updatePassword(passwordChange));

		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getStatus());
	}

	@Test
	void testUpdatePasswordThrowsWhenSessionHasNoPrincipal()
	{
		UserResources resources = new UserResources();
		HttpSession session = mock(HttpSession.class);
		when(session.getAttribute(OpenDcsPrincipal.USER_PRINCIPAL_SESSION_ATTRIBUTE)).thenReturn(null);
		HttpServletRequest httpRequest = mock(HttpServletRequest.class);
		when(httpRequest.getSession(false)).thenReturn(session);
		resources.httpRequest = httpRequest;

		ApiPasswordChange passwordChange = new ApiPasswordChange("old-password", "new-password");

		WebAppException ex = assertThrows(WebAppException.class, () -> resources.updatePassword(passwordChange));

		// A live session with no principal attribute means the session was never authenticated,
		// which is a 401 rather than the 403 an authenticated-but-unauthorized caller gets.
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), ex.getStatus());
	}

	@Test
	void testGetPreferencesThrowsWhenSessionHasNoPrincipal()
	{
		UserResources resources = new UserResources();
		HttpSession session = mock(HttpSession.class);
		when(session.getAttribute(OpenDcsPrincipal.USER_PRINCIPAL_SESSION_ATTRIBUTE)).thenReturn(null);
		HttpServletRequest httpRequest = mock(HttpServletRequest.class);
		when(httpRequest.getSession(false)).thenReturn(session);
		resources.httpRequest = httpRequest;

		WebAppException ex = assertThrows(WebAppException.class, resources::getPreferences);

		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), ex.getStatus());
	}

	@Test
	void testUpdatePreferencesThrowsWhenNoSessionEstablished()
	{
		UserResources resources = new UserResources();
		HttpServletRequest httpRequest = mock(HttpServletRequest.class);
		when(httpRequest.getSession(false)).thenReturn(null);
		resources.httpRequest = httpRequest;

		WebAppException ex = assertThrows(WebAppException.class,
				() -> resources.updatePreferences(Map.of("colorMode", "dark")));

		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getStatus());
	}

	@Test
	void testUpdatePreferencesRejectsMissingBody()
	{
		UserResources resources = new UserResources();
		HttpSession session = mock(HttpSession.class);
		when(session.getAttribute(OpenDcsPrincipal.USER_PRINCIPAL_SESSION_ATTRIBUTE))
				.thenReturn(new OpenDcsPrincipal(new UserBuilder().withEmail("user").build(), Map.of()));
		HttpServletRequest httpRequest = mock(HttpServletRequest.class);
		when(httpRequest.getSession(false)).thenReturn(session);
		resources.httpRequest = httpRequest;

		WebAppException ex = assertThrows(WebAppException.class, () -> resources.updatePreferences(null));

		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getStatus());
	}

	@Test
	void testMergePreferencesAddsReplacesAndRemoves() throws Exception
	{
		Map<String, Object> current = Map.of(
				"preferredUserName", "bob",
				"colorMode", "light",
				"organization", "SPK");
		Map<String, Object> changes = new HashMap<>();
		changes.put("colorMode", "dark");
		changes.put("organization", null);
		changes.put("language", "de-DE");

		Map<String, Object> merged = UserResources.mergePreferences(current, changes);

		assertEquals("bob", merged.get("preferredUserName"), "keys absent from the change must be kept");
		assertEquals("dark", merged.get("colorMode"));
		assertEquals("de-DE", merged.get("language"));
		assertFalse(merged.containsKey("organization"), "a null value must remove the key");
		assertEquals("SPK", current.get("organization"), "the stored map must not be modified");
	}

	@Test
	void testMergePreferencesRejectsBlankName()
	{
		WebAppException ex = assertThrows(WebAppException.class,
				() -> UserResources.mergePreferences(Map.of(), Map.of(" ", "value")));

		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getStatus());
	}

	@Test
	void testMergePreferencesRejectsResultTooLargeToStore()
	{
		// {"a":"<value>"} is 8 bytes plus the value, so this lands exactly one byte over the limit.
		String value = "x".repeat(UserResources.MAX_PREFERENCES_BYTES - 7);

		WebAppException ex = assertThrows(WebAppException.class,
				() -> UserResources.mergePreferences(Map.of(), Map.of("a", value)));

		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getStatus());
	}

	@Test
	void testMergePreferencesAcceptsResultAtTheLimit() throws Exception
	{
		String value = "x".repeat(UserResources.MAX_PREFERENCES_BYTES - 8);

		Map<String, Object> merged = UserResources.mergePreferences(Map.of(), Map.of("a", value));

		assertEquals(value, merged.get("a"));
	}
}

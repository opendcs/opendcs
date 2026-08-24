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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.opendcs.authentication.OpenDcsAuthException;
import org.opendcs.authentication.identityprovider.impl.builtin.BuiltInIdentityProvider;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.IdentityProviderDao;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.User;
import org.opendcs.odcsapi.beans.ApiPasswordChange;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.sec.OpenDcsPrincipal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
	void testFindCredentialUpdatingProviderReturnsFirstBuiltInProvider() throws Exception
	{
		UserResources resources = new UserResources();
		DataTransaction tx = mock(DataTransaction.class);
		// A provider that owns credentials but isn't the built-in one, and a built-in one that
		// doesn't own them, are both skipped in favor of the built-in provider that can update.
		IdentityProvider external = mock(IdentityProvider.class);
		when(external.canUpdateCredentials()).thenReturn(true);
		BuiltInIdentityProvider readOnlyBuiltIn = mock(BuiltInIdentityProvider.class);
		when(readOnlyBuiltIn.canUpdateCredentials()).thenReturn(false);
		BuiltInIdentityProvider expected = mock(BuiltInIdentityProvider.class);
		when(expected.canUpdateCredentials()).thenReturn(true);
		IdentityProviderDao idpDao = mock(IdentityProviderDao.class);
		when(idpDao.getIdentityProvidersForSubject(tx, "user"))
				.thenReturn(List.of(external, readOnlyBuiltIn, expected));

		assertSame(expected, resources.findCredentialUpdatingProvider(idpDao, tx, "user"));
	}

	@Test
	void testFindCredentialUpdatingProviderThrowsForbiddenWhenNoProviderOwnsCredentials() throws Exception
	{
		UserResources resources = new UserResources();
		DataTransaction tx = mock(DataTransaction.class);
		IdentityProviderDao idpDao = mock(IdentityProviderDao.class);
		when(idpDao.getIdentityProvidersForSubject(tx, "external-user")).thenReturn(Collections.emptyList());

		WebAppException ex = assertThrows(WebAppException.class,
				() -> resources.findCredentialUpdatingProvider(idpDao, tx, "external-user"));

		// Externally authenticated users have no password we own, so this is a 403, not a 404.
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getStatus());
	}

	@Test
	void testApplyPasswordChangeCommitsWhenCurrentPasswordMatches() throws Exception
	{
		UserResources resources = new UserResources();
		OpenDcsDatabase db = mock(OpenDcsDatabase.class);
		DataTransaction tx = mock(DataTransaction.class);
		User user = mock(User.class);
		BuiltInIdentityProvider idp = mock(BuiltInIdentityProvider.class);
		when(idp.canUpdateCredentials()).thenReturn(true);
		when(idp.login(any(), any(), any())).thenReturn(Optional.of(user));
		IdentityProviderDao idpDao = mock(IdentityProviderDao.class);
		when(idpDao.getIdentityProvidersForSubject(tx, "user")).thenReturn(List.of(idp));
		OpenDcsPrincipal principal = mock(OpenDcsPrincipal.class);
		when(principal.getName()).thenReturn("user");
		when(principal.getUser()).thenReturn(user);

		resources.applyPasswordChange(db, idpDao, tx, principal, new ApiPasswordChange("old", "new"));

		verify(idp).updateUserCredentials(any(), any(), any(), any());
		verify(tx).commit();
	}

	@Test
	void testApplyPasswordChangeThrowsForbiddenWhenCurrentPasswordIsWrong() throws Exception
	{
		UserResources resources = new UserResources();
		OpenDcsDatabase db = mock(OpenDcsDatabase.class);
		DataTransaction tx = mock(DataTransaction.class);
		BuiltInIdentityProvider idp = mock(BuiltInIdentityProvider.class);
		when(idp.canUpdateCredentials()).thenReturn(true);
		when(idp.login(any(), any(), any())).thenReturn(Optional.empty());
		IdentityProviderDao idpDao = mock(IdentityProviderDao.class);
		when(idpDao.getIdentityProvidersForSubject(tx, "user")).thenReturn(List.of(idp));
		OpenDcsPrincipal principal = mock(OpenDcsPrincipal.class);
		when(principal.getName()).thenReturn("user");

		WebAppException ex = assertThrows(WebAppException.class, () -> resources.applyPasswordChange(db, idpDao, tx,
				principal, new ApiPasswordChange("wrong", "new")));

		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getStatus());
		verify(idp, never()).updateUserCredentials(any(), any(), any(), any());
		verify(tx, never()).commit();
	}

	@Test
	void testApplyPasswordChangeRollsBackWhenLookupFails() throws Exception
	{
		UserResources resources = new UserResources();
		OpenDcsDatabase db = mock(OpenDcsDatabase.class);
		DataTransaction tx = mock(DataTransaction.class);
		IdentityProviderDao idpDao = mock(IdentityProviderDao.class);
		when(idpDao.getIdentityProvidersForSubject(tx, "user"))
				.thenThrow(new OpenDcsDataException("provider lookup failed"));
		OpenDcsPrincipal principal = mock(OpenDcsPrincipal.class);
		when(principal.getName()).thenReturn("user");

		WebAppException ex = assertThrows(WebAppException.class, () -> resources.applyPasswordChange(db, idpDao, tx,
				principal, new ApiPasswordChange("old", "new")));

		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getStatus());
		// The transaction commits on close, so a failure has to roll it back explicitly.
		verify(tx).rollback();
		verify(tx, never()).commit();
	}

	@Test
	void testApplyPasswordChangeRollsBackWhenCredentialUpdateFails() throws Exception
	{
		UserResources resources = new UserResources();
		OpenDcsDatabase db = mock(OpenDcsDatabase.class);
		DataTransaction tx = mock(DataTransaction.class);
		User user = mock(User.class);
		BuiltInIdentityProvider idp = mock(BuiltInIdentityProvider.class);
		when(idp.canUpdateCredentials()).thenReturn(true);
		when(idp.login(any(), any(), any())).thenReturn(Optional.of(user));
		doThrowAuthException(idp);
		IdentityProviderDao idpDao = mock(IdentityProviderDao.class);
		when(idpDao.getIdentityProvidersForSubject(tx, "user")).thenReturn(List.of(idp));
		OpenDcsPrincipal principal = mock(OpenDcsPrincipal.class);
		when(principal.getName()).thenReturn("user");
		when(principal.getUser()).thenReturn(user);

		WebAppException ex = assertThrows(WebAppException.class, () -> resources.applyPasswordChange(db, idpDao, tx,
				principal, new ApiPasswordChange("old", "new")));

		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getStatus());
		verify(tx).rollback();
		verify(tx, never()).commit();
	}

	private static void doThrowAuthException(BuiltInIdentityProvider idp) throws OpenDcsAuthException
	{
		org.mockito.Mockito.doThrow(new OpenDcsAuthException("write failed"))
				.when(idp).updateUserCredentials(any(), any(), any(), any());
	}
}

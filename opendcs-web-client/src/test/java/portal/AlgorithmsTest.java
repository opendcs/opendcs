/*
 *  Copyright 2023 OpenDCS Consortium
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

package portal;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portal.computations.Algorithms;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlgorithmsTest {

	@Mock
	ServletContext servletContext;
	@Mock
	ServletConfig servletConfig;

	@Mock
	HttpServletRequest request;

	@Mock
	HttpServletResponse response;

	@Mock
	RequestDispatcher requestDispatcher;

	@Test
	void doGet() throws Exception {
		when(servletConfig.getServletContext())
				.thenReturn(servletContext);
		when(servletContext.getRequestDispatcher("/algorithms.jsp"))
				.thenReturn(requestDispatcher);
		when(request.getRequestDispatcher("/algorithms.jsp"))
				.thenReturn(requestDispatcher);
		Algorithms algorithmsServlet = new Algorithms();
		algorithmsServlet.init(servletConfig);
		algorithmsServlet.doGet(request, response);
		verify(request.getRequestDispatcher("/algorithms.jsp"))
				.forward(request, response);
	}
}

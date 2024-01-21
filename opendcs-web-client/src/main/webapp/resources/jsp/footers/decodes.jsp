<%--
  ~  Copyright 2023 OpenDCS Consortium
  ~
  ~  Licensed under the Apache License, Version 2.0 (the "License");
  ~  you may not use this file except in compliance with the License.
  ~  You may obtain a copy of the License at
  ~       http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~  Unless required by applicable law or agreed to in writing, software
  ~  distributed under the License is distributed on an "AS IS" BASIS,
  ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~  See the License for the specific language governing permissions and
  ~  limitations under the License.
  --%>

<%-- Footer --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<div class="navbar navbar-expand-lg navbar-light">
	<div class="text-center d-lg-none w-100">
		<button type="button" class="navbar-toggler dropdown-toggle"
			data-toggle="collapse" data-target="#navbar-footer">
			<i class="icon-unfold mr-2"></i> Footer
		</button>
	</div>

	<div class="navbar-collapse collapse" id="navbar-footer">
		<span class="navbar-text"><a href="#">OpenDCS Web</a> </span>

		<ul class="navbar-nav ml-lg-auto">

			<li class="nav-item"><a
				href="<% String swaggerUrl =  (String) request.getAttribute("api_swaggerui_url"); %><%= swaggerUrl %>"
				class="nav-link" target="_blank" rel="noopener"> <i class="icon-code"></i> <span>
						API Swagger UI </span>
			</a>
		</ul>
	</div>
</div>
<%-- /footer --%>
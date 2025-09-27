<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
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

<%-- Core JS files --%>
<script src="/webjars/jquery/jquery.min.js"></script>
<script src="/webjars/bootstrap/js/bootstrap.bundle.min.js"></script>
<script src="/webjars/jquery-blockui/jquery.blockUI.js"></script>
<%-- /core JS files --%>

<%-- Theme JS files --%>
<script src="/webjars/d3js/d3.min.js"></script>
<%-- /theme JS files --%>


<%-- Datatables files --%>
<script src="/webjars/datatables/js/dataTables.min.js"></script>
<script src="/webjars/datatables-responsive/js/dataTables.responsive.min.js"></script>
<script src="/webjars/select2/4.0.13/js/select2.min.js"></script>
<script src="/webjars/datatables-select/2.1.0/js/dataTables.select.min.js"></script>
<script src="/webjars/datatables-buttons/3.2.5/js/dataTables.buttons.min.js"></script>
<%-- /datatables files --%>

<%--  OpenDCS Web Files --%>
<script src="../resources/js/decodes.js"></script>
<script src="../resources/js/lib/dom_utilities.js"></script>
<script src="../resources/js/lib/opendcs_utilities.js"></script>
<%-- /opendcs web files --%>

<%-- Get API Path --%>
<script>
	var origin = window.location.origin;
	var apiBasePath = `<%= getServletConfig().getServletContext().getInitParameter("api_base_path") %>`;
	window.API_URL = new URL(apiBasePath, origin).href;
</script>
<%-- /get api path --%>

<jsp:include page="/resources/jsp/modals/waiting.jsp" />
<jsp:include page="/resources/jsp/modals/notification.jsp" />
<jsp:include page="/resources/jsp/modals/yesno.jsp" />
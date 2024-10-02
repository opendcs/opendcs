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

<%-- Global stylesheets --%>
<link href="../resources/css/icons/icomoon/styles.min.css"
	rel="stylesheet" type="text/css">
<link href="../resources/css/limitless/bootstrap.min.css"
	rel="stylesheet" type="text/css">
<link href="../resources/css/limitless/bootstrap_limitless.min.css"
	rel="stylesheet" type="text/css">
<link href="../resources/css/limitless/layout.min.css" rel="stylesheet"
	type="text/css">
<link href="../resources/css/limitless/components.min.css"
	rel="stylesheet" type="text/css">
<link href="../resources/css/limitless/colors.min.css" rel="stylesheet"
	type="text/css">
<link href="../resources/css/limitless/colors.min.css" rel="stylesheet"
	type="text/css">
<link href="../resources/css/select.dataTables.css" rel="stylesheet"
	type="text/css">
<link href="../resources/css/main.css" rel="stylesheet" type="text/css">

<%-- /global stylesheets --%>

<%-- Core JS files --%>
<script src="../resources/js/jquery-3.4.1.js"></script>
<script src="../resources/js/bootstrap.bundle.min.js"></script>
<script src="../resources/js/plugins/loaders/blockui.min.js"></script>
<%-- /core JS files --%>

<%-- Theme JS files --%>
<script src="../resources/js/plugins/visualization/d3/d3.min.js"></script>
<%-- /theme JS files --%>


<%-- Datatables files --%>
<script
	src="../resources/js/plugins/tables/datatables/datatables.min.js"></script>
<script
	src="../resources/js/plugins/tables/datatables/extensions/responsive.min.js"></script>
<script src="../resources/js/plugins/forms/selects/select2.min.js"></script>
<script src="../resources/js/dataTables.select.js"></script>
<script src="../resources/js/dataTables.buttons.min.js"></script>
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
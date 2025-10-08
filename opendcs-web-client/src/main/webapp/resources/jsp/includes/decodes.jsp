<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%-- Core JS files --%>
<script src="<c:url value='/webjars/jquery/3.7.1/jquery.min.js'/>"></script>
<script src="<c:url value='/webjars/bootstrap/5.3.8/js/bootstrap.bundle.min.js'/>"></script>
<script src="<c:url value='/webjars/select2/4.0.13/js/select2.min.js'/>"></script>

<!-- jQuery plugins / utilities -->
<script src="<c:url value='/webjars/jquery-blockui/2.70/jquery.blockUI.js'/>"></script>
<script src="<c:url value='/webjars/jquery-validation/1.20.0/jquery.validate.min.js'/>"></script>
<script src="<c:url value='/webjars/d3js/6.7.0/d3.min.js'/>"></script>

<!-- Date handling -->
<script src="<c:url value='/webjars/moment/2.30.1/min/moment.min.js'/>"></script>
<script src="<c:url value='/webjars/bootstrap-daterangepicker/3.1.0/daterangepicker.js'/>"></script>

<%-- Datatables files --%>
<script src="<c:url value='/webjars/datatables.net/1.13.8/js/jquery.dataTables.min.js'/>"></script>
<script src="<c:url value='/webjars/datatables.net-bs5/1.13.8/js/dataTables.bootstrap5.min.js'/>"></script>

<!-- DataTables extensions -->
<script src="<c:url value='/webjars/datatables.net-responsive/2.5.0/js/dataTables.responsive.min.js'/>"></script>
<script src="<c:url value='/webjars/datatables.net-responsive-bs5/2.5.0/js/responsive.bootstrap5.min.js'/>"></script>
<script src="<c:url value='/webjars/datatables.net-buttons/2.4.2/js/dataTables.buttons.min.js'/>"></script>
<script src="<c:url value='/webjars/datatables.net-buttons-bs5/2.4.2/js/buttons.bootstrap5.min.js'/>"></script>
<script src="<c:url value='/webjars/datatables.net-select/1.7.1/js/dataTables.select.min.js'/>"></script>
<script src="<c:url value='/webjars/datatables.net-select-bs5/1.4.0/js/select.bootstrap5.min.js'/>"></script>

<!-- Web Client files -->
<script src="<c:url value='/resources/js/decodes.js'/>"></script>
<script src="<c:url value='/resources/js/lib/dom_utilities.js'/>"></script>
<script src="<c:url value='/resources/js/lib/opendcs_utilities.js'/>"></script>

<!-- Modals -->
<jsp:include page="/resources/jsp/modals/waiting.jsp" />
<jsp:include page="/resources/jsp/modals/notification.jsp" />
<jsp:include page="/resources/jsp/modals/yesno.jsp" />

<!-- API base -->
<script>
	(function () {
		var origin = window.location.origin;
		var apiBasePath = "<%= getServletConfig().getServletContext().getInitParameter("api_base_path") %>";
		window.API_URL = new URL(apiBasePath, origin).href;
	})();
</script>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<%@include file="/WEB-INF/common/header.jspf" %>

<body class="navbar-top">
	<%@include file="/WEB-INF/common/top-bar.jspf" %>

	<!-- Page content -->
	<div class="page-content">

		<%@include file="/WEB-INF/common/sidebar.jspf" %>

		<!-- Main content -->
		<div class="content-wrapper">
			<!-- Page header -->
			<div class="page-header page-header-light">
				<div class="page-header-content header-elements-md-inline">
					<div class="page-title d-flex">
						<h4>
							<span class="font-weight-semibold">OpenDCS</span> - Sites
						</h4>
						<a href="#" class="header-elements-toggle text-default d-md-none"><i
							class="bi bi-three-dots-vertical"></i></a>
					</div>
				</div>
			</div>
			<!-- /page header -->

			<!-- Content area -->
			<div class="content">
				<!-- Basic responsive configuration -->
				<div class="card large-padding h-100">
					<div class="card-header header-elements-inline"></div>
					<table id="sitesTable"
						class="table table-hover datatable-responsive tablerow-cursor w-100">
						<thead>
							<tr>
								<th>Site Id</th>
								<th id="siteNameColumnHeader">Site Name (N/A)</th>
								<th>Configured Site Names</th>
								<th>Description</th>
								<th>Actions</th>
							</tr>
						</thead>
						<tbody id="tableBody">
							<!--This is where the data rows go-->
						</tbody>
					</table>
				</div>
				<!-- /basic responsive configuration -->
			</div>
			<!-- /Content area -->

			<%@include file="/WEB-INF/common/footer.jspf" %>


		</div>
		<!-- /main content -->
	</div>
	<!-- /page content -->
	<%@include file="/WEB-INF/common/scripts.jspf" %>

	<script src="/webjars/jquery-validation/jquery.validate.min.js"></script>
	<script src="../resources/js/lib/time.js"></script>
	<script src="../resources/js/datatables/helpers.js"></script>
	<script src="/webjars/datatables/js/dataTables.min.js"></script>
	<script src="../resources/js/lib/dom_utilities.js"></script>
	<script src="../resources/js/sites.js"></script>
	<script src="../resources/js/lib/object_utilities.js"></script>
	<script src="../resources/js/lib/list_utilities.js"></script>

	<%@include file="/WEB-INF/data_modals/site.jspf" %>
</body>
</html>
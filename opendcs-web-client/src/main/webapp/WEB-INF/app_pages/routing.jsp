<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
	<%@include file="/WEB-INF/common/header.jspf" %>
	<body >
		<!-- Main navbar -->
		<%@include file="/WEB-INF/common/top-bar.jspf" %>

	    <!-- Page content -->
	    <div class="page-content d-flex">

	        <%@include file="/WEB-INF/common/sidebar.jspf" %>

			<!-- Main content -->
			<div class="container-fluid flex-grow-1">
				<!-- Page header -->
				<div class="page-header page-header-light">
					<div class="page-header-content header-elements-md-inline">
						<div class="page-title d-flex">
							<h4><span class="font-weight-semibold">OpenDCS</span> - Routing</h4>
							<a href="#" class="header-elements-toggle text-default d-md-none"><i class="bi bi-three-dots-vertical"></i></a>
						</div>
					</div>
				</div> <!-- /page header -->

				<!-- Content area -->
				<div class="content">
					<!-- Basic responsive configuration -->
					<div class="card large-padding border-primary">
						<div class="card-header header-elements-inline">
						</div>
						<table id="routingTable" class="table table-hover table-striped datatable-responsive tablerow-cursor w-100">
							<thead>
								<tr>
									<th>Id</th>
									<th>Name</th>
									<th>Data Source</th>
									<th>Consumer</th>
									<th>Last Modified</th>
									<th>Actions</th>
								</tr>
							</thead>
							<tbody id="tableBody">
								<!--This is where the data rows go-->
							</tbody>
						</table>
					</div> <!-- /basic responsive configuration -->
				</div> <!-- /Content area -->


				


			</div> <!-- /main content -->
		</div> <!-- /page content -->
		<%@include file="/WEB-INF/common/footer.jspf" %>
		<%@include file="/WEB-INF/common/scripts.jspf" %>
		<script src="../resources/js/lib/time.js"></script>
		<script src="../resources/js/lib/date_utilities.js"></script>
		<script src="../resources/js/routing.js"></script>
		<%@include file="/WEB-INF/data_modals/routing.jspf" %>
		<%@include file="/WEB-INF/data_modals/platform_select.jspf" %>
	</body>
</html>
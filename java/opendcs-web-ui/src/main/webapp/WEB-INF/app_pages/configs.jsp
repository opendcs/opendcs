<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
	<%@include file="/WEB-INF/common/header.jspf" %>

	<body >
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
							<h4>Configs</h4>
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
						<table id="configsTable" class="table table-hover table-striped datatable-responsive tablerow-cursor w-100">
							<thead>
								<tr>
									<th>Config ID</th>
									<th>Name</th>
									<th>Equipment Id</th>
									<th># Platforms</th>
									<th>Description</th>
									<th class="text-center">Actions</th>
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

		<script src="../resources/js/plugins/pickers/anytime.min.js"></script>
		<script src="/webjars/pickadate.js/picker.js"></script>
		<script src="/webjars/pickadate.js/picker.date.js"></script>
		<script src="/webjars/pickadate.js/picker.time.js"></script>
		<script src="/webjars/pickadate.js/legacy.js"></script>
		<script src="../resources/js/configs.js"></script>
		
		<script src="../resources/js/lib/date_utilities.js"></script>

		<%@include file="/WEB-INF/data_modals/config.jspf" %>
		<%@include file="/WEB-INF/data_modals/config_sensor.jspf" %>
		<%@include file="/WEB-INF/data_modals/decodes_script.jspf" %>
	</body>
</html>
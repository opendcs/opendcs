<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
	<jsp:include page="/resources/jsp/headers/decodes.jsp" />

	<body class="navbar-top">
		<jsp:include page="/resources/jsp/menus/decodes/main.jsp" />   

	    <!-- Page content -->
	    <div class="page-content">
	
	        <jsp:include page="/resources/jsp/menus/decodes/sidebar.jsp" /> 
			
			<!-- Main content -->
			<div class="content-wrapper">
				<!-- Page header -->
				<div class="page-header page-header-light">
					<div class="page-header-content header-elements-md-inline">
						<div class="page-title d-flex">
							<h4><span class="font-weight-semibold">OpenDCS</span> - Engineering Units</h4>
							<a href="#" class="header-elements-toggle text-default d-md-none"><i class="icon-more"></i></a>
						</div>
					</div>  
				</div> <!-- /page header -->
			
				<!-- Content area -->
				<div class="content">
					<!-- Basic responsive configuration -->
					<div class="card large-padding h-100">
						<div class="form-group row mb-2">
		                    <div class="col-lg-12">
		                    	<button type="button" class="btn btn-success float-right" id="saveButton">Save</button>
		                    </div>
						</div>
						<table id="mainTable" class="table table-hover datatable-responsive w-100">
							<caption class="captionTitleCenter">Engineering Units
								<button class="btn btn-secondary float-right captionButton mr-3" id="addButton">+</button>
							</caption>
							<thead>
								<tr>
									<th>Original Abbrev</th>
									<th>Abbrev</th>
									<th>Full Name</th>
									<th>Family</th>
									<th>Measures</th>
									<th>Actions</th>
								</tr>
							</thead>
						</table>
					</div> <!-- /basic responsive configuration -->
				</div> <!-- /Content area -->


				<jsp:include page="/resources/jsp/footers/decodes.jsp" /> 
				
			</div> <!-- /main content -->
		</div> <!-- /page content -->
	</body>
	
	<jsp:include page="/resources/jsp/includes/decodes.jsp" /> 
	
<script src="../resources/js/plugins/forms/styling/switchery.min.js"></script>
<script src="../resources/js/plugins/forms/styling/switch.min.js"></script>
<script src="../resources/js/plugins/forms/styling/uniform.min.js"></script>
	<script src="../resources/js/datatables/helpers.js"></script>
	<script src="../resources/js/lib/dom_utilities.js"></script>
	<script src="../resources/js/lib/object_utilities.js"></script>
	<script src="../resources/js/lib/date_utilities.js"></script>
	<script src="../resources/js/engineering_units.js"></script>
</html>
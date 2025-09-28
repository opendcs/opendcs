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
							<h4><span class="font-weight-semibold">OpenDCS</span> - Engineering Unit Conversions</h4>
							<a href="#" class="header-elements-toggle text-default d-md-none"><i class="bi bi-three-dots-vertical"></i></a>
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
							<caption class="captionTitleCenter">Engineering Unit Conversions
								<button class="btn btn-secondary float-right captionButton mr-3" id="addButton">+</button>
							</caption>
							<thead>
								<tr>
									<th>Id</th>
									<th>From</th>
									<th>To</th>
									<th>Algorithm</th>
									<th>A</th>
									<th>B</th>
									<th>C</th>
									<th>D</th>
									<th>E</th>
									<th>F</th>
									<th>Actions</th>
								</tr>
							</thead>
						</table>
					</div> <!-- /basic responsive configuration -->
				</div> <!-- /Content area -->


				<%@include file="/WEB-INF/common/footer.jspf" %>
				
			</div> <!-- /main content -->
		</div> <!-- /page content -->
		<%@include file="/WEB-INF/common/scripts.jspf" %>
	
		<script src="/webjars/switchery/switchery.js"></script>
		<script src="/webjars/bootstrap-switch/js/bootstrap-switch.min.js"></script>
		<script src="/webjars/uniform/jquery.uniform.min.js"></script>
		<script src="../resources/js/datatables/helpers.js"></script>
		<script src="../resources/js/lib/dom_utilities.js"></script>
		<script src="../resources/js/lib/object_utilities.js"></script>
		<script src="../resources/js/lib/opendcs_utilities.js"></script>
		<script src="../resources/js/lib/date_utilities.js"></script>
		<script src="../resources/js/eu_conversions.js"></script>
	</body>
	
<<<<<<< HEAD
	<jsp:include page="/resources/jsp/includes/decodes.jsp" />


<script src="../resources/js/datatables/helpers.js"></script>
	<script src="../resources/js/lib/dom_utilities.js"></script>
	<script src="../resources/js/lib/object_utilities.js"></script>
	<script src="../resources/js/lib/opendcs_utilities.js"></script>
	<script src="../resources/js/lib/date_utilities.js"></script>
	<script src="../resources/js/eu_conversions.js"></script>
=======

>>>>>>> a7f2f28 (Rearrange header/foot/etc to standardized names and directory.)
</html>
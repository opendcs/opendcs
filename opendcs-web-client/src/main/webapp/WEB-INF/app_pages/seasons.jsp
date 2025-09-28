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
							<h4><span class="font-weight-semibold">OpenDCS</span> - Seasons</h4>
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
							<caption class="captionTitleCenter">Seasons
								<button class="btn btn-secondary float-right captionButton mr-3" id="addButton">+</button>
							</caption>
							<thead>
								<tr>
									<th></th>
									<th>Original Abbreviation</th>
									<th>Abbreviation</th>
									<th>Descriptive Name</th>
									<th>Start</th>
									<th>End</th>
									<th>TZ</th>
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
		<script src="../resources/js/lib/date_utilities.js"></script>
		<script src="../resources/js/lib/time.js"></script>
		<script src="../resources/js/seasons.js"></script>
	</body>
</html>
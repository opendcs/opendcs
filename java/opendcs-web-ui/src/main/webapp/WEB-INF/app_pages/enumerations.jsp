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
							<h4>Enumerations</h4>
							<a href="#" class="header-elements-toggle text-default d-md-none"><i class="bi bi-three-dots-vertical"></i></a>
						</div>
					</div>
				</div> <!-- /page header -->

				<!-- Content area -->
				<div class="content">
					<!-- Basic responsive configuration -->
					<div class="card large-padding border-primary">
						<div class="form-group row mb-2">
							<label class="col-form-label col-lg-5 text-right">Enumeration</label>
		                    <div class="col-lg-3">
		                      <select id="enumerationSelectbox" class="selectpicker form-control">
		                        </select>
		                    </div>
		                    <div class="col-lg-4">
		                    	<button type="button" class="btn btn-success float-right" id="saveButton">Save</button>
		                    </div>
						</div>
						<table id="enumerationTable" class="table table-hover table-striped datatable-responsive w-100">
							<caption class="captionTitleCenter">Enumerations
								<button class="btn btn-secondary float-right captionButton" id="addEnumerationButton">+</button>
							</caption>
							<thead>
								<tr>
									<th></th>
									<th>Default</th>
									<th>Name</th>
									<th>Description</th>
									<th>Java Class (optional)</th>
									<th>Options</th>
									<th>Actions</th>
								</tr>
							</thead>
						</table>
					</div> <!-- /basic responsive configuration -->
				</div> <!-- /Content area -->
			</div> <!-- /main content -->
		</div> <!-- /page content -->
		<%@include file="/WEB-INF/common/footer.jspf" %>
		<%@include file="/WEB-INF/common/scripts.jspf" %>
		<script src="../resources/js/lib/date_utilities.js"></script>
		<script src="../resources/js/enumerations.js"></script>
	</body>
</html>
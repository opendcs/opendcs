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
							<h4><span class="font-weight-semibold">OpenDCS</span> - Processes</h4>
							<a href="#" class="header-elements-toggle text-default d-md-none"><i class="icon-more"></i></a>
						</div>
					</div>  
				</div> <!-- /page header -->
				<!-- Content area -->
				<div class="content">
					<!-- Basic responsive configuration -->
					<div class="card large-padding h-100">
						<table id="mainTable" class="table table-hover datatable-responsive w-100 tablerow-cursor">
							<caption class="captionTitleCenter">Computations
								<button class="btn btn-secondary float-right captionButton mr-3" id="addButton">+</button>
							</caption>
							<thead>
								<tr>
									<th>ID</th>
									<th>Name</th>
									<th>Num Comps</th>
									<th>Comment</th>
									<th>Actions</th>
								</tr>
							</thead>
						</table>
					</div> 
					<!-- /basic responsive configuration -->
				</div>
				<!-- /Content area -->
				<jsp:include page="/resources/jsp/footers/decodes.jsp" /> 
			</div>
			<!-- /main content -->
		</div>
		<!-- /page content -->
		<!-- Process modal -->
		<div id="modal_main" class="modal fade" tabindex="-1" data-keyboard="false" data-backdrop="static">
			<div class="modal-dialog modal-dialog-scrollable opendcs-modal-90" class_old="modal-dialog modal-xl modal-height-xl">
				<div class="modal-content h-100">
					<div class="modal-header bg-secondary">
						<h6 class="modal-title" id="modalTitle"><span id="modalMainTitle">Process</span><span id="modalSubTitle"></span></h6>
					</div>
					<div id="displayedId" class="displayNone"></div>
					<div class="modal-body grey-background total_height-header-footer">
						<div class="h-100 d-flex flex-column">
							<div class="row">
								<div class="col-lg-5">
									<div class="card pl-3 pr-3 pt-1 pb-1 opendcs_y_scrollable">
										<form class="form-validate-jquery" onkeydown="return event.key != 'Enter';">
											<table class="table table-hover datatable-responsive dataTable no-footer mb-1">
												<caption class="captionTitleCenter">Details</caption>
											</table>
											<div class="form-group row">
												<label class="col-form-label col-lg-2">Process Name</label>
												<div class="col-lg-5">
													<input id="processNameTextbox" type="text" class="form-control" required placeholder="Enter the name of the process">
												</div>
												<label class="col-form-label col-lg-2">Process Id</label>
												<div class="col-lg-3">
													<input id="processIdTextbox" type="text" class="form-control" required placeholder="N/A" disabled>
												</div>
											</div>
											<div class="form-group row">
												<label class="col-form-label col-lg-2">Process Type</label>
												<div class="col-lg-5">
													<select id="processTypeSelectbox" type="text" class="form-control"></select>
												</div>
												<label class="col-form-label col-lg-3 text-right">Manual Edit App</label>
												<div class="col-lg-2 float-left">
													<div class="form-check form-check-switchery"">
														<label class="form-check-label">
															<input type="checkbox" id="manualEditCheckbox" class="form-check-input-switchery" data-fouc>
														</label>
													</div>
												</div>
											</div>
										</form>
									</div>
								</div>
								<div class="card col-lg-7 pl-3 pr-3 pt-1 pb-1 w-100 opendcs_y_scrollable">
									<table class="table table-hover datatable-responsive dataTable no-footer mb-1">
										<caption class="captionTitleCenter">Comments</caption>
									</table>
								    <textarea class="form-control" id="commentsTextarea" rows="4"></textarea>
								</div>
							</div>
							<div class="row m-1 flex-grow-1">
								<div class="card col-lg-12 pl-3 pr-3 pt-1 pb-1 w-100">
									<table id="propertiesTable" resize_on_window_resize="97" class="table table-hover datatable-responsive co-8 border">
										<caption class="captionTitleCenter">Application Properties
											<button class="btn btn-secondary float-right captionButton mt-1" id="addPropertyButton">+</button>
										</caption>
										<thead>
											<tr>
												<th>Property Name</th>
												<th>Value</th>
												<th class="text-center">Actions</th>
											</tr>
										</thead>
										<tbody id="propertiesTableBody"></tbody>
									</table>
								</div>
							</div>
						</div>
					</div>
					<div class="modal-footer pt-3">
						<button type="button" class="btn btn-secondary" id="cancelButton" data-dismiss="modal">Cancel</button>
						<button type="button" class="btn btn-success" id="saveButton">Save</button>
					</div>
				</div>
			</div>
		</div>
	</div>
	<!-- /process modal -->
	</body>
	<jsp:include page="/resources/jsp/includes/decodes.jsp" /> 
	<script src="../resources/js/plugins/forms/styling/switchery.min.js"></script>
	<script src="../resources/js/plugins/forms/styling/switch.min.js"></script>
	<script src="../resources/js/plugins/forms/styling/uniform.min.js"></script>
	<script src="../resources/js/plugins/forms/selects/select2.min.js"></script>
	<script src="../resources/js/datatables/helpers.js"></script>
	<script src="../resources/js/lib/dom_utilities.js"></script>
	<script src="../resources/js/lib/object_utilities.js"></script>
	<script src="../resources/js/lib/date_utilities.js"></script>
	<script src="../resources/js/lib/list_utilities.js"></script>
	<script src="../resources/js/lib/web_utilities.js"></script>
	<script src="../resources/js/processes.js"></script>
</html>
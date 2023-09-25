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
							<h4><span class="font-weight-semibold">OpenDCS</span> - Computations</h4>
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
									<th>Algorithm</th>
									<th>Process</th>
									<th>Enabled</th>
									<th>Comments</th>
									<th>Actions</th>
								</tr>
							</thead>
						</table>
					</div> <!-- /basic responsive configuration -->
				</div> <!-- /Content area -->
				<jsp:include page="/resources/jsp/footers/decodes.jsp" /> 
			</div> <!-- /main content -->
		</div> <!-- /page content -->
		<!-- Computation modal -->
		<div id="modal_main" class="modal fade" tabindex="-1" data-keyboard="false" data-backdrop="static">
			<div class="modal-dialog modal-dialog-scrollable opendcs-modal-90" class_old="modal-dialog modal-xl modal-height-xl">
				<div class="modal-content h-100">
					<div class="modal-header bg-secondary">
						<h6 class="modal-title" id="modalTitle">Edit Computation</h6>
					</div>
					<div id="displayedId" class="displayNone"></div>
					<div class="modal-body grey-background total_height-header-footer">
						<div class="h-100 d-flex flex-column">
						<div class="row">
							<div class="col-lg-5">
									<div class="card pl-3 pr-3 pt-1 pb-1 opendcs_y_scrollable">
										<form class="form-validate-jquery" onkeydown="return event.key != 'Enter';">
											<table class="table table-hover datatable-responsive dataTable no-footer mb-1">
												<caption class="captionTitleCenter">Details
													<div class="float-right">
														<div class="form-check form-check-switchery"">
															<label class="form-check-label">
																<input type="checkbox" id="enabledCheckbox" class="form-check-input-switchery" data-fouc>
															</label>
														</div>
													</div>
												</caption>
											</table>
											<div class="form-group row">
												<label class="col-form-label col-lg-2">Comp Name</label>
												<div class="col-lg-4">
													<input id="compNameTextbox" type="text" class="form-control" required placeholder="Enter the name of the computation">
												</div>
												<label class="col-form-label col-lg-2">Comp Id</label>
												<div class="col-lg-4">
													<input id="compIdTextbox" type="text" class="form-control" required placeholder="This is a new computation." disabled>
												</div>
											</div>
											<div class="form-group row">
												<label class="col-form-label col-lg-2">Algorithm</label>
												<div class="col-lg-4">
													<button type="button" id="algorithmNameButton" class="form-control pointerCursor" value="" title="Click to select algorithm."></button>
												</div>
												<label class="col-form-label col-lg-2">Modified</label>
												<div class="col-lg-4">
													<input id="lastModifiedTextbox" type="text" class="form-control" placeholder="This is a new computation." disabled>
												</div>
											</div>
											<div class="form-group row">
												<label class="col-form-label col-lg-3">Process</label>
												<div class="col-lg-9">
													<select id="processSelectbox" type="text" class="form-control"></select>
												</div>
											</div>
											<div class="form-group row">
												<label class="col-form-label col-lg-3">Group</label>
												<div class="col-lg-9">
													<select id="groupSelectbox" type="text" class="form-control"></select>
													<!-- <input id="groupSelectbox" type="text" class="form-control" placeholder="Enter the group name.">-->
												</div>
											</div>
											<div class="form-group" id="toFromGroup">
												
												
											</div>
											<div class="form-group row">
												<label class="col-form-label col-lg-3">Comments</label>
												<div class="col-lg-9">
											    <textarea class="form-control" id="commentsTextarea" rows="4"></textarea>
												</div>
											</div>
										</form>
									</div>
								</div>
								<div class="card col-lg-7 pl-3 pr-3 pt-1 pb-1 w-100 opendcs_y_scrollable">
									<table id="propertiesTable" resize_on_window_resize="99" class="table table-hover datatable-responsive co-8 border">
										<caption class="captionTitleCenter">Computation Properties
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
							<div class="row m-1 flex-grow-1">
								<div class="card col-lg-12 pl-3 pr-3 pt-1 pb-1 w-100 opendcs_y_scrollable">
									<table id="parametersTable" resize_on_window_resize="99" class="table table-hover datatable-responsive co-8 border">
										<caption class="captionTitleCenter">Time Series Parameters</caption>
										<thead>
											<tr>
												<th>Role</th>
												<th>Location</th>
												<th>Parameter</th>
												<th>Param Type</th>
												<th>Interval</th>
												<th>Duration</th>
												<th>Version</th>
												<th>Delta-T</th>
												<th>Units</th>
												<th class="text-center">Actions</th>
											</tr>
										</thead>
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
	<!-- /computation modal -->
	<!-- Algorithm modal -->
	<div id="modal_algorithm" class="modal fade" tabindex="-1" data-keyboard="false" data-backdrop="static">
		<div class="modal-dialog modal-dialog-scrollable opendcs-modal-90" class_old="modal-dialog modal-xl modal-height-xl">
			<div class="modal-content h-100">
				<div class="modal-header bg-secondary">
					<h6 class="modal-title" id="modalTitle">Select Algorithm</h6>
				</div>
				<div class="modal-body grey-background total_height-header-footer">
					<div class="h-100 d-flex flex-column">
						<div class="row h-100">
							<div class="col-lg-12 h-100">
								<div class="card pl-3 pr-3 pt-1 pb-1 h-100 opendcs_y_scrollable">
									<table id="algorithmTable" resize_on_window_resize="97" class="table table-hover datatable-responsive co-8 border">
										<caption class="captionTitleCenter">Algorithms</caption>
										<thead>
											<tr>
												<th>ID</th>
												<th>Name</th>
												<th>Exec Class</th>
												<th># Comps</th>
												<th>Comment</th>
											</tr>
										</thead>
									</table>
								</div>
							</div>
						</div>
					</div>
				</div>
				<div class="modal-footer pt-3">
					<button type="button" class="btn btn-secondary" id="cancelAlgoButton" data-dismiss="modal">Cancel</button>
					<button type="button" class="btn btn-success" id="submitAlgoButton">Submit</button>
				</div>
			</div>
		</div>
	</div>
	<!-- /algorithm modal -->
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
	<script src="../resources/js/computations.js"></script>
</html>
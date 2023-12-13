<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%--
  ~  Copyright 2023 OpenDCS Consortium
  ~
  ~  Licensed under the Apache License, Version 2.0 (the "License");
  ~  you may not use this file except in compliance with the License.
  ~  You may obtain a copy of the License at
  ~       http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~  Unless required by applicable law or agreed to in writing, software
  ~  distributed under the License is distributed on an "AS IS" BASIS,
  ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~  See the License for the specific language governing permissions and
  ~  limitations under the License.
  --%>

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
							<h4><span class="font-weight-semibold">OpenDCS</span> - Algorithms</h4>
							<a href="#" class="header-elements-toggle text-default d-md-none"><i class="icon-more"></i></a>
						</div>
					</div>  
				</div> 
				<!-- /page header -->
				<!-- Content area -->
				<div class="content">
					<!-- Basic responsive configuration -->
					<div class="card large-padding h-100">
						<table id="mainTable" class="table table-hover datatable-responsive w-100 tablerow-cursor">
							<caption class="captionTitleCenter">Algorithms
								<button class="btn btn-secondary float-right captionButton mr-3" id="addButton">+</button>
							</caption>
							<thead>
								<tr>
									<th>ID</th>
									<th>Name</th>
									<th>Exec Class</th>
									<th>#Comps</th>
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
		<!-- algorithm modal -->
		<div id="modal_main" class="modal fade" tabindex="-1" data-keyboard="false" data-backdrop="static">
			<div class="modal-dialog modal-dialog-scrollable opendcs-modal-90" class_old="modal-dialog modal-xl modal-height-xl">
				<div class="modal-content h-100">
					<div class="modal-header bg-secondary">
						<h6 class="modal-title" id="modalTitle">Edit Algorithm</h6>
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
											<label class="col-form-label col-lg-3">Algorithm Name</label>
											<div class="col-lg-9">
												<input id="algorithmNameTextbox" type="text" class="form-control" required placeholder="Enter the name of the algorithm.">
											</div>
										</div>
										<div class="form-group row">
											<label class="col-form-label col-lg-3">Exec Class</label>
											<div class="col-lg-9">
												<input id="execClassTextbox" type="text" class="form-control" required placeholder="Enter the java class for the algorithm.">
											</div>
										</div>
										<div class="form-group row">
											<label class="col-form-label col-lg-3">Algorithm ID</label>
											<div class="col-lg-9">
												<input id="algorithmIdTextbox" type="text" class="form-control" required placeholder="No ID - New algorithm being created" disabled>
											</div>
										</div>
										<div class="form-group row">
											<label class="col-form-label col-lg-3">Num Comps</label>
											<div class="col-lg-9">
												<input id="numCompsTextbox" type="text" name="ownerAgencyTextbox" class="form-control" required placeholder="There was an issue getting number of running comps." disabled>
											</div>
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
								<div class="card col-lg-7 pl-3 pr-3 pt-1 pb-1 opendcs_y_scrollable">
										<table id="parametersTable" resize_on_window_resize="95" class="table table-hover datatable-responsive co-8 border">
											<caption class="captionTitleCenter">Parameters
												<button class="btn btn-secondary float-right captionButton mt-1" id="addParameterButton">+</button>
											</caption>
											<thead>
												<tr>
													<th>Role Name</th>
													<th>Type Code</th>
													<th class="text-center">Actions</th>
												</tr>
											</thead>
										</table>
									</div>
							</div>
							<div class="row m-1 flex-grow-1">
									<div class="card col-lg-12 pl-3 pr-3 pt-1 pb-1 w-100 opendcs_y_scrollable">
										<table id="propertiesTable" resize_on_window_resize="96" class="table table-hover datatable-responsive co-8 border">
											<caption class="captionTitleCenter">Properties
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
		<!-- /algorithm modal -->
	</body>
	<jsp:include page="/resources/jsp/includes/decodes.jsp" /> 
	<script src="../resources/js/plugins/forms/styling/switchery.min.js"></script>
	<script src="../resources/js/plugins/forms/styling/switch.min.js"></script>
	<script src="../resources/js/plugins/forms/styling/uniform.min.js"></script>
	<script src="../resources/js/datatables/helpers.js"></script>
	<script src="../resources/js/lib/dom_utilities.js"></script>
	<script src="../resources/js/lib/object_utilities.js"></script>
	<script src="../resources/js/lib/date_utilities.js"></script>
	<script src="../resources/js/algorithms.js"></script>
</html>
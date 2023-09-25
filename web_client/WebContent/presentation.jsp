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
							<h4>
								<span class="font-weight-semibold">OpenDCS</span> - Presentation
							</h4>
							<a href="#" class="header-elements-toggle text-default d-md-none">
								<i class="icon-more"></i>
							</a>
						</div>
					</div>
				</div>
				<!-- /page header -->
				<!-- Content area -->
				<div class="content">
					<!-- Basic responsive configuration -->
					<div class="card large-padding">
						<div class="card-header header-elements-inline"></div>
						<table id="presentationTable"
							class="table table-hover datatable-responsive tablerow-cursor w-100">
							<thead>
								<tr>
									<th>Group Id</th>
									<th>Name</th>
									<th>Inherits From</th>
									<th>Last Modified</th>
									<th>Is Production</th>
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
				<jsp:include page="/resources/jsp/footers/decodes.jsp" />
			</div>
			<!-- /main content -->
		</div>
		<!-- /page content -->
	</body>
	<!-- /theme JS files -->
	<!-- Presentation modal -->
	<div id="modal_presentation" class="modal fade" tabindex="-1"
		data-keyboard="false" data-backdrop="static">
		<div class="modal-dialog modal-dialog-scrollable opendcs-modal-90">
			<div class="modal-content">
				<div class="modal-header bg-secondary">
					<h6 class="modal-title" id="SourcesTitle">Presentation Element</h6>
				</div>
				<div id="displayedPresentationGroupId" class="displayNone"></div>
				<div class="modal-body">
					<!--<h6 class="font-weight-semibold">Network List Name</h6>-->
					<div class="form-group row smallMarginBottom">
						<label class="col-form-label col-lg-1 text-right">Group
							Name</label>
						<div class="col-lg-2">
							<input id="presentationName" type="text" class="form-control">
						</div>
						<label class="col-form-label col-lg-2 text-right">Inherits
							From</label>
						<div class="col-lg-2">
							<select id="inheritsFromSelectbox"
								class="selectpicker form-control">
							</select>
						</div>
						<div class="col-form-label col-lg-2 float-right text-right">Is
							Production</div>
						<div class="text-right col-lg-1">
							<!-- <input type="checkbox" id="isProductionCheckbox" name="isProductionCheckbox"> -->
							<!-- <label class="col-form-label col-lg-4">Enabled</label> -->
							<div class="form-check form-check-switchery"">
								<label class="form-check-label"> <input type="checkbox"
									id="isProductionSwitch" class="form-check-input-switchery"
									data-fouc>
								</label>
							</div>
						</div>
					</div>
					<div class="mb-4">
						<!-- Solid divider -->
						<hr class="solid">
					</div>
					<table id="presentationElementsTable" resize_on_window_resize="95"
						class="table table-hover datatable-responsive">
						<thead>
							<tr>
								<th>Data Type Standard</th>
								<th>Data Type Code</th>
								<th>Units</th>
								<th>Fractional Digits</th>
								<th>Min Value</th>
								<th>Max Value</th>
								<th>Actions</th>
							</tr>
						</thead>
						<tbody id="presentationElementsTableBody">
							<!--This is where the data rows go-->
						</tbody>
					</table>
				</div>
				<div class="modal-footer">
					<!--<button type="button" class="btn btn-link" data-dismiss="modal">Close</button>-->
					<button type="button" class="btn btn-secondary"
						id="cancelPresentationModalButton" data-dismiss="modal">Cancel</button>
					<button type="button" class="btn btn-success"
						id="savePresentationModalButton">Save</button>
				</div>
			</div>
		</div>
	</div>
	<!-- /presentation modal -->
	<jsp:include page="/resources/jsp/includes/decodes.jsp" />
	<script src="../resources/js/plugins/forms/styling/switchery.min.js"></script>
	<script src="../resources/js/plugins/forms/styling/switch.min.js"></script>
	<script src="../resources/js/presentation.js"></script>
	<script src="../resources/js/datatables/helpers.js"></script>
	<script src="../resources/js/lib/dom_utilities.js"></script>
</html>
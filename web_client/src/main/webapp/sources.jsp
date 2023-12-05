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
							<span class="font-weight-semibold">OpenDCS</span> - Sources
						</h4>
						<a href="#" class="header-elements-toggle text-default d-md-none"><i
							class="icon-more"></i></a>
					</div>
				</div>
			</div>
			<!-- /page header -->

			<!-- Content area -->
			<div class="content">
				<!-- Basic responsive configuration -->
				<div class="card large-padding">
					<div class="card-header header-elements-inline"></div>
					<table id="sourcesTable"
						class="table table-striped table-hover datatable-responsive tablerow-cursor w-100">
						<thead>
							<tr>
								<th>Name</th>
								<th>Type</th>
								<th>Arguments</th>
								<th>Used By</th>
								<th>Actions</th>
								<th>ID</th>
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

<!-- Success modal -->
<div id="modal_success" class="modal fade sourceDialog" tabindex="-1"
	data-keyboard="false" data-backdrop="static">
	<div class="modal-dialog modal-dialog-scrollable opendcs-modal-90">
		<div class="modal-content h-100">
			<div class="modal-header opendcs-modal-header-medium bg-secondary">
				<h6 class="modal-title" id="sourcesTitle">Sources</h6>
			</div>
			<div id="displayedId" class="displayNone"></div>
			<div class="modal-body opendcs-modal-body-100 d-flex flex-column">
				<!--<h6 class="font-weight-semibold">Network List Name</h6>-->
				<div class="card w-100 p-3">
					<div class="row smallMarginBottom">

						<label class="col-form-label col-lg-1 text-right">Name</label>
						<div class="col-lg-3">
							<input id="sourceName" type="text" class="form-control">
						</div>
						<label class="col-form-label col-lg-2 text-right">Type:</label>
						<div class="col-lg-4">
							<select id="sourceType" class="selectpicker form-control"
								autofocus>

							</select>
						</div>
						<div id="sourceId" class="invisible"></div>
					</div>

				</div>
				<div class="row flex-grow-1">
					<div class="col-lg-3">
						<div class="card w-100">
							<table id="groupTable"
								class="table table-hover datatable-responsive tablerow-cursor">
								<caption class="captionTitleCenter">Group Members</caption>
								<thead>
									<tr>
										<th>Group Member</th>
										<th>Id</th>
									</tr>
								</thead>

								<tbody id="groupTableBody">

								</tbody>
							</table>
						</div>
					</div>
					<div class="col-lg-9">
						<div class="card h-100 w-100">
							<table id="propertiesTable" resize_on_window_resize="96.8"
								class="w-100 table table-hover datatable-responsive co-8">
								<caption class="captionTitleCenter">
									Properties
									<button
										class="btn btn-secondary float-right captionButton mt-1 mr-2"
										id="addPropertyButton">+</button>
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

			<div class="modal-footer">
				<!--<button type="button" class="btn btn-link" data-dismiss="modal">Close</button>-->
				<!-- <button type="button" class="btn btn-danger  mr-auto" id="deleteSourceModalButton">Delete</button> -->
				<button type="button" class="btn btn-secondary"
					id="cancelSourcesModalButton" data-dismiss="modal">Cancel</button>
				<button type="button" class="btn btn-success"
					id="saveSourcesModalButton">Save</button>
			</div>
		</div>
	</div>
</div>


<jsp:include page="/resources/jsp/includes/decodes.jsp" />
<script src="../resources/js/datatables/helpers.js"></script>
<script src="../resources/js/sources.js"></script>
<script
	src="https://cdn.datatables.net/rowreorder/1.2.8/js/dataTables.rowReorder.min.js"></script>

</html>
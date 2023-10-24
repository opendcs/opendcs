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
							<span class="font-weight-semibold">OpenDCS</span> - Sites
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
				<div class="card large-padding h-100">
					<div class="card-header header-elements-inline"></div>
					<table id="sitesTable"
						class="table table-hover datatable-responsive tablerow-cursor w-100">
						<thead>
							<tr>
								<th>Site Id</th>
								<th id="siteNameColumnHeader">Site Name (N/A)</th>
								<th>Configured Site Names</th>
								<th>Description</th>
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


<!-- Site modal -->
<div id="modal_site" class="modal fade" tabindex="-1"
	data-keyboard="false" data-backdrop="static">
	<div
		class="modal-dialog modal-dialog-scrollable modal-med opendcs-modal-90">
		<div class="modal-content">
			<div class="modal-header bg-secondary">
				<h6 class="modal-title" id="sitesTitle">Site</h6>
			</div>
			<div id="displayedSiteId" class="displayNone"></div>
			<div class="modal-body grey-background">
				<div class="row h-100">
					<div class="col-lg-7">
						<div class="row h-50 p-1">
							<div class="col-lg-12 card">
								<table id="siteNamesTable" resize_on_window_resize="97"
									class="table table-hover datatable-responsive border">
									<caption id="siteNamesCaption" class="captionTitleCenter">
										Site Names
										<button function="addBlankRow" class="btn btn-secondary float-right captionButton"
											id="addSiteNameButton">+</button>
									</caption>
									<thead>
										<tr>
											<th>Type</th>
											<th>Identifier</th>
											<th class="text-center">Actions</th>
										</tr>
									</thead>

									<tbody id="groupTableBody">

									</tbody>
								</table>
							</div>
						</div>
						<div class="row h-50 p-1">
							<div class="col-lg-12 card">
								<table id="propertiesTable" resize_on_window_resize="97"
									class="table table-hover datatable-responsive co-8 border">
									<caption class="captionTitleCenter">
										Properties
										<button function="addBlankRow" class="btn btn-secondary float-right captionButton">+</button>
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
					<div class="col-lg-5">
						<div class="row h-100 p-1">
							<div class="card col-lg-12">
								<form class="form-validate-jquery"
									onkeydown="return event.key != 'Enter';">
									<table
										class="table table-hover datatable-responsive dataTable no-footer mb-2">
										<caption class="captionTitleCenter">Details</caption>
									</table>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Latitude</label>
										<div class="col-lg-9">
											<input id="latitudeTextbox" type="text" name="numbers"
												class="form-control" required
												placeholder="Enter the latitude value of the site.">
										</div>
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Longitude</label>
										<div class="col-lg-9">
											<input id="longitudeTextbox" type="text" name="numbers"
												class="form-control" required
												placeholder="Enter the longitude value of the site.">
										</div>
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Elevation</label>
										<div class="col-lg-9">
											<input id="elevationTextbox" type="text" name="numbers"
												class="form-control" required
												placeholder="Enter the elevation for the site.">
										</div>
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Elev Units</label>
										<div class="col-lg-9">
											<select id="elevUnitsSelectbox" name="elevUnits"
												class="form-control">
												<option value="m">M (Meters)</option>
												<option value="cm">cM (Centimeters)</option>
												<option value="ft">ft (Feet)</option>
												<option value="in">in (Inches)</option>
												<option value="km">kM (Kilometers)</option>
												<option value="mm">mM (Millimeters)</option>
												<option value="mi">mi (Miles)</option>
												<option value="nmi">nmi (Nautical Miles)</option>
												<option value="um">uM (Micrometers)</option>
												<option value="yd">yd (Yards)</option>
											</select>
										</div>
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Nearest City</label>
										<div class="col-lg-9">
											<input id="nearestCityTextbox" type="text"
												class="form-control no_validate" required
												placeholder="Enter the nearest city.">
										</div>
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Time Zone</label>
										<div class="col-lg-9">
											<select id="tzSelectbox" name="elevUnits" class="form-control">
											</select>
										</div>
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">State</label>
										<div class="col-lg-9">
											<input id="stateTextbox" type="text"
												class="form-control no_validate" required
												placeholder="Enter the two letter abbreviation of the state.">
										</div>
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Country</label>
										<div class="col-lg-9">
											<input id="countryTextbox" type="text"
												class="form-control no_validate" required
												placeholder="Enter the country.">
										</div>
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Region</label>
										<div class="col-lg-9">
											<input id="regionTextbox" type="text"
												class="form-control no_validate" required
												placeholder="Enter the region of this site.">
										</div>
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Public Name</label>
										<div class="col-lg-9">
											<input id="publicNameTextbox" type="text"
												class="form-control no_validate" required
												placeholder="Enter the public name for this site.">
										</div>
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Description</label>
										<div class="col-lg-9">
											<textarea id="descriptionTextbox" rows="3" cols="3"
												class="form-control"
												placeholder="Enter the description of this platform."></textarea>
										</div>
									</div>
								</form>
							</div>
						</div>
					</div>
				</div>
			</div>

			<div class="modal-footer">
				<!--<button type="button" class="btn btn-link" data-dismiss="modal">Close</button>-->
				<!-- <button type="button" class="btn btn-danger  mr-auto" id="deleteSourceModalButton">Delete</button>-->
				<button type="button" class="btn btn-secondary"
					id="cancelSiteModalButton" data-dismiss="modal">Cancel</button>
				<button type="button" class="btn btn-success"
					id="saveSiteModalButton">Save</button>
			</div>
		</div>
	</div>
</div>
<!-- /sites modal -->

<jsp:include page="/resources/jsp/includes/decodes.jsp" />

<script src="../resources/js/plugins/forms/validation/validate.min.js"></script>
<script src="../resources/js/lib/time.js"></script>
<script src="../resources/js/datatables/helpers.js"></script>
<script src="../resources/js/datatables/datatables.js"></script>
<script src="../resources/js/lib/dom_utilities.js"></script>
<script src="../resources/js/sites.js"></script>
</html>
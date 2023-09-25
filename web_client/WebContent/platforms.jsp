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
							<span class="font-weight-semibold">OpenDCS</span> - Platforms
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
					<table id="platformsTable"
						class="table table-hover datatable-responsive tablerow-cursor w-100">
						<thead>
							<tr>
								<th>Id</th>
								<th>Platform</th>
								<th>Agency</th>
								<th>Transport-ID</th>
								<th>Config</th>
								<th>Expiration</th>
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


<!-- Platform modal -->
<div id="modal_platform" class="modal fade" tabindex="-1"
	data-keyboard="false" data-backdrop="static">
	<div class="modal-dialog modal-dialog-scrollable opendcs-modal-90"
		class_old="modal-dialog modal-xl modal-height-xl">
		<div class="modal-content h-100">
			<div class="modal-header bg-secondary">
				<h6 class="modal-title" id="platformTitle">Platforms</h6>
			</div>
			<div id="displayedPlatformId" class="displayNone"></div>
			<div class="modal-body grey-background total_height-header-footer">
				<div class="row h-100">
					<div class="col-lg-5 h-100 d-flex flex-column">
						<div class="row m-1 h-50">
							<div
								class="card col-lg-12 pl-3 pr-3 pt-1 pb-1 h-100 opendcs_y_scrollable">
								<form class="form-validate-jquery"
									onkeydown="return event.key != 'Enter';">
									<table
										class="table table-hover datatable-responsive dataTable no-footer mb-1">
										<caption class="captionTitleCenter">Details</caption>
									</table>

									<div class="form-group row">
										<label class="col-form-label col-lg-3">Site</label>
										<div class="col-lg-9">
											<select id="siteSelectbox" name="siteSelectbox"
												data-placeholder="Select a site"
												class="form-control select-clear" data-fouc>
												<option></option>
											</select>
										</div>
										<!-- 
										<div class="col-lg-9">
											<select id="siteSelectbox" name="siteSelectbox" class="form-control">
												<option value=""></option>
											</select>
										</div>
										-->
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Designator</label>
										<div class="col-lg-9">
											<input id="designatorTextbox" type="text"
												class="form-control" required
												placeholder="Enter the designator for this platform.">
										</div>
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Config</label>
										<div class="col-lg-8">
											<select id="configSelectbox" name="configSelectbox"
												data-placeholder="Select a config"
												class="form-control select-clear" data-fouc>
												<option></option>
											</select>
										</div>
										<div class="col-lg-1">
											<span class="w-100 h-100 pointerCursor"> <i
												id="editConfig" class="icon-pencil7"></i>
											</span>
										</div>
									</div>
									<div class="form-group row">
										<label class="col-form-label col-lg-3">Owner Agency</label>
										<div class="col-lg-9">
											<input id="ownerAgencyTextbox" type="text"
												name="ownerAgencyTextbox" class="form-control" required
												placeholder="Enter the agency for this platform.">
										</div>
									</div>

									<div class="form-group row">
										<label class="col-form-label col-lg-2">Description</label>
										<div class="col-lg-10">
											<textarea id="descriptionTextbox" rows="3" cols="3"
												class="form-control"
												placeholder="Enter the description of this platform."></textarea>
										</div>
									</div>

									<div class="form-group row">
										<label class="col-form-label col-lg-3">Production</label>
										<div class="col-lg-9 form-check form-check-switchery"
											id="isProductionDiv">
											<label class="form-check-label"> <input
												type="checkbox" id="isProduction"
												class="form-check-input-switchery" data-fouc>
											</label>

										</div>
									</div>
								</form>
							</div>
						</div>

						<div class="row m-1 flex-grow-1">
							<div
								class="card col-lg-12 pl-3 pr-3 pt-1 pb-1 h-100 opendcs_y_scrollable">
								<table id="propertiesTable" resize_on_window_resize="96"
									class="table table-hover datatable-responsive co-8 border">
									<caption class="captionTitleCenter">
										Properties
										<button
											class="btn btn-secondary float-right captionButton mt-1"
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
					<div class="col-lg-7 h-100 d-flex flex-column">
						<div class="row m-1 h-50">
							<div class="col-lg-12 card p-3 h-100  opendcs_y_scrollable">
								<table id="sensorInformationTable" resize_on_window_resize="97"
									class="table table-hover datatable-responsive border tablerow-cursor">
									<caption id="siteNamesCaption" class="captionTitleCenter">
										Platform Sensor Information
										<!-- <button class="btn btn-secondary float-right captionButton" id="addSensorButton">+</button> -->
									</caption>
									<thead>
										<tr>
											<th>Sensor Id</th>
											<th>Name</th>
											<th>Actual Site</th>
											<th>Properties</th>
											<th>Actual Site ID</th>
											<th>SensorProperties</th>
											<th>Min</th>
											<th>Max</th>
											<th>USGS DDNO</th>
										</tr>
									</thead>
									<tbody></tbody>
								</table>
							</div>
						</div>
						<div class="row m-1 flex-grow-1">
							<div class="col-lg-12 card p-3 h-100  opendcs_y_scrollable">
								<table id="transportMediaTable" resize_on_window_resize="96"
									class="table table-hover datatable-responsive co-8 border tablerow-cursor">
									<caption class="captionTitleCenter">
										Transport Media
										<button class="btn btn-secondary float-right captionButton"
											id="addTransportMediaButton">+</button>
									</caption>
									<thead>
										<tr>
											<th>Type</th>
											<th>Id</th>
											<th>Script Name</th>
											<th>Selector</th>
											<th>Time Zone</th>
											<th>Time Adjustment</th>
											<th>Channel Number</th>
											<th>1st Transmission Time</th>
											<th>Transmit Interval</th>
											<th>Transmission Duration</th>
											<th>Preamble</th>
											<th>Logger Type</th>
											<th>Login</th>
											<th>User Name</th>
											<th>Password</th>
											<th>Baud</th>
											<th>Parity</th>
											<th>Stop Bits</th>
											<th>Data Bits</th>
											<th class="text-center">Actions</th>
										</tr>
									</thead>
									<tbody></tbody>
								</table>
							</div>
						</div>
					</div>
				</div>

			</div>

			<div class="modal-footer pt-3">
				<!--<button type="button" class="btn btn-link" data-dismiss="modal">Close</button>-->
				<!-- <button type="button" class="btn btn-danger  mr-auto" id="deleteSourceModalButton">Delete</button>-->
				<button type="button" class="btn btn-secondary"
					id="cancelSiteModalButton" data-dismiss="modal">Cancel</button>
				<button type="button" class="btn btn-success"
					id="savePlatformModalButton">Save</button>
			</div>
		</div>
	</div>
	<!-- /platform modal -->

	<!-- Transport Media modal -->
	<div id="modal_transportmedia" class="modal fade" tabindex="-1"
		data-keyboard="false" data-backdrop="static">
		<div
			class="modal-dialog modal-dialog-scrollable opendcs-modal-tall-narrow">
			<div class="modal-content h-100">
				<div class="modal-header bg-secondary">
					<h6 class="modal-title" id="transportMediaTitle">Edit
						Transport Medium</h6>
				</div>
				<div id="displayedTmRowIndex" class="displayNone"></div>
				<div class="modal-body grey-background">
					<div class="row m-1 h-50">
						<div class="card col-lg-12 p-2 h-100 overflow-auto">
							<table
								class="table table-hover datatable-responsive dataTable no-footer mb-2">
								<caption class="captionTitleCenter">General Details</caption>
							</table>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Medium
									Type</label>
								<div class="col-lg-9">
									<select id="mediumTypeSelectbox"
										class="selectpicker form-control">
									</select>
								</div>
								<!-- 
							<label class="col-form-label col-lg-3">Medium Type</label>
							<div class="col-lg-9">
								<input id="mediumTypeTextbox" type="text" class="form-control" required placeholder="Enter The Medium Type.">
							</div>
							-->
							</div>
							<div class="form-group row mb-2">
								<label data-mediumtype="data-logger,iridium,other,shef"
									class="col-form-label col-lg-3">Medium Identifier</label> <label
									data-mediumtype="goes,goes-random,goes-self-timed"
									class="col-form-label col-lg-3">DCP Address</label> <label
									data-mediumtype="incoming-tcp" class="col-form-label col-lg-3">Looger
									ID</label> <label data-mediumtype="polled-modem"
									class="col-form-label col-lg-3">Telephone Number</label> <label
									data-mediumtype="polled-tcp" class="col-form-label col-lg-3">Host:Port</label>
								<div class="col-lg-9">
									<input id="dcpAddressTextbox" type="text" class="form-control"
										required placeholder="Enter The DCP Address.">
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Decoding Script</label>
								<div class="col-lg-9">
									<input id="decodingScriptTextbox" type="text"
										class="form-control" required
										placeholder="Enter the Decoding Script.">
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Time Zone</label>
								<div class="col-lg-9">
									<input id="timezoneTextbox" type="text" class="form-control"
										required placeholder="Enter the timezone.">
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Time Adjustment</label>
								<div class="col-lg-9">
									<input id="timeAdjustmentTextbox" type="text"
										class="form-control" required
										placeholder="Enter the time adjustment.">
								</div>
							</div>
						</div>
					</div>
					<div class="row m-1 h-50">
						<div class="card col-lg-12 p-2 h-100 overflow-auto"
							data-mediumtype="goes,goes-random,goes-self-timed">
							<table
								class="table table-hover datatable-responsive dataTable no-footer mb-2">
								<caption class="captionTitleCenter">GOES DCP Channel
									Parameters</caption>
							</table>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Channel Number</label>
								<div class="col-lg-9">
									<input id="channelNumTextbox" type="text" class="form-control"
										required placeholder="Enter the channel number.">
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">1st Transmission
									Time</label>
								<div class="col-lg-9">
									<input id="firstTransTimeTextbox" type="text"
										class="form-control" required placeholder="(HH:MM:SS)">
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Transmit Interval</label>
								<div class="col-lg-9">
									<input id="transmitIntervalTextbox" type="text"
										class="form-control" required placeholder="(HH:MM:SS)">
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Transmit Duration</label>
								<div class="col-lg-9">
									<input id="transmitDurationTextbox" type="text"
										class="form-control" required placeholder="(HH:MM:SS)">
								</div>
							</div>
							<!-- 
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Preamble</label>
								<div class="col-lg-9">
									<select id="preambleSelectbox" class="selectpicker form-control">
			                      		<option value="L">Long</option>
			                      		<option value="S">Short</option>
			                      		<option value="U">Unknown</option>
			                        </select>
								</div>
							</div>
							-->
						</div>
						<div class="card col-lg-12 p-2 h-100 overflow-auto"
							data-mediumtype="incoming-tcp,polled-tcp">
							<table
								class="table table-hover datatable-responsive dataTable no-footer mb-2">
								<caption class="captionTitleCenter">TCP Parameters</caption>
							</table>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Logger Type</label>
								<div class="col-lg-9">
									<select id="loggerTypeSelectbox"
										class="selectpicker form-control">
										<option value=""></option>
										<option value="amasser">Amasser</option>
										<option value="campbell">Campbell</option>
										<option value="fts">fts</option>
										<option value="h555">h555</option>
										<option value="sutron">Sutron</option>
										<option value="vedasii">Vedasii</option>
									</select>
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Do Login</label>
								<div class="col-lg-9 form-check form-check-switchery"
									id="doLoginDiv">
									<label class="form-check-label"> <input
										onclick="doLoginClicked(event, this)" type="checkbox"
										id="doLoginEnabled" class="form-check-input-switchery"
										data-fouc>
									</label>

								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">User Name</label>
								<div class="col-lg-9">
									<input id="tcpUsername" type="text" class="form-control"
										required placeholder="Username" disabled>
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Password</label>
								<div class="col-lg-9">
									<input id="tcpPassword" type="password" class="form-control"
										required placeholder="Password" disabled>
								</div>
							</div>
						</div>
						<div class="card col-lg-12 p-2 h-100 overflow-auto"
							data-mediumtype="polled-modem">
							<table
								class="table table-hover datatable-responsive dataTable no-footer mb-2">
								<caption class="captionTitleCenter">TCP Parameters</caption>
							</table>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Logger Type</label>
								<div class="col-lg-9">
									<select id="loggerTypeSelectbox"
										class="selectpicker form-control">
										<option value=""></option>
										<option value="amasser">Amasser</option>
										<option value="campbell">Campbell</option>
										<option value="fts">fts</option>
										<option value="h555">h555</option>
										<option value="sutron">Sutron</option>
										<option value="vedasii">Vedasii</option>
									</select>
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Baud Combo</label>
								<div class="col-lg-9">
									<!-- 
									<input type="text" name="myText_ex" value="Norway" selectBoxOptions="Canada;Denmark;Finland;Germany;Mexico;Norway;Sweden;United Kingdom;United States">
									-->

									<input type="text" name="combobox" list="baudList" id="baud"
										class="selectpicker form-control">
									<datalist id="baudList">
										<option value="300">
										<option value="1200">
										<option value="2400">
										<option value="4800">
										<option value="9600">
									</datalist>

								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Baud</label>
								<div class="col-lg-9">
									<select id="baudSelectbox" class="selectpicker form-control">
										<option value="0">0</option>
										<option value="300">300</option>
										<option value="1200">1200</option>
										<option value="2400">2400</option>
										<option value="4800">4800</option>
										<option value="5600">5600</option>
									</select>
								</div>
							</div>

							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Parity</label>
								<div class="col-lg-9">
									<select id="paritySelectbox" class="selectpicker form-control">
										<option value="N">None</option>
										<option value="E">Even</option>
										<option value="O">Odd</option>
										<option value="M">Mark</option>
										<option value="S">Space</option>
										<option value="U">Unknown</option>
									</select>
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Stop Bits</label>
								<div class="col-lg-9">
									<select id="stopBitsSelectbox"
										class="selectpicker form-control">
										<option value="0">0</option>
										<option value="1">1</option>
										<option value="2">2</option>
									</select>
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Data Bits</label>
								<div class="col-lg-9">
									<select id="dataBitsSelectbox"
										class="selectpicker form-control">
										<option value="7">7</option>
										<option value="8">8</option>
									</select>
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Do Login</label>
								<div class="col-lg-9 form-check form-check-switchery"
									id="doLoginDiv">
									<label class="form-check-label"> <input
										onclick="doLoginClicked(event, this)" type="checkbox"
										id="doLoginEnabled" class="form-check-input-switchery"
										data-fouc>
									</label>

								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">User Name</label>
								<div class="col-lg-9">
									<input id="tcpUsername" type="text" class="form-control"
										required placeholder="Username" disabled>
								</div>
							</div>
							<div class="form-group row mb-2">
								<label class="col-form-label col-lg-3">Password</label>
								<div class="col-lg-9">
									<input id="tcpPassword" type="text" class="form-control"
										required placeholder="Password" disabled>
								</div>
							</div>
						</div>
					</div>
				</div>
				<div class="modal-footer pt-3">
					<!--<button type="button" class="btn btn-link" data-dismiss="modal">Close</button>-->
					<!-- <button type="button" class="btn btn-danger  mr-auto" id="deleteSourceModalButton">Delete</button>-->
					<button type="button" class="btn btn-secondary"
						id="transportMediaModalCancel">Cancel</button>
					<button type="button" class="btn btn-success"
						id="transportMediaModalOk">Ok</button>
				</div>
			</div>
		</div>
	</div>



</div>


<!-- Platform Sensor modal -->
<div id="modal_platformsensor" class="modal fade" data-keyboard="false"
	data-backdrop="static">
	<div
		class="modal-dialog modal-dialog-scrollable opendcs-modal-tall-narrow">
		<div class="modal-content h-100">
			<div class="modal-header opendcs-modal-header-medium bg-secondary">
				<h6 class="modal-title" id="platformSensorTitle">Platform
					Sensor Parameters</h6>
			</div>
			
			<div class="modal-body grey-background p-1 d-flex flex-column">
				<div id="displayedPlatformSensorRowId" class="displayNone"></div>
				<div class="form-group row">
					<label class="col-form-label col-2 text-right">Sensor
						Number</label> <label class="col-form-label col-2 text-left"
						id="sensorNumberText"></label> <label
						class="col-form-label col-2 text-right">Sensor Name</label> <label
						class="col-form-label col-2 text-left" id="sensorNameText"></label>
					<label class="col-form-label col-2 text-right">Param Code</label> <label
						class="col-2 text-left" id="sensorParamCodeText"></label>
				</div>
				<div class="row m-1">
					<div class="card col-lg-12 p-3 h-100">
						<div class="form-group row mb-2">
							<label class="col-form-label col-3 text-right">Actual
								Site</label>
							<div class="col-6">
								<select id="actualSiteSelectbox" name="actualSiteSelectbox"
									data-placeholder="(inherited)"
									class="form-control select-clear" data-fouc>
									<option data-site_id="-1"></option>
								</select>
							</div>
						</div>
						<div class="form-group row mt-2">
							<p class="text-center col-12">Min and Max defined here
								override min/max in configuration.</p>
						</div>
						<div class="form-group row">
							<label data-mediumtype="polled-tcp"
								class="col-form-label col-lg-3 text-right">Platform Min</label>
							<div class="col-lg-3">
								<input id="platformSpecificMinTextbox" type="text"
									class="form-control">
							</div>
							<label data-mediumtype="polled-tcp"
								class="col-form-label col-lg-3 text-right">Config Min</label>
							<div class="col-lg-3">
								<input id="configMinTextbox" type="text" class="form-control"
									readonly>
							</div>
						</div>
						<div class="form-group row">
							<label data-mediumtype="polled-tcp"
								class="col-form-label col-lg-3 text-right">Platform Max</label>
							<div class="col-lg-3">
								<input id="platformSpecificMaxTextbox" type="text"
									class="form-control">
							</div>
							<label data-mediumtype="polled-tcp"
								class="col-form-label col-lg-3 text-right">Config Max</label>
							<div class="col-lg-3">
								<input id="configMaxTextbox" type="text" class="form-control"
									readonly>
							</div>
						</div>
						<div class="form-group row">
							<label data-mediumtype="polled-tcp"
								class="col-form-label col-lg-3 text-right">USGS DDNO</label>
							<div class="col-lg-3">
								<input id="usgsDdnoTextbox" type="text" class="form-control">
							</div>
						</div>
					</div>

				</div>
				<div class="form-group row  flex-grow-1">
					<div class="col-lg-12">
						<div class="card w-100 h-100">
							<table id="platformSensorPropertiesTable"
								resize_on_window_resize="95"
								class="table table-hover datatable-responsive co-8 border">
								<!-- <caption class="captionTitleCenter">Additional Properties</caption> -->
								<caption class="captionTitleCenter">
									Additional Properties
									<button
										class="btn btn-secondary float-right captionButton mt-1"
										id="addPlatformSensorPropertyButton">+</button>
								</caption>
								<thead>
									<tr>
										<th>Name</th>
										<th>Value</th>
										<th class="text-center">Actions</th>
									</tr>
								</thead>
							</table>
						</div>
					</div>
				</div>
			</div>
			<div class="modal-footer">
				<!--<button type="button" class="btn btn-link" data-dismiss="modal">Close</button>-->
				<!-- <button type="button" class="btn btn-danger  mr-auto" id="deleteSourceModalButton">Delete</button>-->
				<button type="button" class="btn btn-secondary"
					id="platformSensorModalCancel">Cancel</button>
				<button type="button" class="btn btn-success"
					id="platformSensorModalOk">Ok</button>
			</div>
		</div>
	</div>
</div>
<!-- /platform sensor modal -->


<jsp:include page="/resources/jsp/includes/decodes.jsp" />

<script src="../resources/js/plugins/forms/styling/switchery.min.js"></script>
<script src="../resources/js/plugins/forms/styling/switch.min.js"></script>
<script src="../resources/js/plugins/forms/styling/uniform.min.js"></script>
<script src="../resources/js/datatables/helpers.js"></script>
<script src="../resources/js/lib/dom_utilities.js"></script>
<script src="../resources/js/lib/object_utilities.js"></script>
<script src="../resources/js/lib/date_utilities.js"></script>
<script src="../resources/js/platforms.js"></script>
</html>
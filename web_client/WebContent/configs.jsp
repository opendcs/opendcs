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
							<h4><span class="font-weight-semibold">OpenDCS</span> - Configs</h4>
							<a href="#" class="header-elements-toggle text-default d-md-none"><i class="icon-more"></i></a>
						</div>
					</div>  
				</div> <!-- /page header -->
			
				<!-- Content area -->
				<div class="content">
					<!-- Basic responsive configuration -->
					<div class="card large-padding h-100">
						<div class="card-header header-elements-inline">
						</div>
						<table id="configsTable" class="table table-hover datatable-responsive tablerow-cursor w-100">
							<thead>
								<tr>
									<th>Config ID</th>
									<th>Name</th>
									<th>Equipment Id</th>
									<th># Platforms</th>
									<th>Description</th>
									<th class="text-center">Actions</th>
								</tr>
							</thead>
							<tbody id="tableBody">
								<!--This is where the data rows go-->
							</tbody>
						</table>
					</div> <!-- /basic responsive configuration -->
				</div> <!-- /Content area -->


				<jsp:include page="/resources/jsp/footers/decodes.jsp" /> 
				
			</div> <!-- /main content -->
		</div> <!-- /page content -->
	</body>

	<!-- Success modal -->
	<div id="modal_config" class="modal fade" tabindex="-1" data-keyboard="false" data-backdrop="static">
		<div class="modal-dialog modal-dialog-scrollable opendcs-modal-90">
			<div class="modal-content">
				<div class="modal-header bg-secondary">
					<h6 class="modal-title" id="configTitle">Configs</h6>
				</div>

				<div class="modal-body">
					<!--<h6 class="font-weight-semibold">Network List Name</h6>-->
					<div class="card pt-2 pb-2">
						<div class="form-group row">
							<label class="col-form-label col-lg-1 text-right">Name:</label>
							<div class="col-lg-2">
								<input id="configName" type="text" class="form-control">
							</div>
						    <label class="col-form-label col-lg-1 text-right">Num Platforms:</label>
							<div class="col-lg-2">
								<input id="numPlatforms" type="text" class="form-control" readonly>
							</div>
							<label class="col-form-label col-lg-1 text-right">Description:</label>
							<div class="col-lg-4">
								<input id="configDescription" type="text" class="form-control">
							</div>
						</div>
					</div>
					<div class="card h-50">
						<table id="sensorTable" resize_on_window_resize="95" class="table table-hover datatable-responsive tablerow-cursor">
							<caption class="captionTitleCenter">Sensors
								<button class="btn btn-secondary float-right captionButton" id="addSensorButton">+</button>
							</caption>
							<thead>
								<tr>
									<th>Sensor</th>
									<th>Name</th>
									<th>Data Type</th>
									<th>Mode</th>
									<th>Sampling Times</th>
									<th>Properties</th>
									<th class="text-center">Actions</th>
									<th>Absolute Max</th>
									<th>Absolute Min</th>
									<th>Recording Interval</th>
									<th>Time of First Sample</th>
									<th>Usgs Stat Code</th>
									<th>DataTypeJson</th>
									<th>PropertiesJson</th>
								</tr>
							</thead>
							
						</table>
					</div>
					<div class="card h-40">
						<table id="decodingScriptTable" resize_on_window_resize="95" class="table table-hover datatable-responsive tablerow-cursor overflow_y-auto">
						<caption class="captionTitleCenter">Decoding Scripts
							<button class="btn btn-secondary float-right captionButton" id="addDecodingScriptButton">+</button>
						</caption>
							<thead>
								<tr>
									<th>Name</th>
									<th>Type</th>
									<th class="text-center">Actions</th>
								</tr>
							</thead>
							<tbody id="decodingTableBody">
								<!--This is where the data rows go-->
							</tbody>
						</table>
					</div>
				</div>

				<div class="modal-footer">
					<!--<button type="button" class="btn btn-link" data-dismiss="modal">Close</button>-->
					<button type="button" class="btn btn-secondary" id="cancelConfigsModalButton" data-dismiss="modal">Cancel</button>
					<button type="button" class="btn btn-success" id="saveConfigsModalButton">Save</button>
				</div>
			</div>
		</div>
	</div>
	<!--  <script src="/static/js/modal_configs.js"></script> -->
	<!-- /success modal -->
	
	<!-- Config Sensor modal -->
	<div id="modal_configsensor" class="modal fade" data-keyboard="false" data-backdrop="static">
		<div class="modal-dialog modal-sm-med modal-dialog-scrollable">
			<div class="modal-content h-100">
				<div class="modal-header bg-secondary">
					<h6 class="modal-title" id="editConfigTitle">Edit Config Sensor</h6>
				</div>
				<div id="displayedConfigSensorRowId" class="displayNone"></div>
				<div class="modal-body grey-background p-1">
					<div class="form-group row">
						<label class="col-form-label col-2 text-right">Configuration</label>
						<label class="col-form-label col-2 text-left" id="configurationText"></label>
						<label class="col-form-label col-2 text-right">Sensor</label>
						<label class="col-form-label col-2 text-left" id="sensorNumberText"></label>
					</div>
				<div class="row m-1 h-50">
					<div class="card col-lg-12 p-3">
						<div class="form-group row">
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-3 text-right">Sensor Name</label>
							<div class="col-lg-4">
								<input id="sensorNameTextbox" type="text" class="form-control">
							</div>
						</div>
						<div class="form-group row">
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-2 text-right">Standard</label>
							<div class="col-lg-4">
								<select id="standard1Selectbox" name="elevUnits" class="form-control">
								</select>
							</div>
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-2 text-right">Code</label>
							<div class="col-lg-4">
								<input id="code1Textbox" type="text" class="form-control">
							</div>
						</div>
						<div class="form-group row">
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-2 text-right">Standard</label>
							<div class="col-lg-4">
								<select id="standard2Selectbox" name="elevUnits" class="form-control">
								</select>
							</div>
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-2 text-right">Code</label>
							<div class="col-lg-4">
								<input id="code2Textbox" type="text" class="form-control">
							</div>
						</div>
						<div class="form-group row">
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-2 text-right">Standard</label>
							<div class="col-lg-4">
								<select id="standard3Selectbox" name="elevUnits" class="form-control">
								</select>
							</div>
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-2 text-right">Code</label>
							<div class="col-lg-4">
								<input id="code3Textbox" type="text" class="form-control">
							</div>
						</div>
						<div class="form-group row">
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-3 text-right">USGS Stat Code</label>
							<div class="col-lg-4">
								<input id="usgsStatCodeTextbox" type="text" class="form-control">
							</div>
						</div>
						<div class="form-group row">
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-2 text-right">Valid Range - Min</label>
							<div class="col-lg-4">
								<input id="validRangeMin" type="text" class="form-control">
							</div>
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-2 text-right">Max</label>
							<div class="col-lg-4">
								<input id="validRangeMax" type="text" class="form-control">
							</div>
						</div>
						<div class="form-group row">
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-3 text-right">Recording Mode</label>
							<div class="col-lg-4">
								<select id="recordingModeSelectbox" class="form-control">
									<option value="F">Fixed</option>
									<option value="V">Variable</option>
								</select>
							</div>
						</div>
						<div class="form-group row">
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-3 text-right">1st Sample Time</label>
							<div class="col-lg-6 input-group">
								<span class="input-group-prepend">
									<span class="input-group-text"><i class="icon-watch2"></i></span>
								</span>
								<input type="text" class="form-control" id="firstSampleTime" value="00:00:00">
							</div>
							<label class="col-form-label col-lg-3 text-left">HH:MM:SS</label>
						</div>
						<div class="form-group row">
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-3 text-right">Sampling Interval</label>
							<div class="col-lg-6 input-group">
								<span class="input-group-prepend">
									<span class="input-group-text"><i class="icon-watch2"></i></span>
								</span>
								<input type="text" class="form-control" id="samplingInterval" value="00:00:00">
							</div>
							<label class="col-form-label col-lg-3 text-left">HH:MM:SS</label>
						</div>
				
							<div class="form-group row">
								<div class="card col-lg-12 p-3">
									<table id="sensorPropertiesTable" class="table table-hover datatable-responsive co-8 border">
										<caption class="captionTitleCenter">Properties
											<button class="btn btn-secondary float-right captionButton" id="addSensorPropertyButton">+</button>
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
			</div>
		<div class="modal-footer pt-3">
					<!--<button type="button" class="btn btn-link" data-dismiss="modal">Close</button>-->
                	<!-- <button type="button" class="btn btn-danger  mr-auto" id="deleteSourceModalButton">Delete</button>-->
					<button type="button" class="btn btn-secondary" id="configSensorModalCancel">Cancel</button>
					<button type="button" class="btn btn-success" id="configSensorModalOk">Ok</button>
				</div>
		</div>
	</div>
	</div>
	<!-- /config sensor modal -->
	
	
	<!-- Decoding Script modal -->
	<div id="modal_decodingscript" class="modal fade" data-keyboard="false" data-backdrop="static">
		<div class="modal-dialog modal-med">
			<div class="modal-content h-100">
				<div class="modal-header bg-secondary">
					<h6 class="modal-title" id="platformSensorTitle">Edit Decoding Script</h6>
				</div>
				<div id="displayedDecodingScriptRowId" class="displayNone"></div>
				<div class="modal-body grey-background p-1">
				<div class="row m-1 h-50">
					<div class="card col-lg-7 p-3">
						<div class="form-group row">
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-2 text-right">Script Name</label>
							<div class="col-lg-2">
								<input id="scriptNameTextbox" type="text" class="form-control">
							</div>
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-2 text-right">Data Order</label>
							<div class="col-lg-2">
								<select id="dataOrderSelectbox" name="elevUnits" class="form-control">
									<option value="A">Ascending</option>
									<option value="D">Descending</option>
									<option value="U">Undefined</option>
								</select>
							</div>
							<label data-mediumtype="polled-tcp" class="col-form-label col-lg-2 text-right">Header Type</label>
							<div class="col-lg-2">
								<select id="headerTypeSelectbox" name="elevUnits" class="form-control">
								</select>
							</div>
						</div>
				
						<div class="form-group row">
							<div class="card col-lg-12 p-3">
								<table id="formatStatementsTable" class="table table-hover datatable-responsive co-8 border">
									<caption class="captionTitleCenter">Format Statements</caption>
									<thead>
										<tr>
											<th></th>
											<th>Label</th>
											<th>Format Statement</th>
											<th class="text-center">Actions</th>
										</tr>
									</thead>
								</table>
							</div>
						</div>
						
						<div class="form-group row">
							<div class="card col-lg-12 p-3">
								<table id="sensorUnitConversionsTable" class="table table-hover datatable-responsive co-8 border">
									<caption class="captionTitleCenter">Sensor Unit Conversions</caption>
									<thead>
										<tr>
											<th>#</th>
											<th>Name</th>
											<th>Units</th>
											<th>Algorithm</th>
											<th>A</th>
											<th>B</th>
											<th>C</th>
											<th>D</th>
											<th>E</th>
											<th>F</th>
										</tr>
									</thead>
								</table>
							</div>
						</div>
					  </div>
					  <div class="card col-lg-5 p-3">
					  	<div class="form-group">
					  		<legend class="text-uppercase font-size-sm font-weight-bold">Sample Message</legend>
							
								<ul class="nav nav-tabs">
									<li class="nav-item"><a href="#lrgsTab" class="nav-link rounded-top active" data-toggle="tab">Load From LRGS</a></li>
									<li class="nav-item"><a href="#fileTab" class="nav-link rounded-top" data-toggle="tab">Load From File</a></li>
								</ul>

								<div class="tab-content">
									<div class="tab-pane fade show active" id="lrgsTab">
										<div class="form-group smallMarginBottom row">
											<label class="col-form-label label1 text-center col-5">Source</label>
											<label class="col-form-label label1 text-center col-5">DCP Address</label>
										</div>
										<div class="form-group smallMarginBottom row">
											<div class="field1 nobr col-5">
												<select id="dataSourceSelect" class="selectpicker form-control"></select>
											</div>
											<div class="field1 nobr col-5">
												<!-- <input id="dcpAddressTextbox" type="text" class="form-control"></input>-->
												<input type="text" name="dal" list="dcpAddressList" id="dcpAddressTextbox" class="form-control">
												<datalist id="dcpAddressList">
												<!-- 
												  <option value="Boston">
												  <option value="Cambridge">
												  -->
												</datalist>
											</div>
											<button class="btn btn-secondary col-2" id="loadMessageButton">Load</button>
										</div>
									</div>
									<div class="tab-pane fade" id="fileTab">
										<div>
										 <label for="input-file">Specify a file:</label><br>
										 <input type="file" id="inputFile">
										</div>
									</div>
								</div>
								<div class="row">
									<div class="col-lg-12">
										<textarea rows="3" class="form-control" placeholder="Sample message goes here..." id="sampleMessage"></textarea>
									</div>
								</div>
								<div class="row">
									<div class="col-lg-12 text-right">
										<button class="btn btn-secondary" id="decodeMessageButton">Decode</button>
									</div>
								</div>
								<div class="row">
									<div class="col-lg-12 overflowx-auto" id="decodedDataDiv">
										
									</div>
								</div>
							</div>
					  </div>
				</div>
			</div>
		<div class="modal-footer pt-3">
					<!--<button type="button" class="btn btn-link" data-dismiss="modal">Close</button>-->
                	<!-- <button type="button" class="btn btn-danger  mr-auto" id="deleteSourceModalButton">Delete</button>-->
					<button type="button" class="btn btn-secondary" id="decodingScriptModalCancel">Cancel</button>
					<button type="button" class="btn btn-success" id="decodingScriptModalOk">Ok</button>
				</div>
		</div>
	</div>
	</div>
	<!-- /decoding script modal -->
	
	<jsp:include page="/resources/jsp/includes/decodes.jsp" /> 
	
	<script src="../resources/js/plugins/ui/moment/moment.min.js"></script>
	<script src="../resources/js/plugins/pickers/daterangepicker.js"></script>
	<script src="../resources/js/plugins/pickers/anytime.min.js"></script>
	<script src="../resources/js/plugins/pickers/pickadate/picker.js"></script>
	<script src="../resources/js/plugins/pickers/pickadate/picker.date.js"></script>
	<script src="../resources/js/plugins/pickers/pickadate/picker.time.js"></script>
	<script src="../resources/js/plugins/pickers/pickadate/legacy.js"></script>
	<script src="../resources/js/configs.js"></script>
	<script src="../resources/js/datatables/helpers.js"></script>
	<script src="../resources/js/lib/object_utilities.js"></script>
	<script src="../resources/js/lib/opendcs_utilities.js"></script>
	<script src="../resources/js/lib/date_utilities.js"></script>
	<script src="../resources/js/lib/list_utilities.js"></script>
</html>
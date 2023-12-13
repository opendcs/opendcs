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
							<span class="font-weight-semibold">OpenDCS</span> - Schedule
							Entry
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
					<table id="scheduleEntryTable"
						class="table table-hover datatable-responsive tablerow-cursor w-100">
						<thead>
							<tr>
								<th>Id</th>
								<th>Name</th>
								<th>Loading Application</th>
								<th>Routing Spec</th>
								<th>Enabled?</th>
								<th>Last Modified</th>
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


<!-- Schedule Modal -->
<div id="modal_schedule" class="modal fade" tabindex="-1"
	data-keyboard="false" data-backdrop="static">
	<div class="modal-dialog modal-dialog-scrollable opendcs-modal-90">
		<div class="modal-content">
			<div class="modal-header bg-secondary">
				<h6 class="modal-title" id="modalTitle">
					<span id="modalMainTitle">Schedule</span><span id="modalSubTitle"></span>
				</h6>
			</div>
			<div class="displayNone" id="selectedSheduleRowIndex" data-index=""></div>
			<div class="modal-body grey-background">
				<div class="row">
					<div class="col-xl-4">
						<div class="card">
							<div class="m-3">


								<div class="form-group row">
									<label class="col-form-label col-lg-4">Enabled</label>
									<div class="col-lg-8 form-check form-check-switchery"
										id="editCbtDiv">
										<label class="form-check-label"> <input
											type="checkbox" id="scheduleEntryEnabled"
											class="form-check-input-switchery" data-fouc>
										</label>

									</div>
								</div>

								<div class="form-group row">
									<label class="col-form-label col-lg-4">Schedule Entry
										Name</label>
									<div class="col-lg-8">
										<input id="scheduleEntryName" type="text" class="form-control">
									</div>
								</div>


								<div class="form-group row">
									<label class="col-form-label col-lg-4">Loading
										Application</label>
									<div class="col-lg-8">
										<select id="scheduleEntryLoadingApp" class="form-control">
										</select>
									</div>
								</div>

								<div class="form-group row">
									<label class="col-form-label col-lg-4">Routing Spec</label>
									<div class="col-lg-8">
										<select id="scheduleEntryRoutingSpec" class="form-control">

										</select>
									</div>
								</div>

								<div class="form-group row">
									<label class="col-form-label col-lg-4">Last Modified</label>
									<div class="col-lg-8">
										<p id="lastModified"></p>
									</div>
								</div>
							</div>
						</div>
					</div>
					<div class="col-xl-8">
						<div class="card">
							<div class="pt-3 pr-3 pl-3 pb-1">
								<h5>Execution Schedule</h5>
							</div>
							<div class="form-group row">

								<div class="col-md-1"></div>
								<div class="col-md-9 pt-1 pr-3 pl-3 pb-3" id="execSchedule">
									<div class="form-check border p-3">
										<div class="form-group row">
											<div class="col-md-3">
												<label class="form-check-label exec-schedule-check"> <input
													type="radio" name="runFreq"
													class="form-check-input-styled-primary" value="continuous"
													checked data-fouc> Run Continuously
												</label>
											</div>
										</div>
									</div>

									<div class="form-check border p-3">
										<div class="form-group row">
											<div class="col-md-3">
												<label class="form-check-label exec-schedule-check"> <input
													type="radio" name="runFreq"
													class="form-check-input-styled-primary" value="once"
													data-fouc> Run Once
												</label>
											</div>
											<div class="col-md-9">
												<div class="form-group row">
													<label class="col-form-label col-md-3">Starting at:</label>
													<div class="col-lg-5">
														<input id="onceStartDateTime" class="form-control"
															type="datetime-local" name="datetime-local">
													</div>
													<div class="col-lg-4">
														<select id="onceTimezoneSelect" class="form-control">
														</select>
													</div>
												</div>
											</div>
										</div>
									</div>

									<div class="form-check border p-3">
										<div class="form-group row">
											<div class="col-md-3">
												<label class="form-check-label exec-schedule-check"> <input
													type="radio" name="runFreq"
													class="form-check-input-styled-primary" value="runevery"
													data-fouc> Run Every
												</label>
											</div>
											<div class="col-md-9">
												<div class="form-group row">
													<div class="col-lg-3">
														<input id="runEveryDigit" type="number"
															class="form-control" value="1"></input>
													</div>
													<div class="col-lg-5">
														<select id="runEveryUnit" class="form-control">
															<option value="minute">Minutes</option>
															<option value="hour">Hours</option>
															<option value="day">Days</option>
														</select>
													</div>
												</div>
												<div class="form-group row">
													<label class="col-form-label col-md-3">Starting at:</label>
													<div class="col-lg-5">
														<input id="runEveryStartDateTime" class="form-control"
															type="datetime-local" name="datetime-local">
													</div>
													<div class="col-lg-4">
														<select id="timezoneSelect" class="form-control">
														</select>
													</div>
												</div>
											</div>

										</div>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>

			<div class="modal-footer mt-3">
				<!--<button type="button" class="btn btn-link" data-dismiss="modal">Close</button>-->
				<!-- <button type="button" class="btn btn-danger  mr-auto" id="deleteSourceModalButton">Delete</button> -->
				<button type="button" class="btn btn-secondary"
					id="cancelModalButton" data-dismiss="modal">Cancel</button>
				<button type="button" class="btn btn-success" id="saveModalButton">Save</button>
			</div>
		</div>
	</div>
</div>
<!-- /schedule modal -->


<jsp:include page="/resources/jsp/includes/decodes.jsp" />

<script src="../resources/js/plugins/forms/styling/switchery.min.js"></script>
<script src="../resources/js/plugins/forms/styling/switch.min.js"></script>
<script src="../resources/js/plugins/forms/styling/uniform.min.js"></script>
<script src="../resources/js/schedule.js"></script>
<script src="../resources/js/lib/time.js"></script>
<script src="../resources/js/datatables/helpers.js"></script>

</html>
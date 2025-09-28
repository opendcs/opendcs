<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
	<%@include file="/WEB-INF/common/header.jspf" %>
	<body class="navbar-top">
		<%@include file="/WEB-INF/common/top-bar.jspf" %>
	    <!-- Page content -->
	    <div class="page-content">
        	<%@include file="/WEB-INF/common/sidebar.jspf" %>
	        <div class="content-wrapper">
                <!-- Page header -->
				<div class="page-header page-header-light">
				    <div class="page-header-content header-elements-md-inline">
				        <div class="page-title d-flex">
				            <h4><i class="bi bi-arrow-left mr-2"></i> <span class="font-weight-semibold">OpenDCS</span> - Netlists</h4>
				            <a href="#" class="header-elements-toggle text-default d-md-none"><i class="bi bi-three-dots-vertical"></i></a>
				        </div>
				    </div>  
				</div>
				<!-- /page header -->
	            <!-- Content area -->
	            <div class="content">
	                <!-- Netlist List Card -->
					<div class="card large-padding">
					    <div class="card-header header-elements-inline">
					    </div>
					    <table id="table" class="table table-hover datatable-responsive tablerow-cursor w-100">
					        <thead>
					            <tr>
					                <th>List name</th>
					                <th>Medium Type</th>
					                <th># of Platforms</th>
					                <th>id</th>
					                <th>Actions</th>
					            </tr>
					        </thead>
					        <tbody id="tableBody">
					            <!--This is where the data rows go-->
					        </tbody>
					    </table>
					</div>
					<!-- /netlist list card -->
				</div>
	            <!-- /Content area -->
				<%@include file="/WEB-INF/common/footer.jspf" %>
	        </div>
	        <!-- /main content -->
	    </div>
	    <!-- /page content -->
		<%@include file="/WEB-INF/common/scripts.jspf" %>
		<script src="../resources/js/netlist.js"></script>
		<script src="../resources/js/datatables/helpers.js"></script>
	</body>
	 <!-- Netlist modal -->
	<div id="modal_netlist" class="modal fade" tabindex="-1" data-bs-keyboard="false" data-bs-backdrop="static">
	    <div class="modal-dialog modal-dialog-scrollable opendcs-modal-90">
	        <div class="modal-content h-100">
	            <div class="modal-header opendcs-modal-header-medium bg-secondary">
	            	<h6 class="modal-title" id="modalTitle"><span id="modalMainTitle">Netlist</span><span id="modalSubTitle"></span></h6>
	            </div>
				<div id="displayedId" class="displayNone"></div>
	            <div class="modal-body opendcs-modal-body-100 d-flex flex-column">
	                <div class="form-group row smallMarginBottom">
	                    <label class="col-form-label col-lg-1 text-right">Name</label>
	                    <div class="col-lg-2">
	                        <input id="netlistName" type="text" class="form-control">
	                    </div>
	                  <label class="col-form-label col-lg-2 text-right">Transport Medium Type</label>
	                  <div class="col-lg-2">
	                      <select id="transportMediumTypeSelectbox" class="selectpicker form-control">
	                        </select>
	                    </div>
	                  <label class="col-form-label col-lg-2 text-right">Site Name Type</label>
	                  <div class="col-lg-2">
	                      <select id="siteNameType" class="selectpicker form-control">
	                        <option value="cwms">cwms</option>
	                        </select>
	                    </div>
	                </div>
	                
	                <div class="overflow_y-auto h-100 opendcs_y_scrollable">
		                <table id="netlistTable" class="table table-hover datatable-responsive" resize_on_window_resize="96">
		                    <thead>
		                        <tr>
		                        	<th>Platform ID</th>
		                            <th>Platform Name</th>
		                            <th>Agency</th>
		                            <th>Transport Medium - ID</th>
		                            <th>Config</th>
		                            <th>Description</th>
		                        </tr>
		                    </thead>
		                    <tbody id="netlistTableBody">
		                        <!--This is where the data rows go-->
		                    </tbody>
		                </table>
	                </div>
	            </div>
	            <div class="modal-footer">
	                <button type="button" class="btn btn-secondary" id="cancelNetlistModalButton" data-bs-dismiss="modal">Cancel</button>
	                <button type="button" class="btn btn-success" id="saveNetlistModalButton">Save</button>
	            </div>
	        </div>
	    </div>
	</div>
	<!-- /netlist modal -->
	
</html>
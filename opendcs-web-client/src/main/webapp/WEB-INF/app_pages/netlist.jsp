<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
	<%@include file="/WEB-INF/common/header.jspf" %>
	<body class="navbar-top">
		<%@include file="/WEB-INF/common/top-bar.jspf" %>
	    <!-- Page content -->
	    <div class="page-content d-flex">
        	<%@include file="/WEB-INF/common/sidebar.jspf" %>
	        <div class="container-fluid flex-grow-1">
                <!-- Page header -->
				<div class="page-header page-header-light">
				    <div class="page-header-content header-elements-md-inline">
				        <div class="page-title d-flex">
				            <h4><i class="bi bi-arrow-left mr-2"></i>Netlists</h4>
				            <a href="#" class="header-elements-toggle text-default d-md-none"><i class="bi bi-three-dots-vertical"></i></a>
				        </div>
				    </div>  
				</div>
				<!-- /page header -->
	            <!-- Content area -->
	            <div class="content">
	                <!-- Netlist List Card -->
					<div class="card large-padding border-primary">
					    <div class="card-header header-elements-inline">
					    </div>
					    <table id="table" class="table table-hover table-striped datatable-responsive tablerow-cursor w-100">
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
				
	        </div>
	        <!-- /main content -->
	    </div>
		<%@include file="/WEB-INF/common/footer.jspf" %>
	    <!-- /page content -->
		<%@include file="/WEB-INF/common/scripts.jspf" %>
		<script src="../resources/js/netlist.js"></script>
		<%@include file="/WEB-INF/data_modals/netlist.jspf" %>
	</body>
	
	
</html>
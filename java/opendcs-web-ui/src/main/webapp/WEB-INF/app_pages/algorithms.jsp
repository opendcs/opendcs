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
	<%@include file="/WEB-INF/common/header.jspf" %>
	<body >
		<%@include file="/WEB-INF/common/top-bar.jspf" %>

	    <!-- Page content -->
	    <div class="page-content d-flex">
			<%@include file="/WEB-INF/common/sidebar.jspf" %>
	        
			<!-- Main content -->
			<div class="container-fluid flex-grow-1">
				<!-- Page header -->
				<div class="page-header page-header-light">
					<div class="page-header-content header-elements-md-inline">
						<div class="page-title d-flex">
							<h4>Algorithms</h4>
							<a href="#" class="header-elements-toggle text-default d-md-none"><i class="bi bi-three-dots-vertical"></i></a>
						</div>
					</div>  
				</div> 
				<!-- /page header -->
				<!-- Content area -->
				<div class="content">
					<!-- Basic responsive configuration -->
					<div class="card large-padding">
						<table id="mainTable" class="table table-hover table-striped datatable-responsive w-100 tablerow-cursor">
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
				
			</div>
			<!-- /main content -->
		</div>
		<%@include file="/WEB-INF/common/footer.jspf" %> 
		<%@include file="/WEB-INF/common/scripts.jspf" %> 
		<script src="/webjars/switchery/switchery.js"></script>
		<script src="/webjars/bootstrap-switch/js/bootstrap-switch.min.js"></script>
		<script src="../resources/js/lib/date_utilities.js"></script>
		<script src="../resources/js/algorithms.js"></script>
		<!-- /page content -->
		<%@include file="/WEB-INF/data_modals/algorithm_edit.jspf" %> 
	</body>
	
</html>
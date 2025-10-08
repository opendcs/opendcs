<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%-- ~ Copyright 2023 OpenDCS Consortium ~ ~ Licensed under the Apache License, Version 2.0 (the "License" ); ~ you
       may not use this file except in compliance with the License. ~ You may obtain a copy of the License at ~
       http://www.apache.org/licenses/LICENSE-2.0 ~ ~ Unless required by applicable law or agreed to in writing,
       software ~ distributed under the License is distributed on an "AS IS" BASIS, ~ WITHOUT WARRANTIES OR CONDITIONS
       OF ANY KIND, either express or implied. ~ See the License for the specific language governing permissions and ~
       limitations under the License. --%>


<%-- Main navbar --%>
<nav class="navbar navbar-expand-md navbar-dark bg-dark fixed-top">
	<div class="container-fluid">
		<!-- Brand -->
		<a href="dashboard" class="navbar-brand navbarMenuHome">OpenDCS</a>

		<!-- Mobile collapse button -->
		<button class="navbar-toggler" type="button"
				data-bs-toggle="collapse" data-bs-target="#navbarTop"
				aria-controls="navbarTop" aria-expanded="false" aria-label="Toggle navigation">
			<span class="navbar-toggler-icon"></span>
		</button>

		<!-- Collapsible area -->
		<div class="collapse navbar-collapse" id="navbarTop">
			<ul class="navbar-nav me-auto"></ul>
			<ul class="navbar-nav">
				<li class="nav-item dropdown" data-bs-display="static">
					<a class="nav-link dropdown-toggle d-flex align-items-center"
					   href="#" id="userDropdown" role="button"
					   data-bs-toggle="dropdown" aria-expanded="false">
						<img src="../resources/img/user_profile_image_large.png"
							 class="rounded-circle me-2" height="34" alt="">
						<span id="usernameDropdownText"></span>
					</a>
					<ul class="dropdown-menu dropdown-menu-end shadow" aria-labelledby="userDropdown">
						<li>
							<a href="#" class="dropdown-item">
								<i class="bi bi-person-plus me-2"></i>My profile
							</a>
						</li>
						<li><hr class="dropdown-divider"></li>
						<li>
							<a href="admin" class="dropdown-item" target="_blank">
								<i class="bi bi-crown me-2"></i>Admin
							</a>
						</li>
						<li>
							<a class="dropdown-item" id="logoutButton"
							   href="<c:url value='login'/>" role="button">
								<i class="bi bi-box-arrow-right me-2"></i>Logout
							</a>
						</li>
					</ul>
				</li>
			</ul>
		</div>
	</div>
</nav>

<script src="<c:url value='/resources/js/layout.js'/>"></script>
<script src="<c:url value='/resources/js/main.js'/>"></script>

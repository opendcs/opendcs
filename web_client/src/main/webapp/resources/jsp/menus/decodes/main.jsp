<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%-- Main navbar --%>
<div class="navbar navbar-expand-md navbar-dark fixed-top">
	<a href="dashboard" class="navbar-brand navbarMenuHome">OpenDCS</a>
	<ul class="navbar-nav mr-auto">

	</ul>


	<div class="d-md-none">
		<button class="navbar-toggler" type="button" data-toggle="collapse"
			data-target="#navbar-mobile">
			<i class="icon-tree5"></i>
		</button>
		<button class="navbar-toggler sidebar-mobile-main-toggle"
			type="button">
			<i class="icon-paragraph-justify3"></i>
		</button>
	</div>

	<div class="collapse navbar-collapse" id="navbar-mobile">

		<%--<span class="badge bg-success ml-md-3 mr-md-auto">Online</span>--%>
		<span class="badge ml-md-3 mr-md-auto"> </span>
		<ul class="navbar-nav">


			<li class="nav-item dropdown dropdown-user"><a href="#"
				class="navbar-nav-link d-flex align-items-center dropdown-toggle"
				data-toggle="dropdown"> <img
					src="../resources/img/limitless/placeholders/placeholder.jpg"
					class="rounded-circle mr-2" height="34" alt=""> <span
					id="usernameDropdownText"></span>
			</a>
				<div class="dropdown-menu dropdown-menu-right">
					<a href="#" class="dropdown-item"><i class="icon-user-plus"></i>
						My profile</a>
					<div class="dropdown-divider"></div>
					<a href="/admin" class="dropdown-item" target="_blank"><i
						class="icon-crown"></i> Admin</a> <a class="dropdown-item"
						id="logoutButton"><i class="icon-switch2"></i>Logout</a>
				</div></li>

		</ul>
	</div>
</div>
<%-- /main navbar --%>
<script src="../resources/js/main.js"></script>
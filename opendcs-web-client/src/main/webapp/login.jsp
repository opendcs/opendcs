<%@ page import="java.util.Objects" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">

	<%! String apiBasePath;// = getServletConfig().getServletContext().getInitParameter("api_base_path");
	 	String authType;// = getServletConfig().getServletContext().getInitParameter("authentication_type");
	 	String authBasePath;// = getServletConfig().getServletContext().getInitParameter("authentication_base_path");
	%>
	<%
		apiBasePath = getServletConfig().getServletContext().getInitParameter("api_base_path");
		authType = getServletConfig().getServletContext().getInitParameter("authentication_type");
		authBasePath = getServletConfig().getServletContext().getInitParameter("authentication_base_path");
	%>

	<%@include file="/WEB-INF/common/header.jspf" %>

	<body>

		<jsp:include page="/resources/jsp/menus/decodes/main.jsp" />   

   		<!-- Page content -->
   		<div class="page-content">

       		 
           
	        <!-- Main content -->
	        <div class="content-wrapper">
            
                <!-- Page header -->
				<div class="page-header page-header-light">
				    <div class="page-header-content header-elements-md-inline">
				        <div class="page-title d-flex">
				            <h4><i class="bi bi-arrow-left me-2"></i> <span class="font-weight-semibold">OpenDCS Web Client</span> - Login</h4>
				            <a href="#" class="header-elements-toggle text-default d-md-none"><i class="bi bi-three-dots-vertical"></i></a>
				        </div>
				    </div>  
				</div>
				<!-- /page header -->
            

	            <!-- Content area -->
	            <div class="content loginPageBackground">

					<div class="wrapper fadeInDown">
					  <div id="formContent" class="slightOpacity">
					    <!-- Tabs Titles -->
					    <h2>Login</h2>
					    <!-- Icon -->
					    <div class="fadeIn first">
					      <img src="../resources/img/user_profile_image_large.png" id="icon" alt="User Icon" />
					    </div>
						  	<% if (Objects.equals(authType, "sso") && authBasePath != null) { %>
						  	  <button class="btn btn-primary mb-2" id="ssoLoginButton">Login</button>
						  	<% } else { %>
								  <p>
									  <input type="text" name="username" class="form-control" placeholder="Username" maxlength="150" required id="id_username">
								  </p>
								  <p>
									  <input type="password" name="password" class="form-control" placeholder="Password" maxlength="150" required id="id_password">
								  </p>
						  		<button class="btn btn-primary mb-2" id="loginButton">Login</button>
						  	<% } %>
					    <!-- Remind Passowrd -->
					    <div id="formFooter">
					      <a class="underlineHover" href="#">Don't have an account?</a>
					    </div>
					  </div>
				</div>
			</div>
            <!-- /Content area -->
            
            <%@include file="/WEB-INF/common/footer.jspf" %>

        </div>
        <!-- /main content -->

    </div>
    <!-- /page content -->
	<%@include file="/WEB-INF/common/scripts.jspf" %>
	<script src="../resources/js/login.js"></script>
<link rel="stylesheet" type="text/css" href="../resources/css/login-form.css">
</body>
<!-- /theme JS files -->
	<% if (Objects.equals(authType, "sso") && authBasePath != null) { %>
		<script>
			$("#ssoLoginButton").on("click", function(e) {
				var returnUrl = window.location.href.substring(0, window.location.href.lastIndexOf("/")) + "/platforms";
				window.location = `/<%=authBasePath%>?OriginalLocation=\${returnUrl}`;
			});
		</script>
	<% } %>

</html>

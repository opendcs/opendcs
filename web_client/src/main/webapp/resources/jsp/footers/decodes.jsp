<%-- Footer --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<div class="navbar navbar-expand-lg navbar-light">
	<div class="text-center d-lg-none w-100">
		<button type="button" class="navbar-toggler dropdown-toggle"
			data-toggle="collapse" data-target="#navbar-footer">
			<i class="icon-unfold mr-2"></i> Footer
		</button>
	</div>

	<div class="navbar-collapse collapse" id="navbar-footer">
		<span class="navbar-text"><a href="#">OpenDCS Web</a> </span>

		<ul class="navbar-nav ml-lg-auto">

			<li class="nav-item"><a
				href="<% String swaggerUrl =  (String) request.getAttribute("api_swaggerui_url"); %><%= swaggerUrl %>"
				class="nav-link" target="_blank"> <i class="icon-code"></i> <span>
						API Swagger UI </span>
			</a>
		</ul>
	</div>
</div>
<%-- /footer --%>
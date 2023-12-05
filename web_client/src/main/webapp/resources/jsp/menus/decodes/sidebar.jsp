<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%-- Main sidebar --%>
<div
	class="sidebar sidebar-dark sidebar-main sidebar-expand-md sidebar-fixed">

	<%-- Sidebar mobile toggler --%>
	<div class="sidebar-mobile-toggler text-center">
		<a href="#" class="sidebar-mobile-main-toggle"> <i
			class="icon-arrow-left8"></i>
		</a> Navigation <a href="#" class="sidebar-mobile-expand"> <i
			class="icon-screen-full"></i> <i class="icon-screen-normal"></i>
		</a>
	</div>
	<%-- /sidebar mobile toggler --%>


	<%-- Sidebar content --%>
	<div class="sidebar-content">
		<a href="#"
			class="navbar-nav-link sidebar-control sidebar-main-toggle d-none d-md-block">
			<i class="icon-paragraph-justify3 floatRight"></i>
		</a>
		<%-- Main navigation --%>
		<div class="card card-sidebar-mobile">
			<ul class="nav nav-sidebar" data-nav-type="accordion">
				<%-- Main --%>
				
				<li class="nav-item">
					
				</li>

				<li
					class="nav-item nav-item-submenu <% if (request.getAttribute("tool") == "decodes") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
					<a href="#" class="nav-link"><i class="icon-copy"></i> <span>Decodes
							Database Editor</span></a>
					<ul class="nav nav-group-sub" data-submenu-title="rl_edit"
						style="<% if (request.getAttribute("tool") == "decodes") { %> display:block; <% } else { %> display:none; <% } %>">
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "platforms") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
							<a href="platforms" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Platforms</span>
						</a>
						</li>
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "sites") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
							<a href="sites" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Sites</span>
						</a>
						</li>
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "configs") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
							<a href="configs" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Configs</span>
						</a>
						</li>
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "presentation") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
							<a href="presentation" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Presentation</span>
						</a>
						</li>
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "routing") { %> nav-item-open <% } else { %> nav-item-closed <% } %>d">
							<a href="routing" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Routing</span>
						</a>
						</li>
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "sources") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
							<a href="sources" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Sources</span>
						</a>
						</li>
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "netlist") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
							<a href="netlist" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Netlists</span>
						</a>
						</li>
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "schedule entry") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
							<a href="schedule" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Schedule Entry</span>
						</a>
						</li>
					</ul>
				</li>
				<li
					class="nav-item nav-item-submenu <% if (request.getAttribute("tool") == "computations") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
					<a href="#" class="nav-link"><i class="icon-copy"></i> <span>Computations</span></a>
					<ul class="nav nav-group-sub" data-submenu-title="rl_edit"
						style="<% if (request.getAttribute("tool") == "computations") { %> display:block; <% } else { %> display:none; <% } %>">
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "algorithms") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
							<a href="algorithms" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Algorithms</span>
						</a>
						</li>
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "computations") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
							<a href="computations" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Computations</span>
						</a>
						</li>
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "processes") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
							<a href="processes" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Processes</span>
						</a>
						</li>
					</ul>
				</li>
				<li
					class="nav-item nav-item-submenu <% if (request.getAttribute("tool") == "rledit") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
					<a href="#" class="nav-link"><i class="icon-copy"></i> <span>Reflist
							Editor</span></a>
					<ul class="nav nav-group-sub" data-submenu-title="rl_edit"
						style="<% if (request.getAttribute("tool") == "rledit") { %> display:block; <% } else { %> display:none; <% } %>">
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "enumerations") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
							<a href="enumerations" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Enumerations</span>
						</a>
						</li>
						<li
							class="nav-item nav-item-submenu <% if (request.getAttribute("page") == "engineering_units") { %> nav-item-open <% } else { %> nav-item-closed <% } %>">
							<a href="engineering_units" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Engineering
									Units</span>
						</a>
						</li>
						
						<li
							class="nav-item nav-item-submenu <%if (request.getAttribute("page") == "eu_conversions") {%> nav-item-open <%} else {%> nav-item-closed <%}%>">
							<a href="eu_conversions" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>EU Conversions</span>
						</a>
						</li>
						<li
							class="nav-item nav-item-submenu <%if (request.getAttribute("page") == "seasons") {%> nav-item-open <%} else {%> nav-item-closed <%}%>">
							<a href="seasons" class="nav-link"> <i
								class="icon-file-presentation"></i> <span>Seasons</span>
						</a>
						</li>
					</ul>
				</li>

			</ul>
		</div>
		<%-- /main navigation --%>

	</div>
	<%-- /sidebar content --%>

</div>
<%-- /main sidebar --%>
<script src="../resources/js/limitless/app.js"></script>
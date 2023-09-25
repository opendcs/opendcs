<%-- notification modal --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<div id="notificationModal" class="modal fade" tabindex="-1"
	data-backdrop="static" data-keyboard="false">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header" id="notificationModalTitleDiv">
				<h6 class="modal-title" id="notificationModalTitle"></h6>
				<%--<button type="button" class="close" data-dismiss="modal">&times;</button>--%>
			</div>

			<div class="modal-body">
				<h6 class="font-weight-semibold" id="notificationModalTextTitle"></h6>
				<p id="notificationModalText"></p>
			</div>

			<div class="modal-footer">
				<button type="button" data-dismiss="modal" class="btn">OK</button>
			</div>
		</div>
	</div>
</div>
<script src="../resources/js/modals/notification.js"></script>
<%-- /alert modal --%>

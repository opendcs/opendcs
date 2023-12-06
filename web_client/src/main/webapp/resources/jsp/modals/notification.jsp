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

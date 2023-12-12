<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
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

<%-- Yesno modal --%>
<div id="yesNoModal" class="modal fade" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header" id="yesNoModalTitleDiv">
                <h6 class="modal-title" id="yesNoModalTitle"></h6>
                <%--<button type="button" class="close" data-dismiss="modal">&times;</button>--%>
            </div>

            <div class="modal-body">
                <h6 class="font-weight-semibold" id="yesNoModalTextTitle"></h6>
                <p id="yesNoModalText"></p>
            </div>

            <div class="modal-footer">
                <button type="button" data-dismiss="modal" class="btn"
                    id="modal_yesno_no_button">No</button>
                <button type="button" data-dismiss="modal" class="btn"
                    id="modal_yesno_yes_button">Yes</button>
            </div>
        </div>
    </div>
</div>
<%-- /yesno modal --%>
<script src="../resources/js/modals/yesno.js"></script>
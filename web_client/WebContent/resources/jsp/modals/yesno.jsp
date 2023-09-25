<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
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
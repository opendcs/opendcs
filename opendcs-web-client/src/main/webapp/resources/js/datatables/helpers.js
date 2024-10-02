/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

var dragDatatable = {
        "source": {},
        "destination": {}
};
var propSpecsMeta = {
        "seasons": {},
        "transportMediumTypes": {}
}

document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded helpers.js.");

    //deprecated event.path, so adding it back in.
    //This is for the maketableinline function - var ignoreFocusOut = event.path[0].dataset["ignorefocusout"];
    if (!Event.prototype.hasOwnProperty('path')) {
        Object.defineProperty(Event.prototype, 'path', {
            get() { return this.composedPath(); }
        });
    }

    //Resizes datatables when the window is resized.  DataTables don't resize well on their own.
    $( window ).on( "resize", function() {
        var allTables = $("[resize_on_window_resize]");
        for (var x = 0; x < allTables.length; x++)
        {
            var curTable = $(allTables[x]);
            var tableId = curTable.attr("id");
            if (tableId != null)
            {
                updateDataTableScroll(tableId);
            }
        }
    });

    setPropSpecsMeta();
});


/**
 * 
 * @param applicableActions {object} Actions to override the default ones.
 *                                   See example below. 
 *           example [
 *                     {
 *                         "type": "delete",
 *                         "onclick": function() {console.log("clicked.");} //null means use default.
 *                     }
 *                   ]
 * @returns
 */
function createActionDropdown(applicableActions)
{
    var actionRefs = {
            "delete": {
                "target": "<!-- DELETE_HTML -->",
                "html": '<a <!-- ONCLICK --> class="dropdown-item" data-ignorefocusout="true" action_type="delete_row"><i class="icon-cancel-circle2"></i> Delete</a>',
                "default_onclick": "deleteRow_default(event, this)"
            },
            "copy": {
                "target": "<!-- COPY_HTML -->",
                "html": '<a <!-- ONCLICK --> class="dropdown-item" data-ignorefocusout="true" action_type="copy_row"><i class="icon-cancel-circle2"></i> Copy</a>',
                "default_onclick": "copyRow_default(event, this)"
            }
    };

    var actionDropdownHtml = '<div class="list-icons float-right">'
        + '<div class="dropdown">'
        + '    <a href="javascript:void(0)" class="list-icons-item" data-toggle="dropdown" onclick="clickedDropdown(event, this)" data-ignorefocusout="true">'
        + '    <i class="icon-menu9"></i>'
        + '</a>'
        + '<div class="dropdown-menu dropdown-menu-right">'
        + "<!-- COPY_HTML -->"
        + "<!-- DELETE_HTML -->"
        + '</div>'
        + '</div>'
        + '</div>';

    applicableActions.forEach(applicAction => {

        var type = applicAction["type"];
        var onclick = applicAction["onclick"] != null ? applicAction["onclick"] : actionRefs[type]["default_onclick"];
        actionDropdownHtml = actionDropdownHtml.replace(actionRefs[type]["target"], actionRefs[type]["html"].replace("<!-- ONCLICK -->", `onclick='${onclick}'`));
    });

    return actionDropdownHtml;
}

function setPropSpecsMeta()
{
    //seasonMeta
    var params = {};
    $.ajax({
        url: `${window.API_URL}/seasons`,
        type: "GET",
        data: params,
        success: function(response) {
            propSpecsMeta["seasons"] = response;
        },
        error: function(response) {
            console.log("Error getting seasons.");
        }
    });
    params = {
            "name": "TransportMediumType"
    };
    $.ajax({
        url: `${window.API_URL}/reflists`,
        type: "GET",
        data: params,
        success: function(response) {
            propSpecsMeta["transportMediumTypes"] = response["TransportMediumType"].items;
        },
        error: function(response) {
            console.log("Error getting seasons.");
        }
    });
}

//You can pass the jquery table, or the ID of the table, starting with # or not.
//Pass action as null if you don't want an action row.  Otherwise, use the regular format
//actions = [{
//"type": "delete",
//"onclick": null
//}];
function addBlankRowToDataTable(targetTable, redraw, action, inlineOptions, dragIcon)
{
    if (typeof(targetTable) == "string" && !targetTable.startsWith("#"))
    {
        targetTable = "#" + targetTable;
    }
    var targetDataTable = $(targetTable).DataTable();
    var numCols = targetDataTable.columns().nodes().length;
    var emptyRow = new Array(numCols);
    emptyRow.fill("");
    if (action != null)
    {
        emptyRow[numCols-1] = createActionDropdown(action);
    }
    if (dragIcon != null)
    {
        emptyRow[0] = '<i class="move-cursor icon-arrow-resize8 mr-3 icon-1x"></i>';
    }
    targetDataTable.row.add(emptyRow);
    if (redraw)
    {
        targetDataTable.draw();
    }
    if (inlineOptions != null)
    {
        var targetTableId = "";
        if (typeof(targetTable) == "object")
        {
            targetTableId = targetTable.attr("id");
        }
        else
        {
            targetTableId = targetTable.replace("#", "");
        }
        makeTableInline(targetTableId, inlineOptions);
    }
}

function clickedDropdown(event, clickedDropdown)
{
    var curDisplay = $(clickedDropdown).next(".dropdown-menu").css("display");
    $(".dropdown-menu").css("display", ""); //Closes all of the dropdowns that are currently open.
    if (curDisplay != "block") //this will only reopen it if it wasn't open to begin with.
    {
        $(clickedDropdown).next(".dropdown-menu").toggle();
    }
    event.stopPropagation();
    //This scrolls down so the dropdown will be fully visible in the case that
    //it forces the scroll to move down.
    event.target.scrollIntoView();
}

function clickedDropdown_deprecated(event, clickedDropdown)
{
    $(clickedDropdown).next(".dropdown-menu").toggle();
    event.stopPropagation();
}

function copyRow_default(event, clickedLink)
{
    console.log("Copying row with default function.");
}

function deleteRow_default(event, clickedLink)
{
    var clickedRow = clickedLink.closest("tr");
    var clickedTable = clickedRow.closest("table");
    var clickedDataTable = $(clickedTable).DataTable();
    toggleRowMarkedForDeletion(clickedRow);
    $(clickedLink).closest(".dropdown-menu").css("display", "");
    event.stopPropagation();
}

function deleteOpendcsObject_default(event, clickedLink, params)
{
    $(clickedLink).closest(".dropdown-menu").toggle();

    var objectType = params.objectType;
    var objectTypeDisplayName = params.objectTypeDisplayName;
    var objectIdIndex = params.objectIdIndex;
    var objectNameIndex = params.objectNameIndex;
    var urlIdName = params.urlIdName;

    var jqueryTable = $(clickedLink.closest("table"));
    var dTable = jqueryTable.DataTable();

    var rowData = dTable.row(clickedLink.closest("tr")).data();
    var objectId = rowData[objectIdIndex];
    var objectName = rowData[objectNameIndex];
    set_yesno_modal(`Delete ${objectTypeDisplayName}`, 
            `Delete ${objectName} ${objectTypeDisplayName}`, 
            `Are you sure you want to delete the ${objectName} ${objectTypeDisplayName}?`, 
            "bg-warning", 
            function() {
        var url = `${window.API_URL}/${objectType}`;

        if (objectId != "")
        {
            url += `&${urlIdName}=${objectId}`;
        }
        else
        {
            show_notification_modal(`Delete ${objectTypeDisplayName}`, 
                    `This is not a saved ${objectTypeDisplayName}.`, 
                    `You cannot delete this ${objectTypeDisplayName}, as it has not been saved yet.`, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    function() {
                hide_notification_modal();
            }
            );
            return;
        }

        $.ajax({
            url: url,
            type: "DELETE",
            headers: {     
                "Content-Type": "application/json"   
            },
            dataType: "text",
            data: {},
            success: function(response) {
                hide_waiting_modal(500);
                show_notification_modal(`Delete ${objectTypeDisplayName}`, 
                        `${objectTypeDisplayName} Deleted Successfully`, 
                        `${response}`, 
                        "OK", 
                        "bg-success", 
                        "bg-secondary",
                        function() {
                    hide_notification_modal();
                    location.reload();
                }
                );
            },
            error: function(response) {
                show_notification_modal(`Delete ${objectTypeDisplayName}`, 
                        `There was an error deleting ${objectTypeDisplayName}.`, 
                        response.responseText, 
                        "OK", 
                        "bg-danger", 
                        "bg-secondary",
                        function() {
                    hide_notification_modal();
                    location.reload();
                }
                );
            }
        });
        hide_yesno_modal();
    }, 
    "bg-warning", 
    null, 
    "bg-secondary");
    show_yesno_modal();
    event.stopPropagation();
}

function toggleRowMarkedForDeletion(jqRow)
{
    var curDeleteValue = $(jqRow).attr("delete");
    //Mark for deletion
    if (curDeleteValue == null || curDeleteValue.toLowerCase() != "true")
    {
        setRowMarkedForDeletion(jqRow, true);
    }
    //Remove mark for deletion
    else
    {
        setRowMarkedForDeletion(jqRow, false);
    }
}


function setRowMarkedForDeletion(jqRow, newValue)
{
    //Mark for deletion
    if (newValue)
    {
        $(jqRow).addClass("rowToBeDeleted");
        $(jqRow).attr("delete", "true");
        $(jqRow).find("[action_type=delete_row]").get(0).childNodes[1].nodeValue = " Undelete";
    }
    //Remove mark for deletion
    else
    {
        $(jqRow).removeClass("rowToBeDeleted");
        $(jqRow).attr("delete", "");
        $(jqRow).find("[action_type=delete_row]").get(0).childNodes[1].nodeValue = " Delete";
    }
}


function getNonDeletedRowData(table)
{
    var jqTable;
    if (typeof(table) == "string")
    {
        if (!table.startsWith("#"))
        {
            table = "#" + table;
        }
    }
    var jqTable = $(table);
    var dTable = jqTable.DataTable();

    var data = dTable.data();
    var returnRows = [];
    for (var x = 0; x < data.length; x++)
    {
        var deleteAttr = $(dTable.row(x).node()).attr("delete");
        if (deleteAttr == null || deleteAttr.toLowerCase() != "true")
        {
            returnRows.push(data[x]);
        }
    }
    return returnRows;
}

function addElementToDataTableHeader(datatableId, element)
{
    var targetSelector = "#" + datatableId + "_wrapper .dataTables_filter";
    element.appendTo(targetSelector);
    $(targetSelector).addClass("w-60");
    $("#" + datatableId + "_wrapper" + " .dataTables_filter label").addClass("float-left");
}

function makeTableInline(tableId, inlineOptions, runAtEndClick, runAtEndFocusOut)
{
    var jqTableId = tableId;
    if (!jqTableId.startsWith("#"))
    {
        jqTableId = "#" + jqTableId;
    }
    $(jqTableId + ' tbody tr td').off('click');
    $(jqTableId + ' tbody tr td').on('click', function() {

        var clickedWidth = $(this).width();

        var clickable = $(this).attr("clickable");
        if (clickable != null && clickable.toLowerCase() == "false")
        {
            //Cell not clickable, returning early.
            return;
        }
        var targetTable = $(jqTableId).DataTable();
        var clickedCell = targetTable.cell(this);
        if (clickedCell.length > 0)
        {
            var colNumOld = clickedCell[0][0].column;
            var rowNum = clickedCell[0][0].row;
            
            var dtColNum = targetTable.column(this).index(); //This is the col num including hidden columns.
            var colNum = getRealColumnIndex(this); //This is to take into account invisible columns;
            
            if (!$(jqTableId).hasClass("editing")) {
                //var data = scheduleTable.row(row).data();
                var data = targetTable.row(rowNum).data()
                var $row = $(this.parentElement);
                var thisPosition = $row.find("td:nth-child(" + (colNum + 1).toString() + ")");
                var thisPositionText = thisPosition.text();


                var inputId = "Position_" + rowNum.toString() + "_" + colNum.toString();
                var inputId2 = "Position2_" + rowNum.toString() + "_" + colNum.toString();
                var inputId3 = "Position3_" + rowNum.toString() + "_" + colNum.toString();

                var forcedParam = $(this).attr("forced_propspec_type");
                if (forcedParam != null)
                {
                    $(jqTableId).addClass("editing");
                    var splitForcedParam = forcedParam.split(":");
                    if (splitForcedParam.length > 1)
                    {
                        forcedParam = splitForcedParam[0];
                    }
                    if (forcedParam == "b")
                    {
                        var curVal = this.textContent.toLowerCase();
                        var selectHtml = `<select style="width:${clickedWidth}" id="${inputId}">`;
                        ["", "true", "false"].forEach(d => {
                            var selectedFlag = "";
                            if (curVal == d.toLowerCase())
                            {
                                selectedFlag = 'selected';
                            }
                            selectHtml += `<option value="${d}" ${selectedFlag}>${d}</option>`;
                        })
                        selectHtml += "</select>";
                        thisPosition.empty().append($(selectHtml));
                    }
                    else if (forcedParam == "e" && splitForcedParam.length > 1)
                    {
                        var curVal = this.textContent.toLowerCase();
                        var selectType = splitForcedParam[1].toLowerCase();
                        if (selectType == "season")
                        {
                            var sn = propSpecsMeta["seasons"].map(s => s.abbr);
                            var selectHtml = `<select id="${inputId}"><option></option>`;
                            sn.forEach(s => {
                                var selectedFlag = "";
                                if (curVal == s.toLowerCase())
                                {
                                    selectedFlag = 'selected';
                                }
                                selectHtml += `<option value="${s}" ${selectedFlag}>${s}</option>`;
                            })
                            selectHtml += "</select>";
                            thisPosition.empty().append($(selectHtml));
                        }
                        else if (selectType == "transportmediumtype")
                        {
                            var curVal = this.textContent.toLowerCase();
                            var tmTypes = Object.keys(propSpecsMeta["transportMediumTypes"]);
                            var selectHtml = `<select id="${inputId}"><option></option>`;
                            tmTypes.forEach(tmt => {
                                var selectedFlag = "";
                                if (curVal == tmt.toLowerCase())
                                {
                                    selectedFlag = 'selected';
                                }
                                selectHtml += `<option value="${tmt}" ${selectedFlag}>${tmt}</option>`;
                            })
                            selectHtml += "</select>";
                            thisPosition.empty().append($(selectHtml));
                        }

                    }
                    else if (forcedParam == "i")
                    {
                        thisPosition.empty().append($("<input></input>", {
                            "id": inputId,
                            "type": "number"
                        }));
                        $("#" + inputId).val(thisPositionText);
                    }
                    else if (forcedParam == "s" || forcedParam == "h" || forcedParam == "n" || forcedParam == "f" || forcedParam == "f")
                    {
                        thisPosition.empty().append($("<input></input>", {
                            "id": inputId
                        }));
                        $("#" + inputId).val(thisPositionText);
                    }
                }
                else
                {
                    for (var x = 0; x < inlineOptions.columnDefs.length; x++)
                    {
                        var colOptions = inlineOptions.columnDefs[x];
                        if (colOptions.targets.indexOf(dtColNum) != -1)
                        {
                            $(jqTableId).addClass("editing");
                            if (colOptions.type == "input")
                            {
                                thisPosition.empty().append($("<input></input>", {
                                    "id": inputId
                                }));
                                $("#" + inputId).val(thisPositionText);
                                break;
                            }
                            else if (colOptions.type == "number")
                            {
                                thisPosition.empty().append($("<input></input>", {
                                    "id": inputId,
                                    "type": "number"
                                }));
                                $("#" + inputId).val(thisPositionText);
                                break;
                            }
                            else if (colOptions.type == "textarea")
                            {
                                thisPosition.empty().append($('<div class="form-group">' +
                                        '<textarea class="form-control" id="' + inputId + '"></textarea>' +
                                '</div>'));
                                $("#" + inputId).val(thisPositionText);
                                break;
                            }
                            else if (colOptions.type == "select")
                            {
                                var curVal = this.textContent.toLowerCase();
                                var selectHtml = `<select id="${inputId}">`;
                                colOptions.data.forEach(d => {
                                    var selectedFlag = "";
                                    if (curVal == d.toLowerCase())
                                    {
                                        selectedFlag = 'selected';
                                    }
                                    selectHtml += `<option value="${d}" ${selectedFlag}>${d}</option>`;
                                })
                                selectHtml += "</select>";

                                //TODO: Need to sanitize for code injection.
                                thisPosition.empty().append($(selectHtml));
                                break;
                            }
                            else if (colOptions.type == "searchable_select")
                            {
                                var optionGroupHtml = "";
                                for (var x = 0; x < colOptions.data.length; x++)
                                {
                                    var fullName = colOptions.data[x];
                                    var splitName = fullName.split("-");
                                    var baseName = `${splitName[0]}-*`;
                                    var subName = splitName.length > 1 ? `*-${splitName[1]}` : null;
                                    var optionHtml = `<optgroup label="${fullName}">
                                        <option value="${fullName}">${fullName}</option>
                                        <option value="${baseName}">${baseName}</option>`
                                        optionHtml += subName != null ? `<option value=${subName}>${subName}</option>` : "";
                                    optionHtml += `</optgroup>`;
                                    optionGroupHtml += optionHtml;
                                }

                                var objHtml = `
                                    <div data-type="special_select" id="${inputId}">
                                    <select class="form-control select-search" data-fouc>

                                    ${optionGroupHtml}

                                    </select>
                                    </div>`;
                                thisPosition.empty().append($(objHtml));
                                $(`#${inputId}`).find("select").select2({
                                    matcher(params, data) {
                                        const originalMatcher = $.fn.select2.defaults.defaults.matcher;
                                        const result = originalMatcher(params, data);

                                        if (
                                                result &&
                                                data.children &&
                                                result.children &&
                                                data.children.length
                                        ) {
                                            if (
                                                    data.children.length !== result.children.length &&
                                                    data.text.toLowerCase().includes(params.term.toLowerCase())
                                            ) {
                                                result.children = data.children;
                                            }
                                            return result;
                                        }

                                        return null;
                                    },
                                });
                                $(`#${inputId}`).find("select").val(thisPositionText).trigger("change");
                                $(`#${inputId}`).closest("div").children().on("mousedown", function(e) {
                                    var select = $(this).closest("div");
                                    event.preventDefault();
                                    var curTs = new Date();
                                    var tsString = curTs.toISOString();
                                    select.get(0).dataset["time_clicked"] = tsString;
                                });
                                break;
                            }
                            else if (colOptions.type == "editable_select")
                            {
                                var curVal = this.textContent.toLowerCase();
                                var selectHtml = `<div data-type="special_select" id="${inputId}" class="select-editable"><select id="${inputId3}" onchange="this.nextElementSibling.value=this.value">`;
                                colOptions.data.forEach(d => {
                                    var selectedFlag = "";
                                    if (curVal == d.toLowerCase())
                                    {
                                        selectedFlag = 'selected';
                                    }
                                    selectHtml += `<option value="${d}" ${selectedFlag}>${d}</option>`;
                                })
                                selectHtml += `</select>
                                    <input id="${inputId2}" type="text" name="format" value="" />
                                    </div>`;
                                thisPosition.empty().append($(selectHtml));



                                var actualInput = $("#" + inputId2);
                                $(thisPosition.find("select")).trigger("change");
                                $(thisPosition.find("select")).focus();

                                var selectInput = $("#" + inputId3);

                                actualInput.val(thisPositionText);
                                actualInput.on("mousedown", function(e) {
                                    var curTs = new Date();
                                    var tsString = curTs.toISOString();
                                    $(this).closest("div")[0].dataset["time_clicked"] = tsString;
                                });
                                selectInput.on("mousedown", function(e) {
                                    var curTs = new Date();
                                    var tsString = curTs.toISOString();
                                    $(this).closest("div")[0].dataset["time_clicked"] = tsString;
                                });
                                break;
                            }
                            else
                            {
                                $('#' + tableId).removeClass("editing");
                                break;
                            }
                        }
                    }
                }

                if (colOptions != null && colOptions.type != "searchable_select")
                {
                    $("#" + inputId).focus();
                    $("#" + inputId).find("select").focus(); //This is to focus on the editable_select option.
                    $("#" + inputId).select();
                    $("#" + inputId).keypress(function(event) {
                        var keycode = (event.keyCode ? event.keyCode : event.which);
                        if(keycode == '13'){
                            this.blur();
                            $("#" + this.id).trigger("focusout");
                        }
                    });
                }
                $("#" + inputId).data("prev_val", thisPositionText);


            }

        }

        if (runAtEndClick != null)
        {
            runAtEndClick(this);
        }
    });

    $(jqTableId + ' tbody tr td').off('focusout');
    $(jqTableId + ' tbody tr td').on("focusout", function(e) {
        var targetTable = $(jqTableId).DataTable();
        var visibleColumns = targetTable.columns().visible();
        var clickedCell = targetTable.cell(this);
        var ignoreFocusOut = event.path[0].dataset["ignorefocusout"];

        var editableSelectDiv = $(this).find("[data-type=special_select]");
        var editableInput = null;
        var keepFocus = false;
        if (editableSelectDiv.length > 0)
        {
            var timeClickedString = editableSelectDiv.data("time_clicked");
            if (timeClickedString != null)
            {
                var timeClicked = new Date(timeClickedString);
                var curDateTs = new Date();
                if ((curDateTs - timeClicked) <= 250) //If you this check is performed within 0.140 seconds of the click on the text box, it will keep the box open.
                {
                    keepFocus = true;
                }
            }
            
        }
        if ((ignoreFocusOut != null && ignoreFocusOut.toLowerCase() == "true") || keepFocus)
        {
            return
        }
        if (clickedCell.length > 0)
        {
            var colNumOld = clickedCell[0][0].column;
            var rowNum = clickedCell[0][0].row;
            
            var dtColNum = targetTable.column(this).index(); //This is the col num including hidden columns.
            var colNum = getRealColumnIndex(this); //This is to take into account invisible columns;
            
            var inputId = "Position_" + rowNum.toString() + "_" + colNum.toString();
            var thisVal = $("#" + inputId).val();
            if (editableSelectDiv.length > 0)
            {
                var selectSearchVal = editableSelectDiv.find(".select-search").val();
                var editSearchVal = editableSelectDiv.find("input").val();
                thisVal = selectSearchVal != null ? selectSearchVal : editSearchVal; 
            }
            var previousVal = $("#" + inputId).data("prev_val");
            var $this = $(this);
            var $thisCell = targetTable.cell($this);
            var tempData = targetTable.row($this.closest("tr")).data().slice();
            tempData[dtColNum] = thisVal;
            targetTable.row($this.closest("tr")).data(tempData);
            /**
             * insted of updateing dom via jQuery use DataTable's Method to update dom object and 
             * also not forget to draw() your changes.
             *
             * $this.parent("td").empty().text($this.val());
             *
             **/
            $thisCell.data(thisVal).draw();
            for (var x = 0; x < inlineOptions.columnDefs.length; x++)
            {
                var def = inlineOptions.columnDefs[x];
                if (def.targets.indexOf(dtColNum) != -1)
                {
                    if (previousVal != thisVal)
                    {
                        if ("bgcolor" in def && "change" in def["bgcolor"] && def["bgcolor"]["change"] != null)
                        {
                            var bgColor = def["bgcolor"]["change"];
                            $thisCell.node().style.backgroundColor = bgColor;                              
                        }
                        $thisCell.node().dataset["modified"] = true;
                    }
                }
            }
            $(jqTableId).removeClass("editing");
        }
        if (runAtEndFocusOut != null)
        {
            runAtEndFocusOut(this);
        }
    });
}

function getRealColumnIndex(cell)
{
    var targetTable = $(cell).closest("table").DataTable();
    var visibleColumns = targetTable.columns().visible();
    //This is the col num including hidden columns.
    var dtColNum = targetTable.column(cell).index(); 
    var colNum = dtColNum;
    for (var x = 0; x < dtColNum; x++)
    {
        if (!visibleColumns[x])
        {
            colNum--;
        }
    }
    return colNum;
}

function getDataTableScrollHeight(targetTable, spacingBottomPercentage)
{
    if (typeof(targetTable) == "string" && !targetTable.startsWith("#"))
    {
        targetTable = "#" + targetTable;
    }
    var tt = $(targetTable);
    var cardHeight = tt.closest(".card").height();
    var spacingBottom = cardHeight * spacingBottomPercentage/100;
    var ttWrapper = tt.closest("#" + tt.attr("id") + "_wrapper");
    var tableHeight = tt.height();
    var tableHeaderHeight = ttWrapper.find(".dataTables_scrollHead").height();
    var tableButtonsHeight = ttWrapper.find(".dt-buttons").length > 0 ? ttWrapper.find(".dt-buttons").height() : 0;
    
    var scrollHeight = cardHeight - tableHeaderHeight - tableButtonsHeight - spacingBottom;
    return scrollHeight;
}

function updateDataTableScroll_deprecated(targetTable, spacingBottomPercentage)
{

    if (typeof(targetTable) == "string" && !targetTable.startsWith("#"))
    {
        targetTable = "#" + targetTable;
    }
    var tt = $(targetTable);
    var dt = tt.DataTable();
    dt.draw();
    var scrollY = getDataTableScrollHeight(tt, 10);
    tt.closest("#" + tt.attr("id") + "_wrapper")
    $("#" + tt.attr("id") + "_wrapper .dataTables_scrollBody").css({"max-height": scrollY + "px", "height": scrollY + "px"});
    dt.draw();
}

function getDatatableHeights(targetTable)
{
    var heights = {}
    if (typeof(targetTable) == "string" && !targetTable.startsWith("#"))
    {
        targetTable = "#" + targetTable;
    }
    var tt = $(targetTable);
    var dt = tt.DataTable();
    dt.draw();

}
function getDatatableObjects(targetTable)
{
    if (typeof(targetTable) == "string" && !targetTable.startsWith("#"))
    {
        targetTable = "#" + targetTable;
    }
    var jqTable = $(targetTable);
    var dt = jqTable.DataTable();
    dt.draw();

    var wrapper = jqTable.closest(".dataTables_wrapper");

    var parentDiv = wrapper.parent();
    var buttons = wrapper.find(".dt-buttons");
    var header = wrapper.find(".dataTables_scrollHead");
    var body = wrapper.find(".dataTables_scrollBody");
    var footer = wrapper.find(".dataTables_scrollFoot");

    var objects = {
            "parent_div": parentDiv,
            "wrapper": wrapper,
            "buttons": buttons,
            "header": header,
            "body": body,
            "footer": footer
    }
    return objects;
}

function updateDataTableScroll(targetTable, fillPercentage)
{

    if (typeof(targetTable) == "string" && !targetTable.startsWith("#"))
    {
        targetTable = "#" + targetTable;
    }
    var jqTable = $(targetTable);
    var dt = jqTable.DataTable();
    dt.draw();

    var jqWrapper = jqTable.closest(".dataTables_wrapper");
    var wrapperChildren = jqWrapper.children();

    var wrapperParent = jqWrapper.parent();
    var dtScrollBody = null;
    var allObjects = [];
    var totalOuterHeight = 0;
    
    //Trying to separate out all dom objects other than the scroll body.  This
    //way, we can update the size of the scroll body based on the size/resize
    //of the window.
    for (var x = 0; x < wrapperChildren.length; x++)
	{
    	var curChild = $(wrapperChildren[x]);
    	if (curChild.hasClass("dataTables_scroll"))
		{
    		var scrollChildren = curChild.children();
    		for (var y = 0; y < scrollChildren.length;  y++)
			{
    			var curScrollChild = $(scrollChildren[y]);
    			if (curScrollChild.hasClass("dataTables_scrollBody"))
				{
    				dtScrollBody = curScrollChild;
				}
    			else
				{
    				//add the non scroll body to the all other objects.
    	    		allObjects.push(curScrollChild);
        			totalOuterHeight += curScrollChild.outerHeight();
				}
			}
    		
		}
    	else
		{
    		//add the non scroll body to the all other objects.
    		totalOuterHeight += curChild.outerHeight();
    		allObjects.push(curChild);
		}
	}
    
    if (dtScrollBody == null)
	{
    	console.log("No scroll body for table.  Exiting now.");
    	return;
	}
    
    //var parentHeight = wrapperParent.outerHeight();
    //This is height instead of outerHeight to account for margins and padding.
    var parentHeight = wrapperParent.height(); 
    var dtScrollBodyHeight = dtScrollBody.height();
    var dtScrollBodyOuterHeight = dtScrollBody.outerHeight();
    var dtScrollBodyDiff = dtScrollBodyOuterHeight - dtScrollBodyHeight;
    var sbNewHeight = parentHeight-totalOuterHeight;
    
    if (jqTable.attr("id") == "propertiesTable")
	{
    	console.log("*********PropertiesTable**************");
        console.log("ParentHeight: " + parentHeight);
        console.log("TotalOuterHeight: " + totalOuterHeight);
    	console.log("*********PropertiesTable**************");
        
	}
    if (totalOuterHeight > parentHeight)
	{
    	dtScrollBody.css("max-height", sbNewHeight);
    	dtScrollBody.css("max-height", sbNewHeight);
        //objs.body.css("height", bodyHeight);
    	
	}
    else if (totalOuterHeight < parentHeight)
	{
    	dtScrollBody.css("max-height", sbNewHeight);
    	dtScrollBody.css("max-height", sbNewHeight);
	}
    dt.draw();
}

function updateDataTableScroll_dep2(targetTable, fillPercentage)
{

    if (typeof(targetTable) == "string" && !targetTable.startsWith("#"))
    {
        targetTable = "#" + targetTable;
    }
    var jqTable = $(targetTable);
    var dt = jqTable.DataTable();
    dt.draw();

    var objs = getDatatableObjects(jqTable);
    var totalHeight = objs["parent_div"].height();
    
    if (fillPercentage == null)
    {
        var resizeVal = jqTable.attr("resize_on_window_resize");
        try {
            resizeVal = parseFloat(resizeVal)
        }
        catch (error) {
            //Error parsing float ${resizeVal}.
            resizeVal = 100;
        }
        fillPercentage = resizeVal;
    }



    totalHeight = totalHeight * (fillPercentage / 100.0);
    var buttonHeight = 0;
    if (objs.buttons.length > 0)
    {
        buttonHeight = objs.buttons.outerHeight(true);
    }
    

    if (targetTable == "#platformSelectionTable2")
	{
    	console.log("hit target table.");
	}
    
    console.log("Target Table: " + targetTable);

    var hdrHt = isNaN(objs.header.height()) ? 0 : objs.header.height();
    var ftrHt = isNaN(objs.footer.height()) ? 0 : objs.footer.height(); 
    
    buttonHeight = isNaN(buttonHeight) ? 0 : buttonHeight;
    
    var bodyHeight = totalHeight - hdrHt - buttonHeight - ftrHt;

    var parentHeight = objs.wrapper.parent().height();
    var wrapperHeight = objs.wrapper.height();
    if (wrapperHeight > parentHeight)
	{
    	console.log("Wrapper Height: " + wrapperHeight);
    	console.log("Parent Height: " + parentHeight);
    	bodyHeight -= (wrapperHeight - parentHeight); 
    	console.log("Setting new body height to " + bodyHeight);
	}
    
    
    if (jqTable.attr("id") == "platformSensorPropertiesTable")
    {
        console.log("found the table.");
    }

    objs.body.css("max-height", bodyHeight);
    objs.body.css("height", bodyHeight);

    dt.draw();
}

function getMaxFloatFromColumn(datatableId, columnNum)
{
    var dt = $(`#${datatableId}`).DataTable();
    var data = dt.data();
    var maxVal = null;
    for (var x = 0; x < data.length; x++)
    {
        var curVal = parseFloat(data[x][columnNum]);
        if (maxVal == null || (curVal > maxVal))
        {
            maxVal = curVal;
        }
    }
    return maxVal;
}

//actualproperties = key / value pairs
//inlineOptions gets defaulted to propertiesTableInlineOptions if it's null
function setOpendcsPropertiesTable(table, propspecs, actualProperties, clearTable, inlineOptions, actions, keepCurrentProperties)
{
    if (actualProperties == null)
    {
        actualProperties = {};
    }
    var dTable;
    if (typeof table == "string")
    {
        if (!table.startsWith("#"))
        {
            table = "#" + table;
        }
    }
    dTable = $(table).DataTable();

    if (actions == null)
    {
        actions = [{
            "type": "delete",
            "onclick": null
        }];
    }

    var propSpecMeta = {};
    if (propspecs == null)
    {
        propspecs = [];
    }
    for (var propSpecObj of propspecs)
    {
        if (!(propSpecObj.name in actualProperties))
        {
            actualProperties[propSpecObj.name] = "";
        }
        propSpecMeta[propSpecObj.name] = {
                "hover": propSpecObj.description,
                "type": propSpecObj.type
        }
    }

    if (keepCurrentProperties)
    {
        var curData = dTable.data();
        for (var x = 0; x < curData.length; x++)
        {
            var curRow = curData[x];
            var key = curRow[0];
            var value = curRow[1];
            if (actualProperties[key] == null || actualProperties[key] == "")
            {
                actualProperties[key] = value;
            }
        }
    }

    if (clearTable)
    {
        dTable.init();
        dTable.clear();
        dTable.draw(false);
    }    
    var rowsWithValues = [];
    var rowsWithoutValues = [];

    for (var key in actualProperties)
    {
        var actions = [{
            "type": "delete",
            "onclick": null
        }];
        var newRow = [key, actualProperties[key], createActionDropdown(actions)];
        if (newRow[1] != "")
        {
            rowsWithValues.push(newRow);
        }
        else
        {
            rowsWithoutValues.push(newRow);
        }
    }

    //This causes the properties with values to go to the top.
    for (var x = 0; x < rowsWithValues.length; x++)
    {
        dTable.row.add(rowsWithValues[x]);    
    }
    for (var x = 0; x < rowsWithoutValues.length; x++)
    {
        dTable.row.add(rowsWithoutValues[x]);    
    }

    //Need to draw first so that the "td" elements can be found.
    dTable.draw(false); 
    if (inlineOptions == null)
    {
        var propertiesTableInlineOptions = {
                "columnDefs": [
                    {
                        "targets": [0,1],
                        "type": "input",
                        "data": null,
                        "bgcolor": {
                            "change": "#c4eeff"
                        }
                    }
                    ]
        };
        inlineOptions = propertiesTableInlineOptions;
    }

    makeTableInline(table, inlineOptions);
    dTable.draw(false);


    var rowCount = dTable.rows().count();
    for (var x = 0; x < rowCount; x++)
    {
        var rowData = dTable.row(x).data();
        var propName = rowData[0];
        if (propSpecMeta[propName] != null)
        {
            $(dTable.row(x).node()).attr("title", propSpecMeta[propName].hover);
            $($(dTable.row(x).node()).find("td")[1]).attr("forced_propspec_type", propSpecMeta[propName].type);
        }
    }
}

function openDcsRowDrag(tableObj)
{
    opendcsTableRows = $(tableObj).find("tbody tr");
    // Add HTML5 draggable event listeners to each row
    [].forEach.call(opendcsTableRows, function(row) {
        row.addEventListener('dragstart', handleDragStart, false);
        row.addEventListener('dragenter', handleDragEnter, false)
        row.addEventListener('dragover', handleDragOver, false);
        row.addEventListener('dragleave', handleDragLeave, false);
        row.addEventListener('drop', handleDrop, false);
        row.addEventListener('dragend', handleDragEnd, false);
    });
}


function handleDragStart(e) {
    this.style.opacity = '0.4';

    // Keep track globally of the source table data
    var jqTable = $(this.closest("table"));
    var dtTable = jqTable.DataTable();
    var dtRow = dtTable.row(this);
    var dtRowData = dtRow.data();
    dragDatatable["source"] = {
            "jqRow": $(this),
            "jqTable": jqTable,
            "dtTable": dtTable,
            "dtRow": dtRow,
            "dtRowIndex": dtRow.index(),
            "dtRowData": dtRowData
    }
    
    // Allow moves
    e.dataTransfer.effectAllowed = 'move';

    // Save the source row html as text
    e.dataTransfer.setData('text/plain', e.target.outerHTML);

}

function handleDragOver(e) {
    if (e.preventDefault) {
        e.preventDefault(); // Necessary. Allows us to drop.
    }

    // Allow moves
    e.dataTransfer.dropEffect = 'move'; 

    return false;
}

function handleDragEnter(e) {
    var jqTable = $(this.closest("table"));
    if (jqTable.is(dragDatatable["source"]["jqTable"]))
    {
        this.classList.add('over');
    }
}

function handleDragLeave(e) {
    this.classList.remove('over');  
}


function handleDrop(e) {

    if (e.stopPropagation) {
        e.stopPropagation(); // stops the browser from redirecting.
    }

    //Keep track globally of the destination table data
    var jqTable = $(this.closest("table"));
    var dtTable = jqTable.DataTable();
    var dtRow = dtTable.row(this);
    var dtRowData = dtRow.data();
    //var rowData = dtTable.
    dragDatatable["destination"] = {
            "jqRow": $(this),
            "jqTable": jqTable,
            "dtTable": dtTable,
            "dtRow": dtRow,
            "dtRowIndex": dtRow.index(),
            "dtRowData": dtRowData
    }

    if (dragDatatable["source"]["jqTable"].is(dragDatatable["destination"]["jqTable"]) && !dragDatatable["source"]["jqRow"].is(dragDatatable["destination"]["jqRow"])) {

        // If selected rows and dragged item is selected then move selected rows
        //if (selectedRows.count() > 0 && $(dragSrcRow).hasClass('selected')) {
        //if (selectedRow != null)
        //if (dragSrcRow != null)
        //{
        var sourceIndex = dragDatatable["source"]["dtRowIndex"];
        var targetIndex = dragDatatable["destination"]["dtRowIndex"];
        //row style swap
        var sourceBgColors = getRowTdBackgroundColors(dragDatatable["source"].jqRow);
        var destinationBgColors = getRowTdBackgroundColors(dragDatatable["destination"].jqRow);

        //var sourceSelected = doesRowHaveClass(dragDatatable["source"].jqRow, "selected");
        //var destinationSelected = doesRowHaveClass(dragDatatable["destination"].jqRow, "selected");

        var sourceSelected = isRowIndexSelected(dragDatatable["source"]["dtTable"], sourceIndex); //doesRowHaveClass(dragDatatable["source"].jqRow, "selected");
        var destinationSelected = isRowIndexSelected(dragDatatable["destination"]["dtTable"], targetIndex); //doesRowHaveClass(dragDatatable["destination"].jqRow, "selected");

        var sourceMarkedForDeletion = isRowIndexMarkedForDeletion(dragDatatable["source"]["jqTable"], sourceIndex);
        var destinationMarkedForDeletion = isRowIndexMarkedForDeletion(dragDatatable["destination"]["jqTable"], targetIndex);

        var jqRows = dragDatatable["source"]["jqTable"].find("tbody").find("tr");

        if (sourceIndex < targetIndex)
        {
            for (var x = dragDatatable["source"]["dtRow"].index(); x < targetIndex; x++)
            {
                //data swap
                var nextRow = dragDatatable["source"]["dtTable"].row(x+1).data();
                dragDatatable["destination"]["dtTable"].row(x).data(nextRow);

                var nextRowBgColors = getRowTdBackgroundColors($(jqRows[x+1]));
                setRowTdBackgroundColors($(jqRows[x]), nextRowBgColors);

                //if (doesRowHaveClass($(jqRows[x+1]), "selected"))
                if (isRowIndexSelected(dragDatatable["source"]["dtTable"], x+1))
                {
                    //$(jqRows[x]).addClass("selected");
                    dragDatatable["source"]["dtTable"].row(x).select();
                }
                else
                {
                    //$(jqRows[x]).removeClass("selected");
                    dragDatatable["source"]["dtTable"].row(x).deselect();
                }

                if (isRowIndexMarkedForDeletion(dragDatatable["source"]["jqTable"], x+1))
                {
                    setRowMarkedForDeletion(jqRows[x], true);
                }
                else
                {
                    setRowMarkedForDeletion(jqRows[x], false);
                }

            }
            //var nextRow = dragDatatable["source"]["dtTable"].row(x+1).data();
            //dragDatatable["destination"]["dtTable"].row(targetIndex).data(dragDatatable["source"]["dtRowData"]);
            //setRowTdBackgroundColors(dragDatatable["destination"].jqRow, sourceBgColors);
        }
        else
        {

            for (var x = sourceIndex; x >= targetIndex; x--)
            {
                var nextRow = dragDatatable["source"]["dtTable"].row(x-1).data();
                dragDatatable["destination"]["dtTable"].row(x).data(nextRow);

                var nextRowBgColors = getRowTdBackgroundColors($(jqRows[x-1]));
                setRowTdBackgroundColors($(jqRows[x]), nextRowBgColors);


                //if (doesRowHaveClass($(jqRows[x+1]), "selected"))
                if (isRowIndexSelected(dragDatatable["source"]["dtTable"], x-1))
                {
                    //$(jqRows[x]).addClass("selected");
                    dragDatatable["source"]["dtTable"].row(x).select();
                }
                else
                {
                    //$(jqRows[x]).removeClass("selected");
                    dragDatatable["source"]["dtTable"].row(x).deselect();
                }

                if (isRowIndexMarkedForDeletion(dragDatatable["source"]["jqTable"], x-1))
                {
                    setRowMarkedForDeletion(jqRows[x], true);
                }
                else
                {
                    setRowMarkedForDeletion(jqRows[x], false);
                }

            }
            //var nextRow = dragDatatable["source"]["dtTable"].row(x+1).data();
            //dragDatatable["destination"]["dtTable"].row(targetIndex).data(dragDatatable["source"]["dtRowData"]);

        }
        dragDatatable["destination"]["dtTable"].row(targetIndex).data(dragDatatable["source"]["dtRowData"]);
        setRowTdBackgroundColors(dragDatatable["destination"].jqRow, sourceBgColors);
        if (sourceSelected)
        {
            //dragDatatable["source"].jqRow.removeClass("selected");
            dragDatatable["destination"].dtRow.select();
        }
        else
        {
            dragDatatable["destination"].dtRow.deselect();
        }
        setRowMarkedForDeletion(dragDatatable["destination"].jqRow, sourceMarkedForDeletion);
        dragDatatable["destination"]["dtTable"].draw(); 
    }
    return false;
}

function handleDrop_deprecated(e) {
    // this / e.target is current target element.

    if (e.stopPropagation) {
        e.stopPropagation(); // stops the browser from redirecting.
    }

    // Get destination table id, row
    var dstTable = $(this.closest('table')).attr('id');

    // No need to process if src and dst table are the same
    if (srcTable == dstTable) {

        if (dragSrcRow != null)
        {
            var targetDrag = this;
            var frmtDragData = formatStatementsTable.row($(dragSrcRow)).data();
            var targetDragData = formatStatementsTable.row($(this)).data();

            var srcDragSiteName = frmtDragData[0];
            
            alert("New Data: " + JSON.stringify(newData));

            updateRowClickEvents();
            
            dragSrcRow = null;
            
        } 
        
    }
    return false;
}

function handleDragEnd(e) {
    // this/e.target is the source node.
    // Reset the opacity of the source row
    this.style.opacity = '1.0';

    // Clear 'over' class from both tables
    // and reset opacity
    [].forEach.call(opendcsTableRows, function (row) {
        row.classList.remove('over');
        row.style.opacity = '1.0';
    });
    
}


/**
 * Filters platforms in the netlist display based on the selected transport medium type.  NOTE - it will display the values that are passed and filter out all other values (not displaying them).
 * @param columnNumber {int} the column to run the filter
 * @param columnValue {string} the value that you want to be displayed.  It will hide all other values from sight.
 */
function filterTableBasedOnColumnVal(table, columnNumber, columnValue)
{
    var jqTable;
    if (typeof(table) == "string")
    {
        if (!table.startsWith("#"))
        {
            table = "#" + table;
        }
    }
    var jqTable = $(table);
    var dTable = jqTable.DataTable();
    dTable.column(columnNumber).search(columnValue).draw();
}

function getRowTdBackgroundColors(jqRow)
{
    var bgColors = [];
    var cells = jqRow.find("td");
    for (var x = 0; x < cells.length; x++)
    {
        bgColors.push($(cells[x]).css("backgroundColor"));
    }
    return bgColors;
}

function setRowTdBackgroundColors(jqRow, bgColors)
{
    for (var x = 0; x < bgColors.length; x++)
    {
        var cellBgColor = bgColors[x];
        if (cellBgColor != "")
        {
            $(jqRow.find("td")[x]).css("backgroundColor", cellBgColor);
        }
    }
}

function doesRowHaveClass(jqRow, clss)
{
    return jqRow.hasClass(clss);
}

function isRowIndexSelected(dtTable, rowIndex)
{
    var rowCount = dtTable.rows(rowIndex, {selected: true}).count();
    return rowCount > 0 ? true : false;
}


function getSelectedRows(dtTable)
{
    var selectedRows = dtTable.rows({selected: true});
    return selectedRows
}

function isRowIndexMarkedForDeletion(jqTable, rowIndex)
{
    var jqRows =  jqTable.find("tbody tr");
    var markedForDeletion = false;
    if (jqRows.length > rowIndex)
    {
        var row = jqRows[rowIndex];
        var markedForDeletionVal = $(row).attr("delete");
        markedForDeletion = markedForDeletionVal == null || markedForDeletionVal.toLowerCase() != "true" ? false : true;
    }
    return markedForDeletion;
}


function getModifiedRowIndexes(table)
{
    var jqTable;
    if (typeof(table) == "string")
    {
        if (!table.startsWith("#"))
        {
            table = "#" + table;
        }
    }
    var jqTable = $(table);
    var dTable = jqTable.DataTable();
    var modifiedCells = jqTable.find("td[data-modified]")
    var modifiedRowIndexes = [];
    for (var x = 0; x < modifiedCells.length; x++)
    {
        var rowIndex = dTable.row(dTable.cell(modifiedCells[x]).node().closest("tr")).index();
        if (modifiedRowIndexes.indexOf(rowIndex) == -1)
        {
            modifiedRowIndexes.push(rowIndex);
        }
    }
    return modifiedRowIndexes;
}

function getPropertiesTableData(tableId, includeBlank)
{   
    /*
    if (typeof(table) == "string")
    {
        if (!table.startsWith("#"))
        {
            table = "#" + table;
        }
    }
    var propTable = $(`${table}`).DataTable();
    var propertiesData = propTable.data();
     */
    var propertiesData = getNonDeletedRowData(tableId);
    var properties = {};
    for (var x = 0; x < propertiesData.length; x++)
    {
        var curRow = propertiesData[x];
        var curRowVal = curRow[1];
        if (!includeBlank && curRowVal != "")
        {
            properties[curRow[0]] = curRowVal;
        }
    }
    return properties;
}

function getRowWithColumnData(table, cellValue, columnNumber)
{
    if (typeof(table) == "string")
    {
        if (!table.startsWith("#"))
        {
            table = "#" + table;
        }
    }
    var dTable = $(`${table}`).DataTable();
    var data = dTable.data();
    for (var x = 0; x < data.length; x++)
    {
        var curDataRow = data[x];
        if (cellValue == curDataRow[columnNumber])
        {
            return {
                "row_index": x,
                "row_data": curDataRow,
                "row_html": dTable.row(x).node()
            };
        }
    }
    return null;
}
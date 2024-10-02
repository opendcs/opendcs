//enumerations.js
/**
 * Stores the reflists once retrieved from the API for future use.
 */
var refLists;

/**
 * DataTable reference to the enumeration table.
 */
var enumerationTable;

/**
 * Inline options for the enumeration table.
 */
var enumerationTableInlineOptions = {
        "columnDefs": [
            {
                "targets": [2,3,4,5],
                "type": "input",
                "data": null,
                "bgcolor": {
                    "change": "#c4eeff"
                }
            }
            ]
};

document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded enumerations.js.");
    enumerationTable = $("#enumerationTable").DataTable(
            {
                "select": {
                    "style": "single",
                    "selector": 'td:nth-child(2)'
                },
                "dom": 'flrtip',
                "searching": false,
                "ordering": false,
                "paging": false,
                "info": false,
                "buttons": [],
                "scrollCollapse": true,
                "autoWidth": true,
                createdRow: function ( row, data, dataIndex, cells ) {
                    $(row).attr('draggable', 'true');
                },
                drawCallback: function () {
                    openDcsRowDrag(this);
                }
            });

    $("#saveButton").on("click", function(e) {
        set_yesno_modal(
                `Save Enumeration`, 
                `${$("#enumerationSelectbox").val()}`, 
                `Are you sure you want to save the ${$("#enumerationSelectbox").val()} enumeration?`, 
                "bg-info", 
                function(e) {
                    var openReflistId = $("#enumerationSelectbox").find("option:selected").attr("reflist_id");
                    var openReflistName = $("#enumerationSelectbox").val();
                    var saveData = {
                            "reflistId": parseInt(openReflistId, 10),
                            "enumName": refLists[openReflistName].enumName,
                            "description": refLists[openReflistName].description,
                            "defaultValue": null,
                            "items": {}
                    };
                    var enumData = getNonDeletedRowData("enumerationTable");
                    var selectedRows = enumerationTable.rows({selected: true});
                    if (selectedRows.count() > 0)
                    {
                        var selectedRowIndex = selectedRows[0][0];
                        saveData["defaultValue"] = enumData[selectedRowIndex][2];
                    }
                    for (var x = 0; x < enumData.length; x++)
                    {
                        var curRowData = enumData[x];
                        saveData["items"][curRowData[2]] = {
                                "value": curRowData[2],
                                "description": (curRowData[3] == "") ? null : curRowData[3],
                                        "execClassName": (curRowData[4] == "") ? null : curRowData[4],
                                                "editClassName": (curRowData[5] == "") ? null : curRowData[5],
                                                        "sortNumber": x+1
                        };
                    }

                    $.ajax({
                        url: `${window.API_URL}/reflist`,
                        type: "POST",
                        headers: {     
                            "Content-Type": "application/json"
                        },
                        data: JSON.stringify(saveData),
                        success: function(response) {

                            setTimeout(function() {show_notification_modal("Save Config", 
                                    "Enumeration saved successfully", 
                                    `The enumeration has been saved successfully.`, 
                                    "OK", 
                                    "bg-success", 
                                    "bg-secondary",
                                    function() {
                                hide_notification_modal();
                                location.reload();
                            }
                            )}, 600);
                        },
                        error: function(response) {
                            hide_waiting_modal(500);
                            set_notification_modal("There was an error saving the enumeration", undefined, "The enumeration could not be saved.", undefined, "bg-danger", "bg-danger");
                            show_notification_modal();
                        }
                    });
                    show_waiting_modal();
                },
                "bg-info",
                function(e) {
                },
        "bg-secondary");
    });

    $("#addEnumerationButton").on("click", function(e) {
        var action = [{
            "type": "delete",
            "onclick": null
        }];
        addBlankRowToDataTable("enumerationTable", true, action, enumerationTableInlineOptions, dragIcon=true);
        enumerationTable.draw(false);
    });

    $("#enumerationSelectbox").on("change", function(e) {
        enumerationTable.init();
        enumerationTable.clear();
        enumerationTable.draw(false);

        var actions = [{
            "type": "delete",
            "onclick": null
        }];
        var clickedEnum = refLists[this.value];
        var items = clickedEnum["items"];
        var defaultRowIndex = null;
        var count = 0;
        var keys = Object.keys(items);
        //sortNumber starts at 1, so starting nextSortNumber at one and then 
        //going to <= length
        for (var nextSortNumber = 1; nextSortNumber <= keys.length; nextSortNumber++)
        {
            for (var key in items)
            {
                var item = items[key];
                if (item.sortNumber == nextSortNumber)
                {
                    var description = item.description != null ? item.description : "";
                    var javaClass = item.execClassName != null ? item.execClassName : "";
                    var options = item.editClassName != null ? item.editClassName : "";
                    var newRow = ['<i class="move-cursor icon-arrow-resize8 mr-3 icon-1x"></i>', "", key, description, javaClass, item.editClassName, createActionDropdown(actions)];
                    enumerationTable.row.add(newRow);
                    makeTableInline("enumerationTable", enumerationTableInlineOptions);
                    enumerationTable.draw(false);
                    if (clickedEnum.defaultValue != null && clickedEnum.defaultValue == key)
                    {
                        defaultRowIndex = count; 
                    }
                    count++;
                }
            }
        }
        makeTableInline("enumerationTable", enumerationTableInlineOptions);
        enumerationTable.draw(false);
        if (defaultRowIndex != null)
        {
            enumerationTable.rows(defaultRowIndex).select();
        }
    });

    var params = {};
    $.ajax({
        url: `${window.API_URL}/reflists`,
        type: "GET",
        data: params,
        success: function(response) {
            refLists = response;
            var enumNames = Object.keys(refLists).sort();
            for (var x = 0; x < enumNames.length; x++)
            {
                var enumName = enumNames[x];
                var optionAttributes = {
                        "value": enumName,
                        "reflist_id": refLists[enumName]["reflistId"]
                };
                var newOption = $("<option>").attr(optionAttributes).html(enumName);
                $("#enumerationSelectbox").append(newOption);
            }
            $("#enumerationSelectbox").trigger("change");
        },
        error: function(response) {
        }
    });
});
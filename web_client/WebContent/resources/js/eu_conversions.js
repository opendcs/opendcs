//eu_conversions.js
/**
 * The inline options for the main datatable.  Populated once data is 
 * retreived from the API.
 */
var mainTableInlineOptions; 

/**
 * DataTable reference to the main eu conversion table. 
 */
var mainTable;

/**
 * The actions for the main datatable.  This populates the menu on the right
 * side of each row.
 */
var dtActions = [{
    "type": "delete",
    "onclick": "deleteEuConversion(event, this)"
}];

/**
 * The save queue for the items that need to be saved.  Keeps track of what's
 * been sent to be saved, what's been saved, and what's error'd out on.
 */
var saveQueue = [];

/**
 * A list of the sensor unit conversions for the "algorithm" column of the
 * eu conversion table.
 */
var sensorUnitConversion;

document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded eu_conversions.js.");

    mainTable = $("#mainTable").DataTable(
            {
                "dom": 'flrtip',
                "searching": true,
                "ordering": true,
                "paging": false,
                "info": false,
                "buttons": [],
                "scrollCollapse": true,
                "autoWidth": true,
                "columnDefs": [
                    {
                        "targets": [ 0 ],
                        "visible": false,
                        "searchable": false
                    }
                    ] 
            });

    sensorUnitConversion = new SensorUnitConversion("mainTable", 3, 4, true, function(thisObject) 
            {
        var params = {
                "opendcs_api_call": "unitlist"
        }
        $.ajax({
            url: `../api/gateway`,
            type: "GET",
            data: params,
            success: function(response) {
                var unitList = response.map(a => a.abbr);
                mainTableInlineOptions = 
                {
                        "columnDefs": [
                            {
                                "targets": [1,2],
                                "type": "select",
                                "data": unitList,
                                "bgcolor": {
                                    "change": "#c4eeff"
                                }
                            },
                            {
                                "targets": [3],
                                "type": "select",
                                "data": Object.keys(sensorUnitConversion.openDcsUnitConversionAlgorithms),
                                "bgcolor": {
                                    "change": "#c4eeff"
                                }
                            },
                            {
                                "targets": [4,5,6,7,8,9],
                                "type": "input",
                                "data": null,
                                "bgcolor": {
                                    "change": "#c4eeff"
                                }
                            }
                            ]
                };
                var params = {
                        "opendcs_api_call": "euconvlist"
                }
                $.ajax({
                    url: `../api/gateway`,
                    type: "GET",
                    data: params,
                    success: function(response) {
                        var euConvList = response;

                        for (var x = 0; x < euConvList.length; x++)
                        {
                            var curRow = euConvList[x];
                            var a = curRow.a != null ? curRow.a : "";
                            var b = curRow.a != null ? curRow.b : "";
                            var c = curRow.a != null ? curRow.c : "";
                            var d = curRow.a != null ? curRow.d : "";
                            var e = curRow.a != null ? curRow.e : "";
                            var f = curRow.a != null ? curRow.f : "";
                            var newRow = [curRow.ucId, curRow.fromAbbr, curRow.toAbbr, curRow.algorithm, a, b, c, d, e, f, createActionDropdown(dtActions)];

                            mainTable.row.add(newRow);
                            makeTableInline("mainTable", mainTableInlineOptions, null, function(e) {
                                sensorUnitConversion.setSensorUnitAlgo(e);
                            });
                            mainTable.draw(false);
                        }
                        makeTableInline("mainTable", mainTableInlineOptions, null, function(e) {
                            sensorUnitConversion.setSensorUnitAlgo(e);
                        });
                        mainTable.draw(false);
                        sensorUnitConversion.runSensorUnitAlgoOnTable();
                        hide_waiting_modal(500);
                    },
                    error: function(response) {
                        hide_waiting_modal(500);
                    }
                });
            },
            error: function(response) {
                hide_waiting_modal(500);
            }
        });
            });

    $("#saveButton").on("click", function(e) {

        var modifiedRowIndexes = getModifiedRowIndexes("mainTable");
        var modifiedRowData = mainTable.rows(modifiedRowIndexes).data();
        var namesToBeSaved = [];
        for (var x = 0; x < modifiedRowData.length; x++)
        {
            var curRow = (modifiedRowData[x]);
            namesToBeSaved.push(curRow[2]);
        }

        set_yesno_modal(
                `Save Engineering Unit Conversions`, 
                `Are you sure you want to save the listed unit conversions?`, 
                `${namesToBeSaved.join(", ")}`, 
                "bg-info", 
                function(e) {

                    var saveData = [];
                    for (var x = 0; x < modifiedRowData.length; x++)
                    {
                        var curRow = modifiedRowData[x];

                        var rowSaveObj = {
                                "fromAbbr": curRow[1],
                                "toAbbr": curRow[2],
                                "algorithm": curRow[3],
                                "a": curRow[4],
                                "b": curRow[5],
                                "c": curRow[6],
                                "d": curRow[7],
                                "e": curRow[8],
                                "f": curRow[9]
                        }

                        var ucId = curRow[0];
                        if (ucId != null && ucId != "")
                        {
                            rowSaveObj["ucId"] = ucId;
                        }
                        saveData.push(rowSaveObj);
                    }
                    for (var x = 0; x < saveData.length; x++)
                    {
                        if (x === 0)
                        {
                            show_waiting_modal();
                        }
                        var curSaveData = saveData[x];

                        saveQueue.push(curSaveData.toAbbr);

                        var token = sessionStorage.getItem("token");
                        $.ajax({
                            url: `../api/gateway?token=${token}&opendcs_api_call=euconv`,
                                    type: "POST",
                                    headers: {     
                                        "Content-Type": "application/json"
                                    },
                                    dataType: "text",
                                    data: JSON.stringify(curSaveData),
                                    success: function(response) {
                                        var passedToAbbr = JSON.parse(this.data).toAbbr;
                                        var idx = saveQueue.indexOf(passedToAbbr);
                                        if (idx > -1) { // only splice array when item is found
                                            saveQueue.splice(idx, 1); // 2nd parameter means remove one item only
                                        }
                                        if (saveQueue.length <= 0)
                                        {
                                            setTimeout(function() {
                                                hide_waiting_modal();
                                                show_notification_modal("Save Engineering Unit Conversions", 
                                                        "Engineering unit conversions saved successfully", 
                                                        `The engineering unit conversions have been saved successfully.`, 
                                                        "OK", 
                                                        "bg-success", 
                                                        "bg-secondary",
                                                        function() {
                                                    location.reload();
                                                }
                                                )}, 600);
                                        }
                                        else
                                        {
                                            console.log("Continuing to wait for the save queue to clear up.");
                                        }
                                    },
                                    error: function(response) {
                                        setTimeout(function() 
                                                {
                                            hide_waiting_modal(50);
                                            show_notification_modal("There was an error saving the engineering unit conversions", 
                                                    null, 
                                                    "The engineering unit conversions could not be saved.", 
                                                    "OK", 
                                                    "bg-danger", 
                                            "bg-secondary");
                                            //set_notification_modal("There was an error saving the engineering units", undefined, "The engineering units could not be saved.", undefined, "bg-danger", "bg-danger");
                                            //show_notification_modal();
                                            saveQueue = [];
                                                }, 500);
                                    }
                        });
                    }
                },
                "bg-info",
                function(e) {
                },
        "bg-secondary");
    });

    $("#addButton").on("click", function(e) {

        addBlankRowToDataTable("mainTable", true, dtActions, mainTableInlineOptions);
        mainTable.draw(false);
    });
});

/**
 * Runs the delete functionality for the page.  The user will be asked if 
 * he/she is sure that a delete is desired before continuing with the delete.
 * 
 * @param event        The click event.
 * @param clickedLink  The link that was clicked to perform the delete.
 * @returns
 */
function deleteEuConversion(event, clickedLink)
{
    $(clickedLink).closest(".dropdown-menu").toggle();
    var rowData = mainTable.row(clickedLink.closest("tr")).data();

    var ucId = rowData[0];
    var fromAbbrev = rowData[1];
    var toAbbrev = rowData[2];

    if (ucId == null || ucId == "")
    {
        show_notification_modal("Delete Engineering Unit Conversion", 
                "Cannot delete this engineering unit conversion.", 
                "This engineering unit conversion is not saved, thus it cannot be deleted.  No need to try to delete it.", 
                "OK", 
                "bg-warning", 
                "bg-secondary",
                function() {
            hide_notification_modal();
        }
        );
        return;
    }

    set_yesno_modal("Delete Engineering Unit conversion", 
            `Delete ${fromAbbrev} to $(toAbbrev} Engineering Unit`, 
            `Are you sure you want to delete the  ${fromAbbrev} to ${toAbbrev} engineering unit conversion?`, 
            "bg-warning", 
            function() {

        var token = sessionStorage.getItem("token");

        var url = `../api/gateway?opendcs_api_call=euconv&token=${token}&euconvid=${ucId}`;
        show_waiting_modal();
        $.ajax({
            url: url,
            type: "DELETE",
            headers: {     
                "Content-Type": "application/json"   
            },
            dataType: "text",
            success: function(response) {
                setTimeout(function() {
                    show_notification_modal("Delete Engineering Unit Conversion", 
                            "Engineering Unit Conversion Deleted Successfully", 
                            "The engineering unit conversion has been saved successfully.", 
                            "OK", 
                            "bg-success", 
                            "bg-secondary",
                            function() {
                        hide_notification_modal();
                        location.reload();
                    }
                    );
                    hide_waiting_modal();
                }, 500);
            },
            error: function(response) {
                setTimeout(function() {
                    show_notification_modal("Delete Engineering Unit Conversion", 
                            "There was an error deleting engineering unit conversion.", 
                            response.responseText, 
                            "OK", 
                            "bg-danger", 
                            "bg-secondary",
                            function() {
                        hide_notification_modal();
                    }
                    );
                    hide_waiting_modal();
                }, 500);
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
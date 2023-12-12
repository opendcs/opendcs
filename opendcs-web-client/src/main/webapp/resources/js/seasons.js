//seasons.js
/**
 * A datatable reference to the main data table on the page.
 */
var mainTable;

/**
 * Inline options for the main datatable.
 */
var mainTableInlineOptions = {};


/**
 * Save queue based on the seasons that need to be saved.  Multiple seasons
 * can be edited before selecting save, so multiple seasons need to be saved.
 * TODO - Make the save functionality allow for saving a list of seasons.
 */
var saveQueue = [];

//Run on DOM load.
//------------------------------

document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded seasons.js.");

    allTimezones.unshift("");
    mainTableInlineOptions = {
            "columnDefs": [
                {
                    "targets": [2,3,4,5],
                    "type": "input",
                    "data": null,
                    "bgcolor": {
                        "change": "#c4eeff"
                    }
                },{
                    "targets": [6],
                    "type": "select",
                    "data": allTimezones,
                    "bgcolor": {
                        "change": "#c4eeff"
                    }
                }
                ]
    };

    mainTable = $("#mainTable").DataTable(
            {
                "dom": 'flrtip',
                "searching": false,
                "ordering": false,
                "paging": false,
                "info": false,
                "buttons": [],
                "scrollCollapse": true,
                "autoWidth": true,
                "columnDefs": [
                    {
                        "targets": [ 1 ],
                        "visible": false,
                        "searchable": false
                    }
                    ],
                    createdRow: function ( row, data, dataIndex, cells ) {
                        $(row).attr('draggable', 'true');
                    },
                    drawCallback: function () {
                        openDcsRowDrag(this);
                    }
            });

    $("#saveButton").on("click", function(e) {
        var seasonsToBeSaved = [];
        var allTableData = mainTable.data();
        for (var x = 0; x < allTableData.length; x++)
        {
            var curRow = (allTableData[x]);
            seasonsToBeSaved.push(curRow[2]);
        }

        set_yesno_modal(
                `Save Seasons`, 
                `Are you sure you want to save the listed seasons?`, 
                `${seasonsToBeSaved.join(", ")}`, 
                "bg-info", 
                function(e) {
                    var saveData = [];
                    for (var x = 0; x < allTableData.length; x++)
                    {
                        var curRow = allTableData[x];
                        var seasonToSave = {
                                "sortNumber": x,
                                "abbr": curRow[2],
                                "name": curRow[3],
                                "start": curRow[4],
                                "end": curRow[5],
                                "tz": curRow[6]
                        };
                        var fromAbbr = curRow[1];
                        if (fromAbbr != null && fromAbbr != "")
                        {
                            seasonToSave["fromabbr"] = fromAbbr;
                        }
                        saveData.push(seasonToSave);
                    }

                    for (var x = 0; x < saveData.length; x++)
                    {
                        if (x === 0)
                        {
                            show_waiting_modal();
                        }
                        var curSaveData = saveData[x];

                        saveQueue.push(curSaveData.abbr);

                        var token = sessionStorage.getItem("token");
                        $.ajax({
                            url: `../api/gateway?token=${token}&opendcs_api_call=season`,
                                    type: "POST",
                                    headers: {     
                                        "Content-Type": "application/json"
                                    },
                                    dataType: "text",
                                    data: JSON.stringify(curSaveData),
                                    success: function(response) {
                                        var passedAbbr = JSON.parse(this.data).abbr;
                                        var idx = saveQueue.indexOf(passedAbbr);
                                        if (idx > -1) { // only splice array when item is found
                                            saveQueue.splice(idx, 1); // 2nd parameter means remove one item only
                                        }
                                        if (saveQueue.length <= 0)
                                        {
                                            setTimeout(function() {
                                                hide_waiting_modal();
                                                show_notification_modal("Save Seasons", 
                                                        "Seasons saved successfully", 
                                                        `The seasons have been saved successfully.`, 
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
                                            show_notification_modal("There was an error saving the seasons", 
                                                    null, 
                                                    "The seasons could not be saved.", 
                                                    "OK", 
                                                    "bg-danger", 
                                            "bg-secondary");
                                            saveQueue = [];
                                                }, 500);
                                    }
                        });
                        show_waiting_modal();
                    }
                },
                "bg-info",
                function(e) {
                	//No clicked
                },
        "bg-secondary");
    });

    $("#addButton").on("click", function(e) {
        var action = [{
            "type": "delete",
            "onclick": "deleteSeason(event, this)"
        }];
        addBlankRowToDataTable("mainTable", true, action, mainTableInlineOptions, dragIcon=true);
        mainTable.draw(false);
    });

    var params = {
            "opendcs_api_call": "seasons"
    };
    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: params,
        success: function(response) {
            var actions = [{
                "type": "delete",
                "onclick": "deleteSeason(event, this)"
            }];

            //sort by sort number.
            response.sort((a, b) => (a.sortNumber > b.sortNumber) ? 1 : -1);

            for (var x = 0; x < response.length; x++)
            {
                var curSeason = response[x];

                var timezone = curSeason.tz != null ? curSeason.tz : "";
                var newRow = ['<i class="move-cursor icon-arrow-resize8 mr-3 icon-1x"></i>', curSeason.abbr, curSeason.abbr, curSeason.name, curSeason.start, curSeason.end, timezone, createActionDropdown(actions)];
                mainTable.row.add(newRow);
                makeTableInline("mainTable", mainTableInlineOptions);
                mainTable.draw(false);
            }
            makeTableInline("mainTable", mainTableInlineOptions);
            mainTable.draw(false);
        },
        error: function(response) {
            console.log("Error getting seasons.");
        }
    });
});

/**
 * Queries the api to delete a season.
 * 
 * @param event {object}       the event from the click to delete a season.
 * @param clickedLink {object} the link that was clicked by the user to delete
 *                             the season. 
 * @returns
 */
function deleteSeason(event, clickedLink)
{
    $(clickedLink).closest(".dropdown-menu").toggle();
    var rowData = mainTable.row(clickedLink.closest("tr")).data();

    var originalAbbrev = rowData[1];
    var abbrev = rowData[2];
    var fullName = rowData[3];

    if (originalAbbrev == null || originalAbbrev == "")
    {
        show_notification_modal("Delete Season", 
                "Cannot delete this season.", 
                "This season is not saved, thus it cannot be deleted.  No need to try to delete it.", 
                "OK", 
                "bg-warning", 
                "bg-secondary",
                function() {
            hide_notification_modal();
        }
        );
        return;
    }

    set_yesno_modal("Delete Season", 
            `Delete ${fullName} Season`, 
            `Are you sure you want to delete the ${fullName} season?`, 
            "bg-warning", 
            function() {

        var token = sessionStorage.getItem("token");

        var url = `../api/gateway?opendcs_api_call=season&token=${token}&abbr=${originalAbbrev}`;
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
                    show_notification_modal("Delete Season", 
                            "Season Deleted Successfully", 
                            "The season has been saved successfully.", 
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
                    show_notification_modal("Delete Season", 
                            "There was an error deleting season.", 
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
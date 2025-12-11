//engineering_units.js
 /**
 * Inline options for the main datatable, which get set after retrieving
 * some data from the api.
 */
var mainTableInlineOptions;

/**
 * A DataTable reference to the main datatable
 */
var mainTable;

/**
 * Actions for users to choose from on the Engineering Unit datatable
 * This represents the right menu on each row.
 */
var dtActions = [{
    "type": "delete",
    "onclick": "deleteEngineeringUnit(event, this)"
}];
//Run on DOM load.
//------------------------------

/**
 * Because the Engineering units are ordered, when you save one, you need to
 * reorder them.  This could be an area of improvement for the API, where
 * there could be a way to 'insert' an engineering unit before or after 
 * another one, which brings up issues of multi-users simultaneously.
 */
var saveQueue = [];

document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded engineering_units.js.");
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

    $("#saveButton").on("click", function(e) {

        var modifiedRowIndexes = getModifiedRowIndexes("mainTable");
        var modifiedRowData = mainTable.rows(modifiedRowIndexes).data();
        var euNamesToBeSaved = [];
        for (var x = 0; x < modifiedRowData.length; x++)
        {
            var curRow = (modifiedRowData[x]);
            euNamesToBeSaved.push(curRow[2]);
        }

        set_yesno_modal(
                `Save Engineering Units`, 
                `Are you sure you want to save the listed units?`, 
                `${euNamesToBeSaved.join(", ")}`, 
                "bg-info", 
                function(e) {

                    var saveData = [];
                    for (var x = 0; x < modifiedRowData.length; x++)
                    {
                        var curRow = modifiedRowData[x];
                        var newRow = {
                                "abbr": curRow[1],
                                "name": curRow[2],
                                "family": curRow[3],
                                "measures": curRow[4]
                            };
                        var fromAbbr = curRow[0];
                        var fromAbbrString = "";
                        if (fromAbbr != null && fromAbbr != "")
                    	{
                        	fromAbbrString = (x === 0) ? "?fromabbr=" + fromAbbr : "&fromabbr=" + fromAbbr;
                    	}
                        saveData.push(newRow);
                        
                    }

                    for (var x = 0; x < saveData.length; x++)
                    {
                        if (x === 0)
                        {
                            show_waiting_modal();
                        }
                        var curSaveData = saveData[x];

                        saveQueue.push(curSaveData.abbr);

                        $.ajax({
                            url: `${window.API_URL}/eu${fromAbbrString}`,
                                    type: "POST",
                                    headers: {     
                                        "Content-Type": "application/json"
                                    },
                                    dataType: "text",
                                    data: JSON.stringify(curSaveData),
                                    success: function(response) {
                                        var passedAbbr = JSON.parse(this.data).abbr;
                                        var idx = saveQueue.indexOf(passedAbbr);
                                     // only splice array when item is found
                                        if (idx > -1) { 
                                        	// 2nd parameter means remove one item only
                                            saveQueue.splice(idx, 1); 
                                        }
                                        if (saveQueue.length <= 0)
                                        {
                                            setTimeout(function() {
                                                hide_waiting_modal();
                                                show_notification_modal("Save Engineering Units", 
                                                        "Engineering units saved successfully", 
                                                        `The engineering units have been saved successfully.`, 
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
                                            show_notification_modal("There was an error saving the engineering units", 
                                                    null, 
                                                    "The engineering units could not be saved.", 
                                                    "OK", 
                                                    "bg-danger", 
                                            "bg-secondary");
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

    //This is to load the data on the webpage.
    show_waiting_modal();
    $.ajax({
        url: `${window.API_URL}/reflists`,
        type: "GET",
        data: {},
        success: function(response) {
            var refLists = response;
            var unitFamilyList = Object.keys(refLists.UnitFamily.items);
            //Need to add universal to this list, as it's not in the UnitFamily 
            //reflist.
            unitFamilyList.push("univ");
            unitFamilyList.sort();
            var measuresList = Object.keys(refLists.Measures.items);
            measuresList.sort();
            mainTableInlineOptions = 
            {
                    "columnDefs": [
                        {
                            "targets": [1,2],
                            "type": "input",
                            "data": null,
                            "bgcolor": {
                                "change": "#c4eeff"
                            }
                        },
                        {
                            "targets": [3],
                            "type": "select",
                            "data": unitFamilyList,
                            "bgcolor": {
                                "change": "#c4eeff"
                            }
                        },
                        {
                            "targets": [4],
                            "type": "select",
                            "data": measuresList,
                            "bgcolor": {
                                "change": "#c4eeff"
                            }
                        }
                        ]
            };
            $.ajax({
                url: `${window.API_URL}/unitlist`,
                type: "GET",
                data: {},
                success: function(response) {
                    for (var x = 0; x < response.length; x++)
                    {
                        var curRow = response[x];
                        var newRow = [curRow.abbr, curRow.abbr, curRow.name, curRow.family, curRow.measures, createActionDropdown(dtActions)];
                        mainTable.row.add(newRow);
                        makeTableInline("mainTable", mainTableInlineOptions);
                        mainTable.draw(false);
                    }
                    hide_waiting_modal(500);
                },
                error: function(response) {
                    hide_waiting_modal(500);
                }
            });
        },
        error: function(response) {
        }
    });

});

/**
 * Deleted an engineering unit when the user selects the delete menu item.
 * 
 * @param event        The click event when the user clicks the delete menu item
 * @param clickedLink  The clicked dom object
 * @returns
 */
function deleteEngineeringUnit(event, clickedLink)
{
    $(clickedLink).closest(".dropdown-menu").toggle();
    var rowData = mainTable.row(clickedLink.closest("tr")).data();

    var originalAbbrev = rowData[0];
    var abbrev = rowData[1];
    var fullName = rowData[2];

    if (originalAbbrev == null || originalAbbrev == "")
    {
        show_notification_modal("Delete Engineering Unit", 
                "Cannot delete this engineering unit.", 
                "This engineering unit is not saved, thus it cannot be deleted.  No need to try to delete it.", 
                "OK", 
                "bg-warning", 
                "bg-secondary",
                function() {
            hide_notification_modal();
        }
        );
        return;
    }

    set_yesno_modal("Delete Engineering Unit", 
            `Delete ${fullName} Engineering Unit`, 
            `Are you sure you want to delete the ${fullName} engineering unit?`, 
            "bg-warning", 
            function() {

        show_waiting_modal();
        $.ajax({
            url: `${window.API_URL}/eu?abbr=${originalAbbrev}`,
            type: "DELETE",
            headers: {     
                "Content-Type": "application/json"   
            },
            dataType: "text",
            success: function(response) {
                setTimeout(function() {
                    show_notification_modal("Delete Engineering Unit", 
                            "Engineering Unit Deleted Successfully", 
                            "The engineering unit has been saved successfully.", 
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
                    show_notification_modal("Delete Engineering Unit", 
                            "There was an error deleting engineering unit.", 
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
    return;
}
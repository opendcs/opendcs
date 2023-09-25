

/**
 * Holds the retrieved algorithm references, which are retrieved from the API. 
 */
var algorithmRefs;
/**
 * This is a reference to the main datatable of the page.
 */
var mainTable;
/**
 * This is a reference to the properties table.
 */
var propertiesTable;

/**
 * These are the actions defined for the properties table in the properties table.
 */
var modalTableActions = [{
    "type": "delete",
    "onclick": null
}];

/**
 * The datatable options for the properties table.
 */
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

/**
 * The datatable options for the parameters table.
 */
var parametersTableInlineOptions = {
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

/**
 * The reference to the parametersTable.
 */
var parametersTable;

/**
 * The params for the delete onclick event in the main datatable.
 */
var params = {
        "objectType": "algorithm",
        "objectTypeDisplayName": "Algorithm",
        "objectIdIndex": 0,
        "objectNameIndex": 1,
        "urlIdName": "algorithmid"
};


/**
 * The actions at the side of the datatable: i.e. delete, copy. 
 */
var dtActions = [
    {
        "type": "delete",
        "onclick": `deleteOpendcsObject_default(event, this, ${JSON.stringify(params)})`
    },
    {
        "type": "copy",
        "onclick": "copyRow(event, this)"
    }];


/**
 * The prop specs that are notable for the associated datatable used on this 
 * page.  In this case, the properties table.
 */
var defaultPropSpecs = [];

/*
 * Run this code when all of the content has been loaded.
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded engineering_units.js.");

    var params = {
            "opendcs_api_call": "propspecs",
            "class": "decodes.cwms.validation.ScreeningAlgorithm"
    }

    //Get the prop specs.
    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: params,
        success: function(response) {
            for (var x = 0; x < response.length; x++)
            {
                var curPropSpec = response[x];
                defaultPropSpecs.push(curPropSpec);
            }
        },
        error: function(response) {
            show_notification_modal("Error", 
                    "There was an issue getting data from the OpenDCS REST API", 
                    `Please contact your system administrator.`, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    null);
        }
    });


    //initialize the main datatable on the page.
    $('#mainTable').on('click', 'tbody tr', beginOpenMainTableDialog);
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

    //Initialize the properties table.
    propertiesTable = $("#propertiesTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'flrtip',
                "searching": false,
                "ordering": false,
                "paging": false,
                "autoWidth": true,
                "info": false,
                "scrollY": 150,
                "scrollCollapse": true,
                "buttons": [],
                "columnDefs": []

            });

    //initialize the parameters table.
    parametersTable = $("#parametersTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'flrtip',
                "searching": false,
                "ordering": false,
                "paging": false,
                "autoWidth": true,
                "info": false,
                "scrollY": 150,
                "scrollCollapse": true,
                "buttons": [],
                "columnDefs": []

            });

    //On show, redraw and reinitialize the view of these tables.
    //They cannot be initialized until they are visible (on the modal).
    $("#modal_main").on('shown.bs.modal', function(){
        propertiesTable.draw();
        updateDataTableScroll("propertiesTable");
        propertiesTable.draw();
        parametersTable.draw();
        updateDataTableScroll("parametersTable");
        parametersTable.draw();
    });


    //When clicked, adds a record to the datatable.
    $("#addPropertyButton").on("click", function() {
        var action = [{
            "type": "delete",
            "onclick": null
        }];
        addBlankRowToDataTable("propertiesTable", true, action, propertiesTableInlineOptions);
    });

    //When clicked, adds a record to the datatable.
    $("#addParameterButton").on("click", function() {
        var action = [{
            "type": "delete",
            "onclick": null
        }];
        addBlankRowToDataTable("parametersTable", true, action, propertiesTableInlineOptions);
    });


    //Save the algorithm.  Pops up a yes/no modal requesting confirmation.
    $("#saveButton").on("click", function(e) {
        set_yesno_modal(
                `Save Algorithm`, 
                `Are you sure you want to save this algorithm?`, 
                ``, 
                "bg-info", 
                function(e) {
                    //Build out the save data.
                    var saveData = {
                            "name": $("#algorithmNameTextbox").val(),
                            "execClass": $("#execClassTextbox").val(),
                            "description": $("#commentsTextarea").val(),
                            "numCompsUsing": $("#numCompsTextbox").val(),
                            "algoScripts": [],
                            "props": {},
                            "parms": []
                    };

                    var propData = getNonDeletedRowData("propertiesTable");
                    for (var x = 0; x < propData.length; x++)
                    {
                        var curPropData = propData[x];
                        if (curPropData[1] != "")
                        {
                            saveData.props[curPropData[0]] = curPropData[1];
                        }
                    }

                    var paramData = getNonDeletedRowData("parametersTable");
                    for (var x = 0; x < paramData.length; x++)
                    {
                        var curParamData = paramData[x];
                        saveData.parms.push({
                            "roleName": curParamData[0],
                            "parmType": curParamData[1]
                        });
                    }
                    var displayedId = $("#algorithmIdTextbox").val();
                    if (displayedId != null && displayedId != "")
                    {
                        saveData["algorithmId"] = displayedId;
                    }
                    show_waiting_modal();

                    var token = sessionStorage.getItem("token");
                    $.ajax({
                        url: `../api/gateway?token=${token}&opendcs_api_call=algorithm`,
                        type: "POST",
                        headers: {     
                            "Content-Type": "application/json"
                        },
                        dataType: "text",
                        data: JSON.stringify(saveData),
                        success: function(response) {
                            setTimeout(function() {
                                hide_waiting_modal();
                                show_notification_modal("Save Algorithm", 
                                        "Algorithm saved successfully", 
                                        `The algorithm has been saved successfully.`, 
                                        "OK", 
                                        "bg-success", 
                                        "bg-secondary",
                                        function() {
                                    location.reload();
                                }
                                )}, 600);
                        },
                        error: function(response) {
                            setTimeout(function() 
                                    {
                                hide_waiting_modal(50);
                                show_notification_modal("There was an error saving the algorithm", 
                                        null, 
                                        "The algorithm could not be saved.", 
                                        "OK", 
                                        "bg-danger", 
                                "bg-secondary");
                                    }, 500);
                        }
                    });
                },
                "bg-info",
                function(e) {
                    console.log("No Clicked.");
                },
        "bg-secondary");
    });

    //Opens the main table dialog (this is how to add a new element to the 
    //main datatable.
    $("#addButton").on("click", function(e) {
        beginOpenMainTableDialog(null);

    });

    var params = {
            "opendcs_api_call": "algorithmrefs"
    };

    //Ajax call to load the list of data into the main datatable.
    show_waiting_modal();
    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: params,
        success: function(response) {
            algorithmRefs = response;
            for (var x = 0; x < algorithmRefs.length; x++)
            {
                var curRow = algorithmRefs[x];
                var newRow = [curRow.algorithmId, curRow.algorithmName, curRow.execClass, curRow.numCompsUsing, curRow.description, createActionDropdown(dtActions)];
                mainTable.row.add(newRow);
                //makeTableInline("mainTable", mainTableInlineOptions);
                mainTable.draw(false);
            }
            hide_waiting_modal(500);
        },
        error: function(response) {
            console.log("Error getting UnitFamily reflist.");
        }
    });
    console.log("End of content loaded event function.");
});


/**
 * Clears the main dialog to reload with new data.
 * 
 * @returns
 */
function clearDialog()
{
    $("#displayedId").attr("value", "");
    $("#algorithmNameTextbox").val("");
    $("#execClassTextbox").val("");
    $("#algorithmIdTextbox").val("");
    $("#numCompsTextbox").val("");
    $("#commentsTextarea").val("");
    propertiesTable.init();
    propertiesTable.clear();
    propertiesTable.draw(false);
    parametersTable.init();
    parametersTable.clear();
    parametersTable.draw(false);
}


/**
 * Performs the process for opening the main dialog.  This happens when either
 * the user clicks new, copy, or selects a row to be edited
 * 
 * @param rowClicked If a row is clicked, this will be populated with that row.
 *                   null if the new button is clicked. 
 * @param copy       true if the user selects copy on a new row.
 * @returns
 */
function beginOpenMainTableDialog(rowClicked, copy)
{
    clearDialog();
    $("#modal_main").modal("show");
    //This will be null unless it came from a row being clicked.
    if (rowClicked != null)
    {
        var clickedData = mainTable.row(this).data();
        if (!copy)
        {
            $("#displayedId").attr("value", clickedData[0]);
            $("#algorithmIdTextbox").val(clickedData[0]);
        }
        else
        {
            clickedData = mainTable.row(rowClicked).data();
        }
        $("#algorithmNameTextbox").val(clickedData[1]);
        $("#execClassTextbox").val(clickedData[2]);
        $("#numCompsTextbox").val(clickedData[3]);
        $("#commentsTextarea").val(clickedData[4]);

        var params = {
                "algorithmid": clickedData[0],
                "opendcs_api_call": "algorithm"
        };
        show_waiting_modal();
        $.ajax({
            url: `../api/gateway`,
            type: "GET",
            data: params,
            success: function(response) {
                setOpendcsPropertiesTable("propertiesTable", 
                        defaultPropSpecs, 
                        response.props, 
                        true, 
                        propertiesTableInlineOptions, 
                        modalTableActions);

                for (var x = 0; x < response.parms.length; x++)
                {
                    var curParam = response.parms[x];
                    var roleName = curParam.roleName;
                    var paramType = curParam.parmType;
                    var newRow = [roleName, paramType, createActionDropdown(modalTableActions)];
                    parametersTable.row.add(newRow);
                    makeTableInline("parametersTable", parametersTableInlineOptions);
                    parametersTable.draw(false);
                }
                makeTableInline("parametersTable", parametersTableInlineOptions);
                parametersTable.draw(false);
                hide_waiting_modal(500);
            },
            error: function(response) {
                //General API Error.
                show_notification_modal("unit list retrieval error", 
                        "There was an issue getting the unit list data from the OpenDCS REST API", 
                        `Please contact your system administrator.`, 
                        "OK", 
                        "bg-danger", 
                        "bg-secondary",
                        null);
            }
        });
    }
    else
    {
        setOpendcsPropertiesTable("propertiesTable", 
                defaultPropSpecs, 
                {}, 
                true, 
                propertiesTableInlineOptions, 
                modalTableActions);
    }
}

/**
 * Performs a copy action on the selected row.  In this case, it opens the
 * main dialog, populating everything except the algorithmid into the dialog.
 * 
 * @param event       The click event.
 * @param clickedLink The copy link that was clicked by the user. 
 * @returns
 */
function copyRow(event, clickedLink)
{
    $(clickedLink).closest(".dropdown-menu").toggle();
    event.stopPropagation();
    beginOpenMainTableDialog(clickedLink.closest("tr"), true);
}
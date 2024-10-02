//Setup module
//------------------------------

/**
 * A DataTable reference to the presentation table.
 */
var presentationTable;

/**
 * A DataTable reference to the presentation elements table.
 */
var presentationElementsTable;

/**
 * The inline options for the presentation elements table.
 */
var presElemInlineOptions;

/**
 * OpenDCS reflists and propspecs are stored here.  This is an instance of
 * the OpenDcsData class, which is an object that helps get recurring 
 * API data from the OpenDCS API.
 */
var openDcsData;

document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded presentation.js.");
    var elems = Array.prototype.slice.call(document.querySelectorAll('.form-check-input-switchery'));
    elems.forEach(function(html) {
        var switchery = new Switchery(html);
    });

    openDcsData = new OpenDcsData();
    show_waiting_modal();
    openDcsData.getData(["presentationrefs", "reflists", "unitlist", "datatypelist"], 
            function(classInstance, response) {
        updatePresentationTable(classInstance.data.presentationrefs.data);
        $("#inheritsFromSelectbox").append($('<option>', {
            groupid: -1,
            value: "",
            text: ""
        }));

        for (var x = 0; x < classInstance.data.presentationrefs.data.length; x++)
        {
            var curPres = classInstance.data.presentationrefs.data[x];
            $("#inheritsFromSelectbox").append($('<option>', {
                groupid: curPres.groupId,
                value: curPres.name,
                text: curPres.name
            }));
        }

        hide_waiting_modal(500);
    },
    function(response) {
        hide_waiting_modal(500);
    }
    );

    initializeEvents();
    initializeDataTables()    
});

/**
 * Initializes the presentation table with the reflist data once it is retrieved
 * 
 * @param allPresentations {Array of Objects} An array of all of the 
 *                         presentation groups. 
 * @returns
 */
function updatePresentationTable(allPresentations)
{
    presentationTable.init();
    presentationTable.clear();
    presentationTable.draw(false);
    $("#displayedPresentationGroupId").attr("value", "");
    for (var presentationKey in allPresentations)
    {
        var curPresentation = allPresentations[presentationKey];
        if (curPresentation.name == null)
        {
            curPresentation.name = "";
        }
        if (curPresentation.inheritsFrom == null)
        {
            curPresentation.inheritsFrom = "";
        }
        if (curPresentation.lastModified == null)
        {
            curPresentation.lastModified = "";
        }
        if (curPresentation.production == null)
        {
            curPresentation.production = false;
        }
        var isProdDisplay = curPresentation.production ? '<i class="icon-checkmark4 mr-3 icon-1x"></i>' : "";
        var params = {
                "objectType": "presentation",
                "objectTypeDisplayName": "Presentation Group",
                "objectIdIndex": 0,
                "objectNameIndex": 1,
                "urlIdName": "groupid"
        };
        var actions = [{
            "type": "delete",
            "onclick": `deleteOpendcsObject_default(event, this, ${JSON.stringify(params)})`
        },{
            "type": "copy",
            "onclick": "copyRow(event, this)"
        }];
        presentationTable.row.add([curPresentation.groupId, curPresentation.name, curPresentation.inheritsFrom, curPresentation.lastModified, isProdDisplay, createActionDropdown(actions)]);
    }
    presentationTable.draw(false);
}

/**
 * opens the dialog for the selected record, but it's considered a new one
 * the object id is not set.
 * 
 * @param event       The event from the copy row click.
 * @param clickedLink The copy link clicked, which ties to the corresponding
 *                    row.
 * @returns
 */
function copyRow(event, clickedLink)
{
    $(clickedLink).closest(".dropdown-menu").toggle();
    event.stopPropagation();
    presentationClicked(clickedLink.closest("tr"), true);    
}

/**
 * Shows the presentation group modal and populates it with the presentation
 * details
 * @param presentationDetails {Object} The details of the presentation group
 *                            which are used to populate the presentation dialog
 * @returns
 */
function show_presentation_modal(presentationDetails)
{
    presentationElementsTable.init();
    presentationElementsTable.clear();
    presentationElementsTable.search("");
    $("#presentationName").val("");
    $("#displayedPresentationGroupId").val("-1");
    $("#inheritsFromSelectbox option").removeClass("displayNone");
    if (presentationDetails != null)
    {
        //This is a check to see if copy was clicked or not.
        if ($("#displayedPresentationGroupId").data("value") != -1)
        {
            $("#presentationName").val(presentationDetails.name);
            $("#displayedPresentationGroupId").val(presentationDetails.groupId);
        }
        else
        {
            $("#presentationName").val("");
            $("#displayedPresentationGroupId").val("-1");
        }
        updateSwitchValue("isProductionSwitch", presentationDetails.production);
        $(`#inheritsFromSelectbox option[groupid=${presentationDetails.groupId}]`).addClass("displayNone");
        var selectedInheritVal = -1;
        if (presentationDetails.inheritsFrom != "" && presentationDetails.inheritsFrom != null)
        {
            selectedInheritVal = presentationDetails.inheritsFrom;
        }
        $("#inheritsFromSelectbox").val(selectedInheritVal);
        for (var x = 0; x < presentationDetails.elements.length; x++)
        {
            var curPresElem = presentationDetails.elements[x];
            var min = (curPresElem.min != null) ? curPresElem.min : "";
            var max = (curPresElem.max != null) ? curPresElem.max : "";;
            var actions = [{
                "type": "delete",
                "onclick": null
            }];
            var presRow = [curPresElem.dataTypeStd, curPresElem.dataTypeCode, curPresElem.units, curPresElem.fractionalDigits, min, max, createActionDropdown(actions)];
            var newRow = presentationElementsTable.row.add(presRow);
        }
    }
    presentationElementsTable.draw(true);
    var unitAbbrevList = openDcsData.data.unitlist.data.map(a => a.abbr);
    var dataTypeList = openDcsData.data.datatypelist.data.map(a => a.code).sort();
    presElemInlineOptions = {
            "columnDefs": [
                {
                    "targets": [1],
                    "type": "select",
                    "data": dataTypeList,
                    "bgcolor": {
                        "change": "#c4eeff"
                    }
                },
                {
                    "targets": [0],
                    "type": "select",
                    "data": Object.keys(openDcsData.data.reflists.data.DataTypeStandard.items),
                    "bgcolor": {
                        "change": "#c4eeff"
                    }
                },
                {
                    "targets": [2],
                    "type": "select",
                    "data": unitAbbrevList,
                    "bgcolor": {
                        "change": "#c4eeff"
                    }
                },
                {
                    "targets": [3,4,5],
                    "type": "number",
                    "data": null,
                    "bgcolor": {
                        "change": "#c4eeff"
                    }
                }
                ]
    };
    makeTableInline("presentationElementsTable", presElemInlineOptions);
    $("#modal_presentation").modal("show");
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
function presentationClicked(rowClicked, copy)
{
    var clickedData; 
    if (copy)
    {
        clickedData = presentationTable.row(rowClicked).data();
        $("#displayedPresentationGroupId").data("value", -1);
    }
    else
    {
        clickedData = presentationTable.row(this).data();
        $("#displayedPresentationGroupId").data("value", clickedData[0]);
    }

    show_waiting_modal();
    $.ajax({
        url: `${window.API_URL}/presentation`,
        type: "GET",
        headers: {     
            "Content-Type": "application/json"
        },
        data: {
            "groupid": clickedData[0]
        },
        success: function(response) {
            hide_waiting_modal(500);
            setTimeout(show_presentation_modal, 500, response);
        },
        error: function(response) {
            hide_waiting_modal(500);
            set_notification_modal("There was an error saving the netlist", undefined, "The netlist could not be saved.", undefined, "bg-danger", "bg-danger");
            show_notification_modal();
        }
    });
}

/*******Initialize Events*********/
/**
 * Initializes all of the events on the webpage.
 * 
 * @returns
 */
function initializeEvents()
{
    //Save Presentation from Modal Window.
    $("#savePresentationModalButton").on("click", function(e){
        var pn = $("#presentationName").val();
        if (pn == null || pn == "")
        {
            show_notification_modal("Save Presentation Group", 
                    "Cannot Save Presentation Group", 
                    "You need to set a name for this presentation group.  Please type in a name and try again.", 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    function() {
                hide_notification_modal();
            }
            );
            return;
        }
        set_yesno_modal("Save Presentation Group", 
                `Save ${pn}`, 
                `Are you sure you want to save the ${pn} presentation group?`, 
                "bg-info", 
                function() {

            var params = {
                    "name": $("#presentationName").val(),
                    "production": isSwitchChecked("isProductionSwitch"),
                    "elements": []
            }
            var elemData = getNonDeletedRowData("presentationElementsTable");
            for (var x = 0 ; x < elemData.length; x++)
            {
                var row = elemData[x];
                var elemObj = {
                        "dataTypeCode": row[1],
                        "dataTypeStd": row[0],
                        "fractionalDigits": row[3],
                        "units": row[2]                
                };
                if (row[4] != null && row[4] != "")
                {
                    elemObj["min"] = row[4];
                }
                if (row[5] != null && row[5] != "")
                {
                    elemObj["max"] = row[5];
                }
                params["elements"].push(elemObj);
            }

            var groupId = $("#displayedPresentationGroupId").val();
            if (groupId != null && groupId != "" && groupId != "-1")
            {
                params["groupId"] = groupId;
            }
            var inheritsFrom = $("#inheritsFromSelectbox").val();
            var inheritsGroupId = "";
            if (inheritsFrom != null && inheritsFrom != "" && inheritsFrom != "-1")
            {
                params["inheritsFrom"] = inheritsFrom;
                params["inheritsFromId"] = $("#inheritsFromSelectbox").find(':selected').attr('groupid');
            }

            $.ajax({
                url: `${window.API_URL}/presentation`,
                type: "POST",
                headers: {     
                    "Content-Type": "application/json"
                },

                data: JSON.stringify(params),
                success: function(response) {
                    hide_waiting_modal(500);
                    setTimeout(function() {show_notification_modal("Save Presentation Group", 
                            "Presentation Group saved successfully", 
                            `The presentation group ${response.name} has been saved successfully.`, 
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
                    set_notification_modal("There was an error saving the presentation group", undefined, "The presentation group could not be saved.", undefined, "bg-danger", "bg-danger");
                    show_notification_modal();
                }
            });
            show_waiting_modal();
        }, 
        "bg-info", 
        null, 
        "bg-secondary");    
        show_yesno_modal();
    });
}

/****************Initialize Datatables********************/
/**
 * Initializes all of the datatables on the webpage.
 * 
 * @returns
 */
function initializeDataTables()
{
    presentationTable = $("#presentationTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'Bflrtip',
                "buttons": [
                    {
                        text: '+',
                        action: function ( e, dt, node, config ) {
                            show_presentation_modal(null);
                        },
                        className: "btn main-table-button"
                    }
                    ],
                    "columnDefs": [
                        {
                            "targets": [ 0 ],
                            "visible": false,
                            "searchable": false
                        }
                        ]            

            });
    $('#presentationTable').on('click', 'tbody tr', presentationClicked);

    presentationElementsTable = $("#presentationElementsTable").DataTable(
            {
                "pageLength": -1,
                "scrollCollapse": true,
                "autoWidth": true,
                "dom": 'Bfrti',
                "buttons": [
                    {
                        text: '+',
                        action: function ( e, dt, node, config ) {
                            var targetId = "presentationElementsTable";
                            var action = [{
                                "type": "delete",
                                "onclick": null
                            }];
                            addBlankRowToDataTable(targetId, true, action, presElemInlineOptions);
                        },
                        className: "btn btn-secondary"
                    }
                    ]
            });
}
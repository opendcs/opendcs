//processes.js

/**
 * A DataTable reference to the main datatable on the page.
 */
var mainTable;

/**
 * A DataTable reference to the properties table on the page.
 */
var propertiesTable;

/**
 * The actions for the "application Properties" table.
 */
var modalTableActions = [{
    "type": "delete",
    "onclick": null
}];

/**
 * The inline options for the "Application Properties" table.
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
 * DataTable actions for the main process table.
 */
var dtActions = [
    {
        "type": "delete",
        "onclick": "deleteRow(event, this)"
    },
    {
        "type": "copy",
        "onclick": "copyRow(event, this)"
    }];

/**
 * A cross reference key-value object that allows for easy use of the 
 * OpenDcsData variable interacting with the DOM.
 */
var propspecsRefs = {
        "computationprocess": "decodes.tsdb.ComputationApp",
        "dcpmon": "decodes.dcpmon.DcpMonitor",
        "routingscheduler": "decodes.routing.RoutingScheduler"
}

/**
 * OpenDCS reflists and propspecs are stored here.  This is an instance of
 * the OpenDcsData class, which is an object that helps get recurring 
 * API data from the OpenDCS API.
 */
var openDcsData;

document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded processes.js.");
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
                "columnDefs": [] 
            });

    $('#mainTable').on('click', 'tbody tr', openMainTableDialog);
    $("#modal_main").on('shown.bs.modal', function(){
        updateDataTableScroll("propertiesTable");
        propertiesTable.draw();
    });

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

    openDcsData = new OpenDcsData();
    openDcsData.getData(["apprefs", "tsrefs", "unitlist", "reflists"], 
            function(classInstance, response) {
        for (var x = 0; x < classInstance.data["apprefs"].data.length; x++)
        {
            var curRow = classInstance.data["apprefs"].data[x];
            var params = {
                    "objectType": "app",
                    "objectTypeDisplayName": "Process",
                    "objectIdIndex": 0,
                    "objectNameIndex": 1,
                    "urlIdName": "appid"
            };
            var actions = [{
                "type": "delete",
                "onclick": `deleteOpendcsObject_default(event, this, ${JSON.stringify(params)})`
            },{
                "type": "copy",
                "onclick": "copyRow(event, this)"
            }];
            var newRow = [curRow.appId, curRow.appName, "", curRow.comment, createActionDropdown(actions)];
            mainTable.row.add(newRow);
            mainTable.draw(false);
        }
        var options = [{}];
        for (var key in classInstance.data.reflists.data.ApplicationType.items)
        {
            var curAppType = classInstance.data.reflists.data.ApplicationType.items[key];
            options.push({
                "text": curAppType.value,
                "value": curAppType.value,
                "title": curAppType.description
            });
        }
        createSelectBox("processTypeSelectbox", options);
    },
    function(response) {
    }
    );

    openDcsData.getPropspecs(Object.values(propspecsRefs));
    $('#processTypeSelectbox').on("change", function(e) {
        var selectedVal = this.value;
        if (propspecsRefs[this.value] != null)
        {
            setOpendcsPropertiesTable("propertiesTable", 
                    openDcsData.propspecs[propspecsRefs[this.value]].data, 
                    null, 
                    true, 
                    null, 
                    modalTableActions,
                    true);
        }
    });

    $("#addPropertyButton").on("click", function() {
        var action = [{
            "type": "delete",
            "onclick": null
        }];
        addBlankRowToDataTable("propertiesTable", true, action, propertiesTableInlineOptions);
    });

    $("#saveButton").on("click", function(e) {
        set_yesno_modal(
                `Save Process`, 
                `Are you sure you want to save this process?`, 
                ``, 
                "bg-info", 
                function(e) {
                    var name = $("#processNameTextbox").val();
                    var id = $("#processIdTextbox").val();
                    var processType = $("#processTypeSelectbox").val();
                    var comment = $("#commentsTextarea").val();
                    var isManualEnabled = isSwitchChecked("manualEditCheckbox");
                    var props =  getPropertiesTableData("propertiesTable");

                    var saveData = {
                            "appName": name,
                            "appType": processType,
                            "comment": comment,
                            "manualEditingApp": isManualEnabled,
                            "properties": props
                    }

                    if (id != null && id != "")
                    {
                        saveData["appId"] = id;
                    }

                    show_waiting_modal();

                    $.ajax({
                        url: `${window.API_URL}/app`,
                        type: "POST",
                        headers: {     
                            "Content-Type": "application/json"
                        },
                        dataType: "text",
                        data: JSON.stringify(saveData),
                        success: function(response) {
                            setTimeout(function() {
                                hide_waiting_modal();
                                show_notification_modal("Save Process", 
                                        "Process saved successfully", 
                                        `The process has been saved successfully.`, 
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
                                show_notification_modal("There was an error saving the process", 
                                        null, 
                                        "The process could not be saved.", 
                                        "OK", 
                                        "bg-danger", 
                                "bg-secondary");
                                    }, 500);
                        }
                    });
                },
                "bg-info",
                function(e) {
                },
        "bg-secondary");
    });

    $("#addButton").on("click", function(e) {
        openMainTableDialog(null);
    });
});

/**
 * Clears the process dialog so it can be populated clean and fresh.
 * 
 * @returns
 */
function clearDialog()
{
    $("#modal_main #modalTitle #modalSubTitle").html(" (New)");
    $("#displayedId").attr("value", "");
    $("#processIdTextbox").val("");
    $("#processNameTextbox").val("");
    $("#processTypeSelectbox").val("");
    $("#compIdTextbox").val("");
    $("#commentsTextarea").val("");
    updateSwitchValue("manualEditCheckbox", false);
    propertiesTable.init();
    propertiesTable.clear();
    propertiesTable.draw(false);
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
function openMainTableDialog(rowClicked, copy)
{
    clearDialog();
    //This will be null unless it came from a row being clicked.
    if (rowClicked != null) //opened from clicking a row.
    {
        var clickedData;
        if (!copy)
        {
            clickedData = mainTable.row(this).data();
            $("#modal_main #modalTitle #modalSubTitle").html(` (Edit ${clickedData[1]})`);
        }
        else
        {
            clickedData = mainTable.row(rowClicked).data();
            $("#modal_main #modalTitle #modalSubTitle").html(` (Copy ${clickedData[1]})`);
        }
        var appId = clickedData[0];
        var params = {
                "appid": appId,
                "copy": copy
        }
        $.ajax({
            url: `${window.API_URL}/app`,
            type: "GET",
            data: params,
            success: function(response) {
                var isCopy = getParamValueFromRelativeUrl(this.url, "copy");
                var processType = response.appType;
                $("#modal_main").modal("show");
                $("#processNameTextbox").val(response.appName);
                if (isCopy == null || isCopy.toLowerCase() != "true")
                {
                    $("#processIdTextbox").val(response.appId);
                }
                $("#processTypeSelectbox").val(processType);
                updateSwitchValue("manualEditCheckbox", response.manualEditingApp);
                $("#commentsTextarea").val(response.comment);
                var props = response.properties;
                props["LastModified"] = response.lastModified != null ? response.lastModified : ""; 
                setOpendcsPropertiesTable("propertiesTable", 
                        openDcsData.propspecs[propspecsRefs[processType]].data,
                        props, 
                        true, 
                        null, 
                        modalTableActions);
            },
            error: function(response) {
            }
        });
    }
    else  //creating a new one.
    {
        $("#modal_main").modal("show");
        setOpendcsPropertiesTable("propertiesTable", 
                [], 
                {}, 
                true, 
                null, 
                modalTableActions);
    }
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
    openMainTableDialog(clickedLink.closest("tr"), true);
}
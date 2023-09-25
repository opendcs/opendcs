//sources.js

/**
 * DataTable reference to the sources data table.
 */
var sourcesTable;

/**
 * DataTable reference to the properties data table.
 */
var propertiesTable;

/**
 * DataTable reference to the group data table.
 */
var groupTable;

/**
 * Cross reference data for the prop specs and the select dropdown items.
 */
var sourcesPropSpecs = {
        "directory": "decodes.datasource.DirectoryDataSource",
        "ftp": "decodes.datasource.FtpDataSource",
        "hotbackupgroup": "decodes.datasource.HotBackupGroup",
        "lrgs": "decodes.datasource.LrgsDataSource",
        "roundrobingroup": "decodes.datasource.RoundRobinGroup",
        "file": "decodes.datasource.ScpDataSource",
        "socketstream": "decodes.datasource.SocketStreamDataSource",
        "usgs": "decodes.datasource.UsgsWebDataSource",
        "abstractweb": "decodes.datasource.WebAbstractDataSource",
        "web": "decodes.datasource.WebDataSource",
        "webdirectory": "decodes.datasource.WebDirectoryDataSource",
        "polled": "decodes.polling.PollingDataSource"
};

/**
 * Inline Options for the properties table.
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
 * This is the list of group types that enable the Group Member select list. 
 */
var enableGroupMembers = ["hotbackupgroup", "roundrobingroup"];

/**
 * The OpenDcsData object.  Retrieves all the data you need at the beginning
 * of the page load so it can be used throughout the use of the page without
 * the need to retrieve common data multiple times.
 */
var openDcsData;

document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded sources.js.");
    openDcsData = new OpenDcsData();
    show_waiting_modal();
    openDcsData.getData(["datasourcerefs", "reflists", "unitlist", "datatypelist"], 
            function(classInstance, response) {
        updateSourcesTable(classInstance.data.datasourcerefs.data);
        updateGroupsTable(classInstance.data.datasourcerefs.data);
        for (var key in classInstance.data.reflists.data.DataSourceType.items)
        {
            var value = classInstance.data.reflists.data.DataSourceType.items[key].value;
            var newOption = $('<option>', { 
                value: value,
                text : value 
            });
            $('#sourceType').append(newOption);
        }
        $(`#sourceType option[value="${classInstance.data.reflists.data.DataSourceType.defaultValue}"]`).prop("selected", true);
        updateGroupsEnabled(classInstance.data.reflists.data.DataSourceType.defaultValue);


        hide_waiting_modal(500);
    },
    function(response) {
        hide_waiting_modal(500);
    }
    );
    var propSpecsToGet = Object.values(sourcesPropSpecs);
    openDcsData.getPropspecs(propSpecsToGet);
    initializeEvents();
    initializeDataTables();
});


/**
 * Updates the groups table based on the sources available.
 * 
 * @param srcs {object} Key value pairs of sources in the database.
 * 
 * @returns
 */
function updateGroupsTable(srcs)
{
    for (var sourcesKey in srcs)
    {
        var curSource = srcs[sourcesKey];
        groupTable.row.add([curSource.name, curSource.dataSourceId]);
    }
    groupTable.draw(false);
}

/**
 * Updates the main table (sources) using the reflist of sources retrieved
 * from the API
 * 
 * @param responseJson {object} The sources in the database, retrieved from
 *                              the API.
 * @returns
 */
function updateSourcesTable(responseJson)
{
    sourcesTable.init();
    sourcesTable.clear();
    sourcesTable.draw(false);
    for (var sourcesKey in responseJson)
    {
        var curSource = responseJson[sourcesKey];
        if (curSource.name == null)
        {
            curSource.name = "";
        }
        if (curSource.type == null)
        {
            curSource.type = "";
        }
        if (curSource.arguments == null)
        {
            curSource.arguments = "";
        }
        if (curSource.usedBy == null)
        {
            curSource.usedBy = "";
        }
        var params = {
                "objectType": "datasource",
                "objectTypeDisplayName": "Data Source",
                "objectIdIndex": 5,
                "objectNameIndex": 0,
                "urlIdName": "datasourceid"
        };

        var actions = [{
            "type": "delete",
            "onclick": `deleteOpendcsObject_default(event, this, ${JSON.stringify(params)})`
        },
        {
            "type": "copy",
            "onclick": "copyRow(event, this)"
        }];
        sourcesTable.row.add([curSource.name, curSource.type, curSource.arguments, curSource.usedBy, createActionDropdown(actions), curSource.dataSourceId]);
    }
    sourcesTable.draw(false);
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
    openSourcesDialog(clickedLink.closest("tr"), true);
}

/**
 * Clears the sources dialog so that a new source can be populated into the
 * dialog.
 * 
 * @returns
 */
function clearSourcesDialog()
{
    propertiesTable.clear().draw();
    groupTable.rows().deselect();
    $("#groupTable tr").removeClass("displayNone");
    $("#sourceName").val("");
    $("#sourceType").val(openDcsData.data.reflists.data.DataSourceType.defaultValue);
    $("#sourceId").text("");
}


/**
 * Populates the sources dialog with data of the source, which has been 
 * retrieved from the API.
 * 
 * @param data {object}  The source data that has been retrieved from the API.
 * @returns
 */
function populateSourcesDialog(data)
{
    clearSourcesDialog();
    $("#sourceName").val("");
    if (data != null && data != {})
    {
        if ($("#displayedId").val().toString() != "-1")
        {
            $("#sourceName").val(data.name);
            $("#sourceId").text(data.dataSourceId.toString());
        }
        $("#sourceType").val(data.type);
        for (var key in data.props)
        {
            var actions = [{
                "type": "delete",
                "onclick": null
            }];
            addPropertyToTable(key, data.props[key], createActionDropdown(actions));
        }
        propertiesTable.draw(false);
        var openSourceId = $("#sourceId").text();
        groupTable.rows( function ( idx, data, node ) { 
            if (data[1].toString() == openSourceId)
            {
                groupTable.row(idx).node().classList.add("displayNone");
            }
        });
        for (var x = 0; x < data.groupMembers.length; x++)
        {
            var curGm = data.groupMembers[x];
            groupTable.rows( function ( idx, data, node ) { 
                if(data[1] === curGm.dataSourceId){
                    groupTable.row(idx).select();
                }
            });
        }
        groupTable.draw(false);
        updateGroupsEnabled(data.type);
    }
    // This will get the properties table to fill with the prop specs of the
    // selected source type.
    $("#sourceType").trigger("change");
    makeTableInline("propertiesTable", propertiesTableInlineOptions);
}


/**
 * Adds a property into the property table.
 * 
 * @param name  {string}   The property name.
 * @param value {string}   The property value.
 * 
 * @returns
 */
function addPropertyToTable(name, value)
{
    var actions = [{
        "type": "delete",
        "onclick": null
    }];
    propertiesTable.row.add([name, value, createActionDropdown(actions)]);
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
function openSourcesDialog(rowClicked, copyRow)
{
    var sourceData = null;
    var params = {}
    if (rowClicked != null)
    {
        sourceData = sourcesTable.row(this).data();
        params = {};

        if (!copyRow)
        {
            sourceData = sourcesTable.row(this).data();
        }
        else
        {
            sourceData = sourcesTable.row(rowClicked).data();
        }
        params["datasourceid"] = sourceData[5];

        if (copyRow)
        {
            $("#displayedId").val("-1")
        }
        else
        {
            $("#displayedId").val(sourceData[5]);
        }
        var token = sessionStorage.getItem("token");
        show_waiting_modal();
        params["token"] = token;
        params["opendcs_api_call"] = "datasource";
        $.ajax({
            url: "../api/gateway",
            type: "GET",
            data: params,
            success: function(response) {
                setTimeout(function(data) {
                    populateSourcesDialog(data);
                    hide_waiting_modal(0);
                    $("#modal_success").modal("show");
                }, 500, response);
            },
            error: function(response) {
                hide_waiting_modal(500);
            }
        });
    }
    else
    {
        populateSourcesDialog(null);
        $("#modal_success").modal("show");
    }
}

/**
 * Enables or disable the "enabled groups" table.
 * 
 * @param val {string} This the value of the selected item.  If it is in the
 * enableGroupMembers list, then enable the group members table.  Otherwise,
 * disable it.
 * @returns
 */
function updateGroupsEnabled(val)
{
    if (enableGroupMembers.indexOf(val) != -1)
    {
        $("#groupTable").removeClass("datatable-disabled");
    }
    else
    {
        $("#groupTable").addClass("datatable-disabled");
    }    
}

/**
 * Initializes all of the events on the page.
 * @returns
 */
function initializeEvents()
{

    $('#modal_success').on('shown.bs.modal', function () {
        $("#propertiesTable").DataTable().draw(false);
        updateDataTableScroll("propertiesTable");
    });

    $('#sourceType').on("change", function(e) {
        var selectedVal = this.value;
        if (sourcesPropSpecs[this.value] != null)
        {
            var curData = propertiesTable.data();
            var curProps = {};
            for (var x = 0; x < curData.length; x++)
            {
                var cd = curData[x];
                if (cd[1] != "")
                {
                    curProps[cd[0]] = cd[1];
                }
            }
            propertiesTable.clear().draw();
            var actions = [{
                "type": "delete",
                "onclick": null
            }];
            setOpendcsPropertiesTable("propertiesTable", 
                    openDcsData.propspecs[sourcesPropSpecs[this.value]].data, 
                    curProps, 
                    true, 
                    propertiesTableInlineOptions, 
                    actions);


            updateGroupsEnabled(selectedVal);

        }

    });

    $("#saveSourcesModalButton").on("click", function(e){
        var token = sessionStorage.getItem("token");

        var groupMembers = [];
        var groupData = groupTable.rows( { selected: true } ).data();
        for (var x = 0; x < groupData.length; x++)
        {
            var d = groupData[x];
            groupMembers.push({
                "dataSourceId": d[1], 
                "dataSourceName": d[0]});
        }

        var properties = {};
        var propertiesData = getNonDeletedRowData("propertiesTable");
        for (var x = 0; x < propertiesData.length; x++)
        {
            var p = propertiesData[x];
            if (p[1] != "")
            {
                properties[p[0]] = p[1];
            }
        }

        var params = {
                "name": $("#sourceName").val(),
                "type": $("#sourceType").val(),
                "groupMembers": groupMembers,
                "props": properties            

        }

        if (params.name == "")
        {
            show_notification_modal(
                    "Save Source",
                    "Cannot save source", 
                    "You need to have a name for the source.  Please add a name and then try saving again.",
                    "OK",
                    "bg-danger",
                    "bg-secondary",
                    null
            ); 
            return;
        }

        var dataSourceId = $("#sourceId").text();
        if (dataSourceId != "")
        {
            params["dataSourceId"] = parseInt(dataSourceId, 10);
        }
        show_waiting_modal();
        $.ajax({
            url: `../api/gateway?token=${token}&opendcs_api_call=datasource`,
            type: "POST",
            headers: {     
                "Content-Type": "application/json"
            },
            data: JSON.stringify(params),
            success: function(response) {
                hide_waiting_modal(500);
                setTimeout(function(response) {
                    show_notification_modal(
                            "Save Source",
                            "Saved Successfully", 
                            "The " + response.name + " was saved successfully.  Click OK to reload the page.",
                            "OK",
                            "bg-success",
                            "bg-secondary",
                            function(e){
                                location.reload();
                            }
                    );  
                }, 500, response);
            },
            error: function(response) {
                hide_waiting_modal(500);
                setTimeout(function(response) {
                    show_notification_modal(
                            "Save Source",
                            "Error saving source", 
                            "There was an issue saving the source.",
                            "OK",
                            "bg-danger",
                            "bg-secondary",
                            null
                    );  
                }, 500, response);
            }
        });
    });

    $("#addPropertyButton").on("click", function() {
        addPropertyToTable("", "");
        propertiesTable.draw(false);
        makeTableInline("propertiesTable", propertiesTableInlineOptions);
    });

    $("#deleteSourceModalButton").on("click", function(e){
        var token = sessionStorage.getItem("token");
        var url = `../api/gateway?token=${token}`;
        var params = {
                "opendcs_api_call": "datasource"
        };
        var dataSourceId = $("#sourceId").text();
        if (dataSourceId != "")
        {
            url += "&datasourceid=" + dataSourceId;
        }
        else
        {
            show_notification_modal("Delete Source", 
                    "This is not a saved source.", 
                    "You cannot delete this source, as it has not been saved yet.", 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    function() {
                hide_notification_modal();
            }
            );
            return
        }

        $.ajax({
            url: url,
            type: "DELETE",
            headers: {     
                "Content-Type": "application/json"   
            },

            data: params,
            success: function(response) {
                hide_waiting_modal(500);
                show_notification_modal("Delete Source", 
                        "Source Deleted Successfully", 
                        "The source has been saved successfully.", 
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
                show_notification_modal("Delete Source", 
                        "There was an error deleting Source.", 
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
    });
}


/**
 * Initializes all of the datatables on the web page.
 * 
 * @returns
 */
function initializeDataTables()
{
    sourcesTable = $("#sourcesTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'Bflrtip',
                "buttons": [
                    {
                        text: '+',
                        action: function ( e, dt, node, config ) {
                            openSourcesDialog(null);
                        },
                        className: "btn main-table-button"
                    }
                    ],
                    "columnDefs": [
                        {
                            "targets": [ 5 ],
                            "visible": false,
                            "searchable": false
                        }
                        ]

            });

    groupTable = $("#groupTable").DataTable(
            {
                "select": {
                    "style": "multi+shift"
                },
                "searching": false,
                "ordering": false,
                "paging": false,
                "info": false,
                "dom": 'Bflrtip',
                "buttons": [
                    ],
                    "columnDefs": [

                        {
                            "targets": [ 1 ],
                            "visible": false,
                            "searchable": false
                        }
                        ],
                        "autoWidth": false

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
                "scrollY": 100,
                "scrollCollapse": true,
                "buttons": [],
                "columnDefs": []
            });
    $('#sourcesTable').on('click', 'tbody tr', openSourcesDialog);
}

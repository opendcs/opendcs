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

/* ------------------------------------------------------------------------------
 *
 *  # Netlist
 *  # Users can view, create, edit, and delete network lists.
 *    # Author Olav & Co., Inc.
 *
 * ---------------------------------------------------------------------------- */

/**
 * Maintains the datatable object for the netlist details datatable.
 */
var nlTable;

/**
 * Maintains the datatable object for the netlist list datatable.
 */
var netlistListTable;

/**
 * Keeps track of the netlists when the netlist details modal is open.
 */
var curNetlistDetails = undefined;

/**
 * The list of GOES transport medium types.  
 * TODO: This needs to be hard coded for now, but should not be eventually.
 */
var goesTms =     ["goes",                
    "goes-self-timed", 
    "goes-random"];

/**
 * The list of transport medium types that should be ignored.  
 * TODO: This maybe should be a value that the user can select in a preference
 * page.
 */
var ignoreTms = ["shef",
    "polled-modem", 
    "polled-tcp", 
    "data-logger", 
    "other", 
    "incoming-tcp"];

/**
 * The variable for storing all reference and propspec data so it can all 
 * be retrieved on page load.
 */
var openDcsData;

/**
 * Runs on page load.  
 * Initializes the button clicks, 
 * Initializes the datatables
 * Gets the information from the api that is needed to display the netlist list, as well as information that will be reused as the user navigates. 
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded netlist.js.");
    openDcsData = new OpenDcsData();
    show_waiting_modal();
    openDcsData.getData(["netlistrefs", "reflists", "siterefs", "configrefs", "platformrefs"], 
            function(classInstance, response) {

        updateNetlistsTable(classInstance.data.netlistrefs.data);

        var transportMediumTypes = Object.keys(classInstance.data.reflists.data.TransportMediumType.items);
        var blankO = new Option("", "");
        $("#transportMediumTypeSelectbox").append(blankO);
        for (var x = 0; x < transportMediumTypes.length; x++)
        {
            var o = new Option(transportMediumTypes[x], transportMediumTypes[x]);
            $("#transportMediumTypeSelectbox").append(o);
        }
        for (var x=0; x<ignoreTms.length; x++)
        {
            var targetIndex = transportMediumTypes.indexOf(ignoreTms[x]);
            if (targetIndex != -1)
            {
                transportMediumTypes.splice(targetIndex, 1);
            }
        }
        hide_waiting_modal(500);
    },
    function(response) {
    }
    );

    initializeEvents();
    initializeDataTables();

});

/**
 * Is passed a site id and a site type and retrieves the site name based on 
 * those two passed parameters.
 * 
 * @param siteId     The site id to filter on
 * @param siteType   The site type to filter on
 * @returns {string} the site name for the site based on the site type.
 */
function getSiteName(siteId, siteType)
{
    var siteName = null;
    openDcsData.data.siterefs.data.forEach(site => {
        if (siteName == null && site.siteId == siteId)
        {
            for (var nameType in site.sitenames)
            {
                if (siteType.toLowerCase() == nameType.toLowerCase())
                {
                    siteName = site.sitenames[nameType];
                }
            }
        }
    });
    return siteName;
}

/**
 * Gets the config details of a specific config (Making an ajax request to the OHydroJson api).
 * See the OpenDCS OHydroJson documentation for more details
 * @param configId {int} The database config id of the desired config
 * @return {object} - Returns the config details in object form, or null if no config is found.
 */
function getConfigById(configId)
{
    for (var x = 0; x < openDcsData.data.configrefs.data.length; x++)
    {
        var curConfig = openDcsData.data.configrefs.data[x];
        if (curConfig.configId == configId)
        {
            return curConfig;
        }
    }
    return null;
}

/**
 * Gets all of the configured transport media types for the given platform.
 * 
 * @param platform   The platform which the query will be run against.
 * @returns          A list of transport media types, specific to the platform.
 */
function getTransportMediaTypes(platform)
{
    var tmTypes = [];
    if (platform.transportMedia != null)
    {
        for (var tmType in platform.transportMedia)
        {
            tmTypes.push(tmType);
        }
    }
    return tmTypes;
}

/**
 * Gets all of the configured transport media ids for the given platform.
 * 
 * @param platform   The platform which the query will be run against.
 * @returns          A list of transport media ids, specific to the platform.
 */
function getTransportMediaIds(platform)
{
    var tmIds = [];
    if (platform.transportMedia != null)
    {
        for (var tmType in platform.transportMedia)
        {
            tmIds.push(platform.transportMedia[tmType]);
        }
    }
    return tmIds;
}

/**
 * When creating the details required for displaying a netlist, the platforms also are required.
 * This function will find the platforms that are assigned to that netlist and add them to the netlist object..
 * @param netlistDetails {object} All of the details for the netlist, minus platform information
 * @return {object} Returns the netlist details with the platforms added.
 */
function setNetlistPlatforms(netlistDetails)
{
    var allPlatformsModded = {};
    for (var platKey in openDcsData.data.platformrefs.data)
    {
        var curPlatform = openDcsData.data.platformrefs.data[platKey];

        //TODO: Need to get transportMediumType as a property from the API call.
        curPlatform.transportMediumType = getTransportMediaTypes(curPlatform)

        var config = getConfigById(curPlatform.configId);
        curPlatform.config = "";
        if (config != null)
        {
            curPlatform.config = config.name;
        }

        if (netlistDetails != null)
        {
            var tmIds = getTransportMediaIds(curPlatform);
            if (tmIds.some(r=> Object.keys(netlistDetails.items).includes(r)))
            {
                curPlatform.inNetlist = true;
            }
            else
            {
                curPlatform.inNetlist = false;
            }
        }
        else
        {
            curPlatform.inNetlist = false;
        }
        allPlatformsModded[platKey] = curPlatform;
    }
    return allPlatformsModded;
}

/**
 * Makes the ajax request to the OHydroJson api to get the requested netlist details.  
 * On success, it will open the netlist details modal window, which will trigger a function to populate the current details and configuration of that netlist into the modal.
 * If a netlist is clicked, it will attempt to get the clicked netlist details.  
 * If the "add netlist" button is clicked, it will open the netlist modal window with default options.
 * @param netlistId {int} The database id for the desired netlist.
 */
function populateNetlistDetails(netlistId)
{
    //A netlist was clicked and is being opened to be edited.
    if (netlistId != -1) 
    {
        var params = {
                "opendcs_api_call": "netlist",
                "netlistid": netlistId
        };
        $.ajax({
            url: `../api/gateway`,
            type: "GET",
            data: params,
            success: function(response) {
                var netlistDetails = response;
                var allPlatformsModded = setNetlistPlatforms(netlistDetails);
                show_netlist_modal(netlistDetails, allPlatformsModded, openDcsData.data.reflists.data.TransportMediumType.items);
            },
            error: function(response) {
                console.log("Error getting netlist details.");
            }
        });
    }
    //A new netlist will be created.
    else 
    {
        var allPlatformsModded = setNetlistPlatforms(null);
        show_netlist_modal(null, allPlatformsModded, openDcsData.data.reflists.data.TransportMediumType.items);
    }
}

/**
 * Clears, initializes, and updates the netlist list table with the list of netlists retrieved from the OHydroJson api.
 * The table is comprised of four columns, name, transportMediumType, numPlatforms, and netlistId.
 * netlistId is a hidden column and used to keep track of the db id for clicked netlists.
 * @param netlistJson {object} The netlist list object retrieved from the OHydroJson api.
 */
function updateNetlistsTable(netlistJson)
{
    netlistListTable.clear();
    netlistListTable.draw(false);
    for (var netlistKey in netlistJson)
    {
        var curNetlist = netlistJson[netlistKey];

        var params = {
                "objectType": "netlist",
                "objectTypeDisplayName": "Netlist",
                "objectIdIndex": 3,
                "objectNameIndex": 0,
                "urlIdName": "netlistid"
        };
        var actions = [{
            "type": "delete",
            "onclick": `deleteOpendcsObject_default(event, this, ${JSON.stringify(params)})`,
        },
        {
            "type": "copy",
            "onclick": "copyRow(event, this)"
        }
        ];
        netlistListTable.row.add([curNetlist.name, curNetlist.transportMediumType, curNetlist.numPlatforms, curNetlist.netlistId, createActionDropdown(actions)]);
    }
    netlistListTable.draw(false);
}

/**
 * Opens the netlist dialog in copy form (it opens the selected netlist, but
 * doesn't set the netlistid)
 * 
 * @param event        The event from the click.
 * @param clickedLink  The dom object that the user clicked to perform the copy
 */
function copyRow(event, clickedLink)
{
    $(clickedLink).closest(".dropdown-menu").toggle();
    event.stopPropagation();
    var netlistId = netlistListTable.row(clickedLink.closest("tr")).data()[3];
    openNetlistDialog(netlistId, true);
}

/**
 * Opens the netlist modal window with the desired netlist information.
 * It also assigns the netlistId to the val for displayedId to keep track of the netlist that is open.
 * @param netlistId  {int} The netlist id (in the database) of the desired netlist.
 */
function openNetlistDialog(netlistId, copyRow)
{
    if (!copyRow)
    {
        $("#displayedId").val(netlistId);
    }
    else
    {
        $("#displayedId").val(-1);
    }
    populateNetlistDetails(netlistId);
}

/**
 * Finds the netlist that was clicked and runs the openNetlistDialog function with the id of the netlist that was clicked.
 */
function netlistClicked()
{
    var clickedData = netlistListTable.row(this).data();
    var netlistId = clickedData[3];
    openNetlistDialog(netlistId);
}

/**
 * Clears and hides the netlist modal
 */
function hide_netlist_modal()
{
    set_netlist_modal(undefined);
    $("#modal_netlist").modal("hide");
}

/**
 * Shows and sets the netlist modal details.
 * @param netlistDetails {object} The details of the netlist.
 * @param platformsList {array of objects} The list of all of the platforms, with a property for whether it's in the netlist or not.
 * @param tmTypes {array of strings} the list of transport medium types
 */
function show_netlist_modal(netlistDetails, platformsList, tmTypes)
{
    set_netlist_modal(netlistDetails, platformsList, tmTypes);
    $("#modal_netlist").modal("show");
}
/**
 * Sets the netlist modal details.
 * @param netlistDetails {object} The details of the netlist.
 * @param platformsList {array of objects } The list of all of the platforms, with a property for whether it's in the netlist or not.
 * @param tmTypes {array of strings} the list of transport medium types
 */
function set_netlist_modal(netlistDetails, platformsList, tmTypes)
{
    if (netlistDetails == null)
    {
        netlistDetails = {
                "name": "",
                "transportMediumType": ""
        }
    }
    $("#netlistName").val("");
    nlTable.clear();
    nlTable.search("");
    if (platformsList != undefined)
    {
        if (netlistName == undefined)
        {
            netlistName = "";
        }
        if ($("#displayedId").val().toString() != "-1")
        {
            $("#netlistTitle").text("Netlist - " + netlistDetails.name);
            $("#modal_netlist #modalTitle #modalSubTitle").html(` (Edit ${netlistDetails["name"]})`);
            $("#netlistName").val(netlistDetails.name);
            $("#netlistName").data("originalName", netlistDetails.name);
        }
        else
        {
            if (netlistDetails == null || netlistDetails.netlistId == null)
            {
                $("#modal_netlist #modalTitle #modalSubTitle").html(` (New)`);
            }
            else
            {
                $("#modal_netlist #modalTitle #modalSubTitle").html(` (Copy ${netlistDetails["name"]})`);
            }

        }
        if (netlistName != "")
        {
            $("#transportMediumTypeSelectbox").val(netlistDetails.transportMediumType);
            $("#transportMediumTypeSelectbox").trigger("change");
        }
        for (var key in platformsList)
        {
            var curPlatform = platformsList[key];
            var desc = (curPlatform.description != null ? curPlatform.description : "");
            var tmDataHtml = "";
            for (var key in curPlatform.transportMedia)
            {
                tmDataHtml += key + " - " + curPlatform.transportMedia[key] + "<br>";
            }

            var platformRow = [curPlatform.platformId, curPlatform.name, curPlatform.agency, tmDataHtml, curPlatform.config, desc];

            var newRow = nlTable.row.add(platformRow).select(curPlatform.inNetlist);
        }
    }
    nlTable.draw(true);
}

/** ******Events***** */
/**
 * Initializes all of the events on the webpage.
 * 
 * @returns
 */
function initializeEvents()
{
    $("#buttonAddNetlist").on("click", function(e){
        $("#transportMediumTypeSelectbox").trigger("change");
        openNetlistDialog(-1);
    });
    $('#modal_netlist').on('shown.bs.modal', function () {
        nlTable.columns.adjust();
    });
    $("#saveNetlistModalButton").on("click", function(e){
        set_yesno_modal("Save Schedule", 
                `Save Netlist?`, 
                `Are you sure you want to save this netlist?`, 
                "bg-info", 
                function() {

            var token = sessionStorage.getItem("token");
            var tsm = $("#transportMediumTypeSelectbox").val();
            var params = {
                    "name": $("#netlistName").val(),
                    "siteNameTypePref": $("#siteNameType").val(),
                    "transportMediumType": tsm,
                    "items": {}
            };
            //This will overwrite the saved netlist, or create a new one.  
            //If the netlist id exists, it will overwrite the saved one.
            if ($("#displayedId").val() != -1)
            {
                params["netlistId"] = $("#displayedId").val();
            }
            if (params.name == "")
            {
                show_notification_modal(
                        "Error", 
                        "This Netlist Does Not Have a Name", 
                        "In order to save this netlist, you must give it a name.", 
                        "OK", 
                        "bg-danger", 
                        "bg-secondary",
                        null);
                return;
            }
            if (params.transportMediumType == "")
            {
                show_notification_modal(
                        "Error", 
                        "No Transport Medium Type Selected", 
                        "In order to save this netlist, you must select a transport medium type.", 
                        null, 
                        "bg-danger", 
                        "bg-secondary",
                        null);
                return;
            }

            if (params.siteNameTypePref == "")
            {
                show_notification_modal(
                        "Error", 
                        "No Site Name Type Selected", 
                        "In order to save this netlist, you must select a site name type.", 
                        null, 
                        "bg-danger", 
                        "bg-secondary",
                        null);
                return;
            }

            var selectedRowData = nlTable.rows({selected: true}).data();
            for (var x = 0; x < selectedRowData.length; x++)
            {
                var curData = selectedRowData[x];
                var allPlatformTms = curData[3].split("<br>");
                var validTmType = false;
                allPlatformTms.forEach(ptm => {
                    if (ptm.startsWith(tsm))
                    {
                        validTmType = true;
                    }
                    else
                    {
                        goesTms.forEach(tm => {
                            if (ptm.startsWith(tm))
                            {
                                validTmType = true;
                            }
                        });
                    }
                    if (!validTmType)
                    {
                        console.log("Not valid.");
                    }
                });
                if (!validTmType)
                {
                    show_notification_modal("Error", 
                            "Transport Medium Type Mismatch", 
                            "There are platforms selected that do not match the selected Transport Medium Type.  Each selected platform must match the selected transport medium type.  Please correct this and then this netlist can be saved.", 
                            "OK", 
                            "bg-danger",
                            "bg-secondary",
                            null);
                    return;
                }

                //Need to guess at which TM ID to use.  There may be more than 
                //one per platform, but if they have GOES selected, or another 
                //type, we can assume they want to use that Transport Medium
                var selectedTransportMediaType = $("#transportMediumTypeSelectbox").val();
                var platformInfo = openDcsData.data.platformrefs.data[curData[1]];
                var allPlatformTmTypes = getTransportMediaTypes(platformInfo);

                var tmId = null;
                allPlatformTmTypes.forEach(tmType => {
                    if (tmId == null)
                    {    
                        if (selectedTransportMediaType == tmType)
                        {
                            tmId = platformInfo.transportMedia[tmType];
                        }
                        else if (goesTms.indexOf(tmType) != -1) 
                        {
                            tmId = platformInfo.transportMedia[tmType];
                        }
                    }
                });
                if (tmId != null)
                {
                    var siteName = getSiteName(platformInfo.siteId, $("#siteNameType").val());
                    params.items[tmId] = {
                            "description": curData[5],
                            "platformName": siteName,
                            "transportId": tmId
                    }
                }
                else
                {
                    show_notification_modal("Error", 
                            "Cannot find a Transport Medium Type", 
                            "Could not find a Transport Medium Type for one of the platforms in this netlist.  This would result in a corrupted netlist.  Please fix the issue and try again later.", 
                            "OK", 
                            "bg-danger",
                            "bg-secondary",
                            null);
                    return;
                }
            }

            $.ajax({
                url: `../api/gateway?opendcs_api_call=netlist&token=${token}`,
                type: "POST",
                headers: {     
                    "Content-Type": "application/json"   
                },

                data: JSON.stringify(params),
                success: function(response) {
                    hide_waiting_modal(500);
                    show_notification_modal("Save Netlist", 
                            "Netlist '" + response.name + "' Saved Successfully", 
                            "The " + response.name + " netlist has been saved successfully.", 
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
                    var errorJson = JSON.parse(response.responseText);
                    hide_waiting_modal(500);
                    show_notification_modal("Save Netlist", 
                            "Could not save Netlist", 
                            errorJson.errMessage, 
                            "OK", 
                            "bg-danger", 
                            "bg-secondary",
                            null);
                }
            });
            show_waiting_modal();
            hide_yesno_modal();
        }, 
        "bg-info", 
        null, 
        "bg-secondary");
        show_yesno_modal();
    });
}

/** **************Initialize Datatables******************* */
/**
 * Initializses all of the datatables on the webpage.
 * 
 * @returns
 */
function initializeDataTables()
{
    netlistListTable = $("#table").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'Bflrtip',

                "buttons": [
                    {
                        text: '+',
                        action: function ( e, dt, node, config ) {
                            $("#transportMediumTypeSelectbox").val("");
                            $("#transportMediumTypeSelectbox").trigger("change");
                            openNetlistDialog(-1);
                        },
                        className: "btn main-table-button"
                    }
                    ],
                    "columnDefs": [
                        {
                            "targets": [ 3 ],
                            "visible": false,
                            "searchable": false
                        }
                        ]

            });
    netlistListTable.init();
    netlistListTable.draw(false);
    show_waiting_modal();
    $('#table').on('click', 'tbody tr', netlistClicked);
    nlTable = $("#netlistTable").DataTable({
        "select": {
            "style": "multi+shift"
        },
        "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
        "pageLength": -1,
        "scrollY":     450,
        "scrollCollapse": true,
        "scrollResize": true,
        "autoWidth": true,

        initComplete: function () {
            $("#transportMediumTypeSelectbox").on("focus", function(e){
                $('#transportMediumTypeSelectbox').data('previousVal', $(this).val());
            });
            $("#transportMediumTypeSelectbox").on("change", function(e){
                var val = $(this).val();
                if (val != null && val != "")
                {
                    var selectedRowData = nlTable.rows({selected: true}).data();
                    for (var x = 0; x < selectedRowData.length; x++)
                    {
                        var curData = selectedRowData[x];
                        if (curData[3] != val && goesTms.indexOf(curData[3]) == -1 && goesTms.indexOf(val) == -1)
                        {
                            set_yesno_modal(
                                    "Transport Medium Type Mismatch", 
                                    "There can only be one Transport Medium Type for a netlist.", 
                                    "There are platforms selected that do not match the selected Transport Medium Type.  Each selected platform must match the selected transport medium type.  Would you like to automatically remove all of the platforms using the " + $("#transportMediumTypeSelectbox").data("previousVal") + " transport medium type?", 
                                    "bg-danger", 
                                    function(e) {
                                        nlTable.rows({selected: true}).deselect();
                                        $("#transportMediumTypeSelectbox").trigger("change");
                                    },
                                    "bg-secondary",
                                    function(e) {
                                        $("#transportMediumTypeSelectbox").val($("#transportMediumTypeSelectbox").data("previousVal"));
                                    },
                            "bg-secondary");
                            show_yesno_modal();
                            return;
                        }
                    }
                }
                filterTableBasedOnColumnVal("netlistTable", 3, val);
            });
        }
    });
    nlTable.on( 'select', function ( e, dt, type, indexes ) {
        if ($("#transportMediumTypeSelectbox").val() == "")
        {
            dt.rows(indexes[0]).deselect();
            show_notification_modal(
                    "Error", 
                    "No Transport Medium Type Selected", 
                    "In order to select a platform, you must first select a transport medium type in the 'Transport Medium Type' dropdown.", 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    null);
            return false;
        }

    });
    nlTable.init();
}
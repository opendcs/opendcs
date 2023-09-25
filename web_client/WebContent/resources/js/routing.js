//Setup module
//------------------------------

/**
 * A DataTable reference to the main datatable on the page.
 */
var routingTable;

/**
 * A DataTable reference to the properties table on the page.
 */
var propertiesTable;

/**
 * Stored propspecs for routing specs so that it does not need to be retrieved
 * multiple times.
 */
var routingPropSpecs;

/**
 * Inline options for the properties table.
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
 * Inline options for the platform selection table.
 */
var platformSelectionTableInlineOptions = {
        "columnDefs": [
            {
                "targets": [0],
                "type": "select",
                "data": null,
                "data": ["Netlist", "Platform Name", "Platform ID", "GOES Channel"]
            },
            {
                "targets": [1],
                "type": "input",
                "data": null,
                "bgcolor": {
                    "change": "#c4eeff"
                }
            }
            ]
};

/**
 * A DataTable reference to the properties table on the page.
 */
var platformSelectionTable;

/**
 * A reference to presentation groups retrieved from the API, stored so it does
 * not need to be retrieved later.
 */
var presentationGroupRefs = {};


/**
 * Runs an ajax call to get the routing spec references from the API
 * 
 * @returns
 */
function getRouting()
{
    var params = {
            "opendcs_api_call": "routingrefs"
    };
    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: params,
        success: function(response) {
            var allRouting = response;
            updateRoutingTable(allRouting);
            hide_waiting_modal(500);
        },
        error: function(response) {
            hide_waiting_modal(500);
        }
    });
}


/**
 * Gets the routing refs from the api and populates the routing table
 * accordingly.  It also redraws the table.
 * 
 * @param responseJson {Object} Contains reference data for all of the routing 
 * 								specs
 * @returns
 */
function updateRoutingTable(responseJson)
{
    routingTable.init();
    routingTable.clear();
    routingTable.draw(false);
    for (var routingKey in responseJson)
    {
        var curRouting = responseJson[routingKey];
        if (curRouting.name == null)
        {
            curRouting.name = "";
        }
        if (curRouting.dataSource == null)
        {
            curRouting.dataSource = "";
        }
        if (curRouting.consumer == null)
        {
            curRouting.consumer = "";
        }
        if (curRouting.lastModified == null)
        {
            curRouting.lastModified = "";
        }
        
        var params = {
                "objectType": "routing",
                "objectTypeDisplayName": "Presentation Group",
                "objectIdIndex": 0,
                "objectNameIndex": 1,
                "urlIdName": "routingid"
        };

        var actions = [{
            "type": "delete",
            "onclick_deprecated": "openDeleteModal(event, this)",
            "onclick": `deleteOpendcsObject_default(event, this, ${JSON.stringify(params)})`
        },{
            "type": "copy",
            "onclick": "copyRow(event, this)"
        }];

        routingTable.row.add([curRouting.routingId, 
        	curRouting.name, 
        	curRouting.dataSourceName, 
        	curRouting.destination, 
        	curRouting.lastModified, 
        	createActionDropdown(actions)]);
    }
    routingTable.draw(false);
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

    openRoutingDialog(clickedLink.closest("tr"), true);

}

/**
 * Performs the process for opening the main dialog.  This happens when either
 * the user clicks new, copy, or selects a row to be edited
 * 
 * @param rowClicked If a row is clicked, this will be populated with that row.
 *                   null if the new button is clicked. 
 * @param copyRow       true if the user selects copy on a new row.
 * @returns
 */
function openRoutingDialog(rowClicked, copyRow)
{
    var routingData = null

    clearRoutingDialog();

    var data; 
    if (rowClicked == null)
    {
        data = null;
        $("#displayedRoutingSpecId").attr("value", "-1");
    }
    else if (!copyRow)
    {
        data = routingTable.row(this).data();
        $("#displayedRoutingSpecId").attr("value", data[0]);
        $("#routingName").prop("readonly", true);
    }
    else
    {
        data = routingTable.row(rowClicked).data();
        $("#displayedRoutingSpecId").attr("value", "-1");

    }

    var params = {};

    if (data != null)
    {
        params["routingid"] = data[0];
        params["opendcs_api_call"] = "routing";
        $.ajax({
            //url: "/OHydroJson/routing",
            url: `../api/gateway`,
            type: "GET",
            data: params,
            success: function(response) {
                var routingSpecDetails = response;
                addTimezonesToSelect("timeZoneSelect", response.outputTZ);
                $("#applyToSelect").val(routingSpecDetails.applyTimeTo);
                $("#ascendingTimeOrderCheckbox").prop("checked", routingSpecDetails.ascendingTime);

                var routingId = $("#displayedRoutingSpecId").attr("value");
                if (routingId != null && routingId.toString() != "-1")
                {
                    $("#routingName").prop("readonly", true);
                    $("#routingName").val(routingSpecDetails.name);
                }
                else
                {
                    $("#routingName").prop("readonly", false);
                    $("#routingName").val("");
                }

                $("#dataSourceSelect").val(routingSpecDetails.dataSourceId);
                $("#destinationSelect").val(routingSpecDetails.destinationType);
                $("#destinationSelect").trigger("change");
                $("#destinationArg").val(routingSpecDetails.destinationArg);
                $("#outputFormatSelect").val(routingSpecDetails.outputFormat);
                if (routingSpecDetails.presGroupName != null)
                {
                    $("#presentationGroupSelect").val(presentationGroupRefs[routingSpecDetails.presGroupName]);
                }
                else
                {
                    $("#presentationGroupSelect").val("");
                }
                updateSwitchValue("inlineComputationsCheckbox", routingSpecDetails.enableEquations);
                updateSwitchValue("isProductionCheckbox", routingSpecDetails.production);
                updateSwitchValue("goesSelfTimedCheckbox", routingSpecDetails.goesSelfTimed);
                updateSwitchValue("goesRandomCheckbox", routingSpecDetails.goesRandom);
                updateSwitchValue("qualityNotificationsCheckbox", routingSpecDetails.qualityNotifications);
                updateSwitchValue("goesSpacecraftCheckbox", routingSpecDetails.goesSpacecraftCheck);
                $("#goesSpacecraftCheckbox").trigger("change");
                $("#goesSpacecraftSelector").val(routingSpecDetails.goesSpacecraftSelection.toLowerCase());
                updateSwitchValue("iridiumCheckbox", routingSpecDetails.iridium);
                updateSwitchValue("networkModemDcpCheckbox", routingSpecDetails.networkDCP);
                updateSwitchValue("parityCheckbox", routingSpecDetails.parityCheck);
                $("#parityCheckbox").trigger("change");
                $("#paritySelector").val(routingSpecDetails.paritySelection.toLowerCase());

                var propSpecMeta = {};
                for (var propSpecObj of routingPropSpecs)
                {
                    if (!(propSpecObj.name in routingSpecDetails.properties))
                    {
                        routingSpecDetails.properties[propSpecObj.name] = "";
                    }
                    propSpecMeta[propSpecObj.name] = {
                            "hover": propSpecObj.description,
                            "type": propSpecObj.type
                    }
                }

                for (var key in routingSpecDetails.properties)
                {
                    var actions = [{
                        "type": "delete",
                        "onclick": null
                    }];
                    var newRow = [key, routingSpecDetails.properties[key], createActionDropdown(actions)];
                    propertiesTable.row.add(newRow);
                }

                if (routingSpecDetails.netlistNames != null)
                {
                    for (var x = 0; x < routingSpecDetails.netlistNames.length; x++)
                    {
                        var curNetlistName = routingSpecDetails.netlistNames[x];
                        var actions = [{
                            "type": "delete",
                            "onclick": null
                        }];
                        platformSelectionTable.row.add(["Netlist", curNetlistName, createActionDropdown(actions)]);
                    }
                }
                if (routingSpecDetails.platformNames != null)
                {
                    for (var x = 0; x < routingSpecDetails.platformNames.length; x++)
                    {
                        var curPn = routingSpecDetails.platformNames[x];
                        var actions = [{
                            "type": "delete",
                            "onclick": null
                        }];
                        platformSelectionTable.row.add(["Platform Name", curPn, createActionDropdown(actions)]);
                    }
                }

                if (routingSpecDetails.platformIds != null)
                {
                    for (var x = 0; x < routingSpecDetails.platformIds.length; x++)
                    {
                        var curPid = routingSpecDetails.platformIds[x];
                        var actions = [{
                            "type": "delete",
                            "onclick": null
                        }];
                        platformSelectionTable.row.add(["Platform ID", curPid, createActionDropdown(actions)]);
                    }
                }

                if (routingSpecDetails.goesChannels != null)
                {
                    for (var x = 0; x < routingSpecDetails.goesChannels.length; x++)
                    {
                        var curGc = routingSpecDetails.goesChannels[x];
                        var actions = [{
                            "type": "delete",
                            "onclick": null
                        }];
                        platformSelectionTable.row.add(["GOES Channel", curGc, createActionDropdown(actions)]);
                    }
                }

                platformSelectionTable.draw(false);
                makeTableInline("platformSelectionTable", platformSelectionTableInlineOptions);
                platformSelectionTable.draw(false);

                propertiesTable.draw(false); //Need to draw first so that the "td" elements can be found.
                makeTableInline("propertiesTable", propertiesTableInlineOptions);
                propertiesTable.draw(false);

                var rowCount = propertiesTable.rows().count();
                for (var x = 0; x < rowCount; x++)
                {
                    var rowData = propertiesTable.row(x).data();
                    var propName = rowData[0];
                    if (propSpecMeta[propName] != null)
                    {
                        $(propertiesTable.row(x).node()).attr("title", propSpecMeta[propName].hover);
                    }
                }

                updateSince(routingSpecDetails.since);
                updateUntil(routingSpecDetails.until, routingSpecDetails.settlingTimeDelay);

                $("#modal_success").modal("show");
            },
            error: function(response) {
                hide_waiting_modal(500);
            }
        });
    }
    else
    {
        $("#modal_success").modal("show");
    }
}

/**
 * Controls/updates the "since" portion of the Date/Time section of the modal.
 *  
 * @param value  {string} The value for the left side of the Date/Time section.
 *                        This is the "type" (calendar, now, now -, etc.).
 * @returns
 */
function updateSince(value)
{
    if (value.startsWith("now -"))
    {
        var splitVal = value.split(" - ");
        $("#sinceSelect").val("nowminus");
        $("#sinceNowMinusSelect").val(splitVal[1]);
    }
    else if (value.startsWith("filetime("))
    {
        $("#sinceSelect").val("filetime");
        var trimmedValue = value.replace("filetime(", "")
        trimmedValue = trimmedValue.substr(0, trimmedValue.length-1);
        $("#sinceFileTextbox").val(trimmedValue);
    }
    else //calendar
    {
        $("#sinceSelect").val("calendar");
        var splitOnSlash = value.split("/");
        var year = splitOnSlash[0];
        var dayOfYearSplit = splitOnSlash[1].split(" ");
        var dayOfYear = dayOfYearSplit[0];
        var timeSplit = dayOfYearSplit[1].split(":");
        var hours = timeSplit[0];
        var minutes = timeSplit[1];
        var dt = dateFromDay(year, dayOfYear);
        dt.setHours(hours);
        dt.setMinutes(minutes);
        var dtString = getDashDatestringFromDate(dt);//`${dt.getUTCFullYear()}-${(dt.getUTCMonth()+1).toString().padStart(2, '0')}-${(dt.getUTCDate()).toString().padStart(2, '0')}T${dt.getHours().toString().padStart(2, '0')}-${dt.getMinutes().toString().padStart(2, '0')}`; 
        $("#sinceCalendar").val(dtString);
    }
    $("#sinceSelect").trigger("change");
}


/**
 * Controls/updates the "until" portion of the Date/Time section of the modal.
 *  
 * @param value  {string} The value for the left side of the Date/Time section.
 *                        This is the "type" (calendar, now, now -, etc.).
 * @param timeDelay {boolean} The value for the checkbox enabling the 30
 *                            second delay to avoid duplicates.
 *                            
 * @returns
 */
function updateUntil(value, timeDelay)
{
    if (value == null)
    {
        //Time Delay should be a boolean value here.
        $("#untilSelect").val("realtime");
        $("#realTimeCheckbox" ).prop( "checked", timeDelay);
    }
    else if (value.startsWith("now -"))
    {
        var splitVal = value.split(" - ");
        $("#untilSelect").val("nowminus");
        $("#untilNowMinusSelect").val(splitVal[1]);
    }
    else if (value.startsWith("now"))
    {
        var splitVal = value.split(" - ");
        $("#untilSelect").val("now");
    }
    else //calendar
    {
        $("#untilSelect").val("calendar");
        var splitOnSlash = value.split("/");
        var year = splitOnSlash[0];
        var dayOfYearSplit = splitOnSlash[1].split(" ");
        var dayOfYear = dayOfYearSplit[0];
        var timeSplit = dayOfYearSplit[1].split(":");
        var hours = timeSplit[0];
        var minutes = timeSplit[1];
        var dt = dateFromDay(year, dayOfYear);
        dt.setHours(hours);
        dt.setMinutes(minutes);
        var dtString = getDashDatestringFromDate(dt);
        $("#untilCalendar").val(dtString);
    }
    $("#untilSelect").trigger("change");
}


/**
 * Enables/Disables the corresponding div when the dropdown changes value,
 * showing the user the input that they selected to use.
 * 
 * @param enabledValue {string} This is a string containing the correct type
 *                              (calendar, nowminus, filetime, etc)
 * @returns
 */
function sinceTimeChanged(enabledValue)
{
    $("#sinceNowMinusSelectDiv").addClass("displayNone");
    $("#sinceCalendarDiv").addClass("displayNone");
    $("#sinceFileDiv").addClass("displayNone");
    if (enabledValue == "nowminus")
    {
        $("#sinceNowMinusSelectDiv").removeClass("displayNone");
    }
    else if (enabledValue == "calendar")
    {
        $("#sinceCalendarDiv").removeClass("displayNone");
    }
    else if (enabledValue == "filetime")
    {
        $("#sinceFileDiv").removeClass("displayNone");
    }
}

/**
 * Enables/Disables the corresponding div when the dropdown changes value,
 * showing the user the input that they selected to use.
 * 
 * @param enabledValue {string} This is a string containing the correct type
 *                              (calendar, nowminus, filetime, etc)
 * @returns
 */
function untilTimeChanged(enabledValue)
{
    $("#untilNowTextDiv").addClass("displayNone");
    $("#untilNowMinusSelectDiv").addClass("displayNone");
    $("#untilCalendarDiv").addClass("displayNone");
    $("#untilRealTimeDiv").addClass("displayNone");
    if (enabledValue == "now")
    {
        $("#untilNowTextDiv").removeClass("displayNone");
    }
    else if (enabledValue == "nowminus")
    {
        $("#untilNowMinusSelectDiv").removeClass("displayNone");
    }
    else if (enabledValue == "calendar")
    {
        $("#untilCalendarDiv").removeClass("displayNone");
    }
    else if (enabledValue == "realtime")
    {
        $("#untilRealTimeDiv").removeClass("displayNone");
    }
}


/**
 * Clears the routing dialog to be populated by a new routing spec.
 * 
 * @returns
 */
function clearRoutingDialog()
{
    $("#routingName").val("");
    $("#routingName").prop("readonly", false);
    $("#destinationArg").val("");
    $("#timeZoneSelect").val("");
    $("#presentationGroupSelect").val("");
    updateSwitchValue("inlineComputationsCheckbox", false);
    updateSwitchValue("isProductionCheckbox", false);

    $("#sinceSelect").val("nowminus");
    $("#sinceSelect").trigger("change");
    $("#untilSelect").val("nowminus");
    $("#untilSelect").trigger("change");

    $("#realTimeCheckbox").prop("checked", false);
    $("#ascendingTimeOrderCheckbox").prop("checked", false);

    propertiesTable.init();
    propertiesTable.clear();
    propertiesTable.draw(false);

    platformSelectionTable.init();
    platformSelectionTable.clear();
    platformSelectionTable.draw(false);

    updateSwitchValue("goesSelfTimedCheckbox", false);
    updateSwitchValue("goesRandomCheckbox", false);
    updateSwitchValue("qualityNotificationsCheckbox", false);
    updateSwitchValue("goesSpacecraftCheckbox", false);
    updateSwitchValue("iridiumCheckbox", false);
    updateSwitchValue("networkModemDcpCheckbox", false);
    updateSwitchValue("parityCheckbox", false);
}


/**
 * Opens the save modal, which is simply a yes/no modal confirmation on 
 * whether the user actually wants to save the routing spec or not.
 * 
 * @returns
 */
function openSaveModal()
{

    set_yesno_modal("Save Routing Spec", 
            `Save Routing Spec?`, 
            `Are you sure you want to save this Routing Spec?`, 
            "bg-info", 
            function() {
        var propertiesData = getNonDeletedRowData("propertiesTable");
        var properties = {};
        for (var x = 0; x < propertiesData.length; x++)
        {
            var curRow = propertiesData[x];
            var curRowVal = curRow[1];
            if (curRowVal != "")
            {
                properties[curRow[0]] = curRowVal;
            }
        }

        var platformData = getNonDeletedRowData("platformSelectionTable");
        var goesChannels = [];
        var netlistNames = [];
        var platformIds = [];
        var platformNames = [];
        for (var x = 0; x < platformData.length; x++)
        {
            var curData = platformData[x];
            if (curData[0].toLowerCase() == "goes channel")
            {
                goesChannels.push(curData[1]);
            }
            else if (curData[0].toLowerCase() == "netlist")
            {
                netlistNames.push(curData[1]);
            }
            else if (curData[0].toLowerCase() == "platform id")
            {
                platformIds.push(curData[1]);
            }
            else if (curData[0].toLowerCase() == "platform name")
            {
                platformNames.push(curData[1]);
            }
        }

        var sinceVal = null;
        var untilVal = null;

        var sinceSelectVal = $("#sinceSelect").val();
        if (sinceSelectVal == "nowminus")
        {
            sinceVal = "now - " + $("#sinceNowMinusSelect").val();
        }
        else if (sinceSelectVal == "calendar")
        {
            var dt = new Date($("#sinceCalendar").val());
            var year = dt.getFullYear();
            var dayOfYear = getDayOfYear(dt);
            sinceVal = year.toString() + "/" + dayOfYear.toString() + " " 
            + dt.getHours().toString().padStart(2, '0') + ":" 
            + dt.getMinutes().toString().padStart(2, '0') + ":" 
            + dt.getSeconds().toString().padStart(2, '0');
        }
        else if (sinceSelectVal == "filetime")
        {
            var filetimeVal = $("#sinceFileTextbox").val();
            sinceVal = `filetime(${filetimeVal})`;
        }

        var untilSelectVal = $("#untilSelect").val();
        if (untilSelectVal == "now")
        {
            untilVal = "now";
        }
        else if (untilSelectVal == "nowminus")
        {
            untilVal = "now - " + $("#untilNowMinusSelect").val();
        } 
        else if (untilSelectVal == "calendar")
        {
            var dt = new Date($("#untilCalendar").val());
            var year = dt.getFullYear();
            var dayOfYear = getDayOfYear(dt);
            untilVal = year.toString() + "/" + dayOfYear.toString() + " " 
            + dt.getHours().toString().padStart(2, '0') 
            + ":" + dt.getMinutes().toString().padStart(2, '0') + ":" 
            + dt.getSeconds().toString().padStart(2, '0');
        }
        else if (untilSelectVal == "realtime")
        {
            untilVal = null;
        }

        var params = {
                "applyTimeTo": $("#applyToSelect").val(),
                "ascendingTime": $("#ascendingTimeOrderCheckbox").prop("checked"),
                "dataSourceId": $("#dataSourceSelect").val(),
                "dataSourceName": $("#dataSourceSelect option:selected").text(),
                "destinationArg": $("#destinationArg").val(),
                "destinationType": $("#destinationSelect").val(),
                "enableEquations": isSwitchChecked("inlineComputationsCheckbox"),
                "goesChannels": goesChannels,
                "goesRandom": isSwitchChecked("goesRandomCheckbox"),
                "goesSelfTimed": isSwitchChecked("goesSelfTimedCheckbox"),
                "goesSpacecraftCheck": isSwitchChecked("goesSpacecraftCheckbox"),
                "goesSpacecraftSelection": $("#goesSpacecraftSelector").val(),
                "iridium": isSwitchChecked("iridiumCheckbox"), 
                "name": $("#routingName").val(),
                "netlistNames": netlistNames,
                "networkDCP": isSwitchChecked("networkModemDcpCheckbox"),
                "outputFormat": $("#outputFormatSelect").val(),
                "outputTZ": $("#timeZoneSelect").val(),
                "parityCheck": isSwitchChecked("parityCheckbox"),
                "paritySelection": $("#paritySelector").val(),
                "platformIds": platformIds,
                "platformNames": platformNames,
                "presGroupName": $("#presentationGroupSelect option:selected").text(),
                "production": isSwitchChecked("isProductionCheckbox"),
                "properties": properties,
                "qualityNotifications": isSwitchChecked("qualityNotificationsCheckbox"),
                "settlingTimeDelay": $("#realTimeCheckbox").prop("checked"),
                "since": sinceVal,
                "until": untilVal
        };

        var routingId = $("#displayedRoutingSpecId").attr("value");
        if (routingId != null && routingId != -1)
        {
            params["routingId"] = parseInt(routingId, 10);
        }

        var token = sessionStorage.getItem("token");
        var url = `../api/gateway?token=${token}&opendcs_api_call=routing`;
        $.ajax({
            url: url,
            type: "POST",
            headers: {     
                "Content-Type": "application/json"   
            },
            data: JSON.stringify(params),
            success: function(response) {
                hide_waiting_modal(500);
                show_notification_modal("Save Routing Spec", 
                        "Routing Spec Saved Successfully", 
                        "The routing spec has been saved successfully.", 
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
                show_notification_modal("Save Routing Spec", 
                        "There was an error saving Routing Spec.", 
                        response.responseText.length < 300 ? response.responseText : response.responseText.substring(0,297) + "...", 
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
    "bg-info", 
    null, 
    "bg-secondary");
    show_yesno_modal();
}

document.addEventListener('DOMContentLoaded', function() {
    $("#addPlatformSelectionButton").on("click", function( e, dt, node, config ) {
        var actions = [{
            "type": "delete",
            "onclick": null
        }];
        addBlankRowToDataTable("platformSelectionTable", true, actions, platformSelectionTableInlineOptions);
    });

    $("#addPropertyButton").on("click", function ( e, dt, node, config ) {
        var actions = [{
            "type": "delete",
            "onclick": null
        }];
        addBlankRowToDataTable("propertiesTable", true, actions, propertiesTableInlineOptions);
    });

    var elems = Array.prototype.slice.call(document.querySelectorAll('.form-check-input-switchery'));
    elems.forEach(function(html) {
        var switchery = new Switchery(html);
    });


    $.ajax({
        url: `../api/gateway?opendcs_api_call=propspecs`,
        type: "GET",
        headers: {     
            "Content-Type": "application/json"
        },

        data: {
            "class": "decodes.consumer.TcpClientConsumer"
        },
        success: function(response) {
            routingPropSpecs = response;

        },
        error: function(response) {
        }
    });

    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: {
            "name": "outputformat",
            "opendcs_api_call": "reflists"
        },
        success: function(response) {

            for (var key in response.OutputFormat.items)
            {
                var curOf = response.OutputFormat.items[key];
                var newOption = $('<option>', {
                    value: curOf.value,
                    text: curOf.description
                });
                $("#outputFormatSelect").append(newOption);
            }
        },
        error: function(response) {
        }
    });


    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: {
            "name": "outputformat",
            "opendcs_api_call": "presentationrefs"
        },
        success: function(response) {

            for (var x = 0; x < response.length; x++)
            {
                var curPres = response[x];
                var newOption = $('<option>', {
                    value: curPres.groupId,
                    text: curPres.name
                });
                $("#presentationGroupSelect").append(newOption);
                presentationGroupRefs[curPres.name] = curPres.groupId;
            }
        },
        error: function(response) {
            console.log("Error getting outputformat presentation refs.");
        }
    });


    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: {
            "opendcs_api_call": "datasourcerefs"
        },
        success: function(response) {
            for (var key in response)
            {
                var curDataSource = response[key];
                var newOption = $('<option>', {
                    value: curDataSource.dataSourceId,
                    text: curDataSource.name
                });

                $("#dataSourceSelect").append(newOption);
            }
        },
        error: function(response) {
            hide_waiting_modal(500);
        }
    });

    routingTable = $("#routingTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'Bflrtip',
                "buttons": [
                    {
                        text: '+',
                        action: function ( e, dt, node, config ) {
                            openRoutingDialog(null);
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

    $('#routingTable').on('click', 'tbody tr', openRoutingDialog);
    getRouting();
    platformSelectionTable = $("#platformSelectionTable").DataTable(
            {
                "paging": false,
                "autoWidth": false,
                "scrollY":        100,
                "scrollCollapse": true,
                "dom": 'Blrt',
                "columnDefs": [
                    {"width": "30%", "targets": 0},
                    {"width": "70%", "targets": 0}
                    ],
                    "buttons": [],
                    
                    initComplete: function ()
                    {
                       
                    }
            });

    $('#modal_success').on('shown.bs.modal', function () {
        updateDataTableScroll("propertiesTable");
        updateDataTableScroll("platformSelectionTable");
    });

    platformSelectionTable.init();

    $("#sinceSelect").on("change", function(event) {
        sinceTimeChanged(this.value);
    });

    $("#untilSelect").on("change", function(event) {
        untilTimeChanged(this.value);
    });

    $("#goesSpacecraftCheckbox").on("change", function(){
        $("#goesSpacecraftSelector").attr("disabled", !this.checked);
    });

    $("#parityCheckbox").on("change", function(){
        $("#paritySelector").attr("disabled", !this.checked);
    });

    $("#destinationSelect").trigger("change");


    propertiesTable = $("#propertiesTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'Bflrtip',
                "searching": false,
                "ordering": false,
                "paging": false,
                "autoWidth": false,
                "buttons": [],
                "info": false,
                "scrollY": 100,
                "scrollCollapse": true,
                "autoWidth": true,
                "columnDefs": []

            });
    propertiesTable.draw();

    $("#saveRoutingModalButton").on("click", function() {
        openSaveModal();
    });
});

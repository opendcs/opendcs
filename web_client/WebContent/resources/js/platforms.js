//platforms.js
/**
 * A referenct to the main table on the page (DataTable)
 */
var platformsTable;

/**
 * A datatable reference to the sensor information table. 
 */
var sensorInformationTable;

/**
 * A datatable reference to the transport media table.
 */
var transportMediaTable;

/**
 * A datatable reference to the properties table.
 */
var propertiesTable;

/**
 * A datatable reference to the platformSensorProperties table.
 */
var platformSensorPropertiesTable;

/**
 * The platform data of the open platform.  This can be a complex data
 * structure, so it is stored here.
 */
var openPlatformData;

/**
 * A reference to the open config data, which can be complex.
 */
var openConfigData;

/**
 * Inline properties for the properties table.
 */

/**
 * OpenDCS reflists and propspecs are stored here.  This is an instance of
 * the OpenDcsData class, which is an object that helps get recurring 
 * API data from the OpenDCS API.
 */
var openDcsData;

document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded platforms.js.");

    initializeEvents();
    initializeDataTables();
    initializeElements();

    openDcsData = new OpenDcsData();
    show_waiting_modal();
    openDcsData.getData(["siterefs", "platformrefs", "configrefs", "reflists"], 
            function(classInstance, response) {

        var sitesOptionsGroups = {};
        classInstance.data.siterefs.data.forEach(site => {
            for (var key in site.sitenames)
            {
                var lcKey = key.toLowerCase();
                if (!(lcKey in sitesOptionsGroups))
                {
                    sitesOptionsGroups[lcKey] = [];
                }
                sitesOptionsGroups[lcKey].push({
                    "name": site.sitenames[key],
                    "siteId": site.siteId,
                    "siteNames": site.sitenames
                });
            }
        });
        for (var siteType in sitesOptionsGroups)
        {
            var curOptGroup = sitesOptionsGroups[siteType];
            var optGroupDom = $("<optgroup>").attr("label", siteType);
            curOptGroup.forEach(optGroup => {
                var hoverHtml = "";
                for (var key in optGroup.siteNames)
                {
                    hoverHtml += key + " - " + optGroup.siteNames[key] + "\n" 
                }
                var newOpt = $('<option>', {
                    text: optGroup.name,
                    "title": hoverHtml
                }).attr("site_id", optGroup.siteId);

                var options = optGroupDom.find("option");
                if (options.length <= 0)
                {
                    optGroupDom.append(newOpt);
                }
                else
                {
                    for (var x = 0; x < options.length; x++)
                    {
                        if (newOpt.text() <= options[x].text)
                        {
                            optGroupDom.find("option").eq(x).before(newOpt);
                            break;
                        }
                        if (x >= options.length -1)
                        {
                            optGroupDom.find("option").eq(x).after(newOpt);
                            break;
                        }
                    }
                }
            });
            $('#siteSelectbox').append(optGroupDom);
            $('#actualSiteSelectbox').append(optGroupDom.clone());
        }
        $('#siteSelectbox').select2({
            placeholder: 'Select a site',
            allowClear: true
        });
        $('#actualSiteSelectbox').select2({
            placeholder: 'Select a site',
            allowClear: true
        });

        classInstance.data.configrefs.data.forEach(conf => {
            var hoverHtml = "Platform Count: " + conf.numPlatforms.toString() + "\nDescription: " + conf.description;
            var newOpt = $('<option>', {
                text: conf.name,
                "title": hoverHtml
            }).attr("config_id", conf.configId);

            var options = $("#configSelectbox").find("option");
            if (options.length <= 0)
            {
                $("#configSelectbox").append(newOpt);
            }
            else
            {
                for (var x = 0; x < options.length; x++)
                {
                    if (newOpt.text() <= options[x].text)
                    {
                        $("#configSelectbox").find("option").eq(x).before(newOpt);
                        break;
                    }
                    if (x >= options.length -1)
                    {
                        $("#configSelectbox").find("option").eq(x).after(newOpt);
                        break;
                    }
                }
            }
        });

        $("#configSelectbox").select2({
            placeholder: 'Select a config',
            allowClear: true
        });
        updatePlatformsTable();

        var transportMediumTypes = Object.keys(classInstance.data.reflists.data.TransportMediumType.items);
        for (var x = 0; x < transportMediumTypes.length; x++)
        {
            var o = new Option(transportMediumTypes[x], transportMediumTypes[x]);
            $("#mediumTypeSelectbox").append(o);
        }

        hide_waiting_modal(500);
    },
    function(response) {
        hide_waiting_modal(500);
    }
    );
    openDcsData.getPropspecs(["decodes.db.Platform", "decodes.db.PlatformSensor"]);
});

/**
 * Updates the platforms table, starting with the platform refs, then moving to
 * the config refs, and then the transport media id.
 * @returns
 */
function updatePlatformsTable()
{
    platformsTable.init();
    platformsTable.clear();
    platformsTable.draw(false);
    for (var key in openDcsData.data.platformrefs.data)
    {
        var curPlatform = openDcsData.data.platformrefs.data[key];

        var params = {
                "objectType": "platform",
                "objectTypeDisplayName": "Platform",
                "objectIdIndex": 0,
                "objectNameIndex": 1,
                "urlIdName": "platformid"
        };

        var actions = [{
            "type": "delete",
            "onclick": `deleteOpendcsObject_default(event, this, ${JSON.stringify(params)})`
        },
        {
            "type": "copy",
            "onclick": "copyRow(event, this)"
        }];
        var curConfigName = "";
        for (var x = 0; x < openDcsData.data.configrefs.data.length; x++)
        {
            var cc = openDcsData.data.configrefs.data[x];
            if (cc.configId == curPlatform.configId)
            {
                curConfigName = cc.name;
                break;
            }
        }
        var description = curPlatform.description != null ? curPlatform.description : "";
        var transportId = "";
        if (curPlatform.transportMedia != null && Object.keys(curPlatform.transportMedia).length > 0)
        {
            transportId = curPlatform.transportMedia[Object.keys(curPlatform.transportMedia)[0]];
        }
        var newRow = [curPlatform.platformId, curPlatform.name, curPlatform.agency, transportId, curConfigName, "", description, createActionDropdown(actions)];
        platformsTable.row.add(newRow);
    }
    platformsTable.draw(false);
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
    openPlatformDialog(clickedLink.closest("tr"), true);
}

/**
 * Clears the platform dialog.
 * 
 * @returns
 */
function clearPlatformDialog()
{
    sensorInformationTable.init();
    sensorInformationTable.clear();
    sensorInformationTable.draw(false);

    propertiesTable.clearDataTable();
    
    transportMediaTable.init();
    transportMediaTable.clear();
    transportMediaTable.draw(false);

    $("#siteSelectbox").val("");
    $('#siteSelectbox').trigger('change');
    $("#designatorTextbox").val("");
    $("#configSelectbox").val("");
    $('#configSelectbox').trigger('change');
    $("#ownerAgencyTextbox").val("");
    $("#descriptionTextbox").val("");
    $("#displayedPlatformId").attr("value", "");
    $("#platformTitle").val("");
}

/**
 * Gets a platform sensor by id from a passed list of platform sensors.
 * 
 * @param platformSensors Array{Object} Platform sensor array of object.
 * @param id              {int} Id of the platform sensor from the database. 
 * @returns
 */
function getPlatformSensorById(platformSensors, id)
{
    for (var x = 0; x < platformSensors.length; x++)
    {
        var curPs = platformSensors[x];
        if (curPs.sensorNum == id)
        {
            return curPs;
        }
    }
    return null;
}

/**
 * Gets a sensor by id from a passed list of config sensors
 * 
 * @param configSensors {Array[Object]} Config sensor objects.
 * @param id            {int} Config Sensor Id from the database.
 * @returns
 */
function getConfigSensorById(configSensors, id)
{
    for (var x = 0; x < configSensors.length; x++)
    {
        var curCs = configSensors[x];
        if (curCs.sensorNumber == id)
        {
            return curCs;
        }
    }
    return null;
}

/**
 * Gets a site by id, based on a passed site list.
 * 
 * @param siteList {array} a list of sites. 
 * @param id       {int}   the database id for the site.
 * @returns
 */
function getSiteById(siteList, id)
{
    for (var x = 0; x < siteList.length; x++)
    {
        var curSite = siteList[x];
        if (curSite.siteId == id)
        {
            return curSite;
        }
    }
    return null;
}

/**
 * Populates the platform sensor information table.
 * 
 * @param sensorInformation  {Object} The data to be used to populate the table
 * @returns
 */
function populateSensorInformationTable(sensorInformation)
{
    openConfigData = sensorInformation;
    sensorInformation.configSensors.forEach(sensor => {
        var propString = "";
        var actualSiteString = "";
        var actualSiteId = "";
        var curPlatformSensor = getPlatformSensorById(openPlatformData.platformSensors, sensor.sensorNumber);
        var psMin = "";
        var psMax = "";
        var psProps = "";
        var psUsgsDdno = "";
        if (curPlatformSensor != null)
        {
            for (var key in curPlatformSensor.sensorProps)
            {
                propString += key + " - " + curPlatformSensor.sensorProps[key] + "<br>";
            }
            if (curPlatformSensor.actualSiteId != null)
            {
                actualSiteId = curPlatformSensor.actualSiteId;
                var siteData = getSiteById(openDcsData.data.siterefs.data, curPlatformSensor.actualSiteId);
                if (siteData != null)
                {
                    for (var key in siteData.sitenames)
                    {
                        var curSiteName = siteData.sitenames[key];
                        actualSiteString += key + " - " + curSiteName + "<br>";
                    }
                }
            }
            psProps = JSON.stringify(curPlatformSensor.sensorProps);
            if (curPlatformSensor.max != null)
            {
                propString += "max" + " - " + curPlatformSensor.max + "<br>";
                psMax = curPlatformSensor.max;
            }
            if (curPlatformSensor.min != null)
            {
                propString += "min" + " - " + curPlatformSensor.min + "<br>";
                psMin = curPlatformSensor.min;
            }
            if (curPlatformSensor.usgsDdno != null)
            {
                propString += "DDNO" + " - " + curPlatformSensor.usgsDdno + "<br>";
                psUsgsDdno = curPlatformSensor.usgsDdno;
            }
        }

        var newRow =   [sensor.sensorNumber, 
            sensor.sensorName, 
            actualSiteString, 
            propString, 
            actualSiteId,
            psProps,
            psMin,
            psMax,
            psUsgsDdno];

        sensorInformationTable.row.add(newRow);
    });

    sensorInformationTable.draw();
    updateDataTableScroll("sensorInformationTable");
    sensorInformationTable.draw();
}

/**
 * Populates the platform dialog with the passed data.
 * 
 * @param data    The data of the requested platform.  This data is used to
 *                populate the dialog.
 * @returns
 */
function populatePlatformDialog(data)
{
    clearPlatformDialog();

    propertiesTable.setPropspecs(openDcsData.propspecs["decodes.db.Platform"].data);
    
    openPlatformData = data;

    if (data != null)
    {
        $("#displayedPlatformId").attr("value", data.platformId);
        $("#descriptionTextbox").val(data.description);
        $("#ownerAgencyTextbox").val(data.agency);

        $("#designatorTextbox").val(data.designator);
        $("#platformTitle").val(data.name);

        updateSwitchValue("isProduction", data.production);

        var optsToSelect = $(`#siteSelectbox option[site_id="${data.siteId}"]`);
        if (optsToSelect.length > 0)
        {
            $(optsToSelect[0]).prop('selected', true);
        }
        $('#siteSelectbox').trigger('change');

        optsToSelect = $(`#configSelectbox option[config_id="${data.configId}"]`);
        if (optsToSelect.length > 0)
        {
            $(optsToSelect[0]).prop('selected', true);
        }
        $('#configSelectbox').trigger('change');

        var token = sessionStorage.getItem("token");
        var params = {
                "configid": data.configId,
                "opendcs_api_call": "config",
                "token": token
        }

        $.ajax({
            url: `../api/gateway`,
            type: "GET",
            data: params,
            success: function(response) {
                setTimeout(function(data) {
                    populateSensorInformationTable(data);
                }, 500, response);
            },
            error: function(response) {
                hide_waiting_modal(500);
            }
        });
        var actions = [{
            "type": "delete",
            "onclick": null
        }];
        
        propertiesTable.updateProps(data.properties);

        data.transportMedia.forEach(tm => {
            var actions = [{
                "type": "delete",
                "onclick": null
            }];

            changeObjectPropertyValueToNewValue(tm, null, "");

            var loggerType = tm.loggerType == null ? "" : tm.loggerType;
            var username = tm.username == null ? "" : tm.username;
            var password = tm.password == null ? "" : tm.password;

            var newRow = [tm.mediumType, 
                tm.mediumId, 
                tm.scriptName, 
                (tm.channelNum != null && tm.channelNum != "") ? "chan=" + tm.channelNum : "", 
                        tm.timezone, 
                        tm.timeAdjustment, 
                        tm.channelNum, 
                        getHhmmssFromSeconds(tm.assignedTime), 
                        getHhmmssFromSeconds(tm.transportInterval), 
                        getHhmmssFromSeconds(tm.transportWindow),
                        tm.preamble, 
                        loggerType, 
                        tm.doLogin, 
                        username, 
                        password,
                        tm.baud,
                        tm.parity,
                        tm.stopBits,
                        tm.dataBits,
                        createActionDropdown(actions)];
            changeArrayValueToNewValue(newRow, null, "");
            transportMediaTable.row.add(newRow);
        });
    }
}

/**
 * Clears the sensor information dialog.
 * 
 * @returns
 */
function clearSensorInfoDialog()
{
    $("#sensorNumberText").text("");
    $("#sensorNameText").text("");
    $("#sensorParamCodeText").html("");
    $("#actualSiteSelectbox").val("");
    $("#actualSiteSelectbox").trigger("change");
    $("#platformSpecificMinTextbox").val("");
    $("#platformSpecificMaxTextbox").val("");
    $("#configMinTextbox").val("");
    $("#configMaxTextbox").val("");
    platformSensorPropertiesTable.clear().draw();
}

/**
 * Performs the process for opening the sensor info dialog.  This happens when 
 * the user clicks a row in the platform sensor table.
 * 
 * @param rowClicked This will be the clicked row.
 * @returns
 */
function openSensorInfoDialog(rowClicked)
{
    clearSensorInfoDialog();
    if (rowClicked != null)
    {
        var rowData = sensorInformationTable.row(this).data();
        if (rowData != null)
        {
            $("#displayedPlatformSensorRowId").data("value", sensorInformationTable.row(this).index());


            var configSensorData = getConfigSensorById(openConfigData.configSensors, rowData[0]);
            var platformSensorData = getPlatformSensorById(openPlatformData.platformSensors, rowData[0]);

            $("#sensorNumberText").text(rowData[0]);
            $("#sensorNameText").text(rowData[1]);

            var sensorParamCodes = "";
            for (var key in configSensorData.dataTypes)
            {
                sensorParamCodes += key + " - " + configSensorData.dataTypes[key] + "<br>";
            }

            $("#sensorParamCodeText").html(sensorParamCodes);

            var optsToSelect = $(`#actualSiteSelectbox option[site_id="${rowData[4]}"]`);
            if (optsToSelect.length > 0)
            {
                $(optsToSelect[0]).prop('selected', true);
            }
            $("#actualSiteSelectbox").trigger("change");

            $("#platformSpecificMinTextbox").val(rowData[6]);
            $("#platformSpecificMaxTextbox").val(rowData[7]);
            $("#usgsDdnoTextbox").val(rowData[8]);

            if (platformSensorData != null)
            {
                var sensProps = JSON.parse(rowData[5]);

                var actions = [{
                    "type": "delete",
                    "onclick": null
                }];
                setOpendcsPropertiesTable("platformSensorPropertiesTable", 
                        openDcsData.propspecs["decodes.db.PlatformSensor"].data, 
                        sensProps, 
                        true, 
                        null, 
                        actions);
            }
            if (configSensorData != null)
            {
                if (configSensorData.absoluteMin != null)
                {
                    $("#configMinTextbox").val(configSensorData.absoluteMin);
                }
                if (configSensorData.absoluteMax != null)
                {
                    $("#configMaxTextbox").val(configSensorData.absoluteMax);
                }
            }
            $("#modal_platformsensor").modal("show");
        }
    }
}

/**
 * Performs the process for opening the main dialog.  This happens when either
 * the user clicks new, copy, or selects a row to be edited
 * 
 * @param rowClicked If a row is clicked, this will be populated with that row.
 *                   null if the new button is clicked. 
 * @param copyRow    true if the user selects copy on a new row.
 * @returns
 */
function openPlatformDialog(rowClicked, copyRow)
{
    clearPlatformDialog();

    var params = {}
    if (rowClicked != null)
    {
        if (!copyRow)
        {
            $("#displayedPlatformId").attr("copy", false);
            data = platformsTable.row(this).data();
            params["platformid"] = data[0];
        }
        else
        {
            $("#displayedPlatformId").attr("copy", true);
            data = platformsTable.row(rowClicked).data();
            params["platformid"] = data[0];
            $("#platformTitle").text(data[1]);
        }

        var token = sessionStorage.getItem("token");
        show_waiting_modal();
        params["token"] = token;
        params["opendcs_api_call"] = "platform";
        $.ajax({
            url: `../api/gateway`,
            type: "GET",
            data: params,
            success: function(response) {
                setTimeout(function(data) {
                    populatePlatformDialog(data);
                    hide_waiting_modal(0);
                    $("#modal_platform").modal("show");
                }, 500, response);
            },
            error: function(response) {
                hide_waiting_modal(500);
            }
        });
    }
    else
    {
        $("#displayedPlatformId").attr("copy", false);
        populatePlatformDialog(null);
        $("#modal_platform").modal("show");
    }
}

/**
 * Clears the transport media dialog
 * 
 * @returns
 */
function clearTmDialog()
{
    $("#mediumTypeSelectbox").val("goes");
    $("#mediumTypeSelectbox").trigger("change");
    $("#dcpAddressTextbox").val("");
    $("#decodingScriptTextbox").val("");
    $("#timezoneTextbox").val("");
    $("#timeAdjustmentTextbox").val("");
    $("#channelNumTextbox").val("");
    $("#firstTransTimeTextbox").val("");
    $("#transmitIntervalTextbox").val("");
    $("#transmitDurationTextbox").val("");
    $("#preambleSelectbox").val("Unknown");
    $("#tcpUsername").val("");
    $("#tcpPassword").val("");
    $("#baud").val("");
    $("#paritySelectbox").val("");
    $("#stopBitsSelectbox").val("");
    $("#dataBitsSelectbox").val("");
}

/**
 * Opens the transport media dialog
 * 
 * @param clickedRow   A reference to the selected row.  It's null if a new 
 *                     row is clicked.
 * @returns
 */
function openTmDialog(clickedRow)
{
    clearTmDialog();
    var data = transportMediaTable.row(this).data(); //This is good enough to determine if it is a new item, or an edit to an existing one.
    if (data != null)
    {
        var rowIndex = transportMediaTable.row(this).index();
        $("#displayedTmRowIndex").val(rowIndex);
        $("#mediumTypeSelectbox").val(data[0]);
        $("#mediumTypeSelectbox").trigger("change");
        $("#dcpAddressTextbox").val(data[1]);
        $("#decodingScriptTextbox").val(data[2]);
        $("#timezoneTextbox").val(data[4]);
        $("#timeAdjustmentTextbox").val(data[5]);
        $("#channelNumTextbox").val(data[6]);
        $("#firstTransTimeTextbox").val(data[7]);
        $("#transmitIntervalTextbox").val(data[8]);
        $("#transmitDurationTextbox").val(data[9]);
        $("#preambleSelectbox").val(data[10]);
        $("#loggerTypeSelectbox").val(data[11]);

        var loginEnabled = data[12];
        if (loginEnabled != null)
        {
            updateSwitchValue("doLoginEnabled", loginEnabled);
        }
        $("#tcpUsername").val(data[13]);
        $("#tcpPassword").val(data[14]);
        $("#baud").val(data[15]);
        $("#paritySelectbox").val(data[16]);
        $("#stopBitsSelectbox").val(data[17]);
        $("#dataBitsSelectbox").val(data[18]);
    }

    $("#modal_transportmedia").modal("show");
}

/** ******Events***** */
function initializeEvents()
{
    $("#editConfig").on("click", function(e)
            {
        var selectedConfigId = parseInt($('#configSelectbox').find(":selected").attr("config_id"), 10);
        if (isNaN(selectedConfigId))
        {
            show_notification_modal("Edit Config", 
                    "No Selected Config", 
                    `There is no selected config to edit.  Please select a config and click 'edit' again..`, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    function() {
                hide_notification_modal();
            });
        }
        else
        {
            window.open(`configs?auto_load=true&config_id=${selectedConfigId}`, '_blank');
        }
            });

    $("#baud").on("focus", function()
            {
        this.value = '';
            });

    $("#mediumTypeSelectbox").on("change", function(e) {

        var mediumTypeSpecificDivs = $("div[data-mediumtype]");
        mediumTypeSpecificDivs.addClass("displayNone");
        var activeDiv = $(`div[data-mediumtype*='${this.value}']`);
        activeDiv.removeClass("displayNone");

        //<label data-mediumtype="data-logger,iridium,other,shef"
        $("label[data-mediumtype]").addClass("displayNone");
        activeLabel = $(`label[data-mediumtype*='${this.value}']`);
        activeLabel.removeClass("displayNone");

    });

    $("#modal_platform").on('shown.bs.modal', function(){
        updateDataTableScroll("sensorInformationTable");
        propertiesTable.updateDataTableScroll();
        updateDataTableScroll("transportMediaTable");
        updateDataTableScroll("platformSensorPropertiesTable");
    });

    $("#modal_platformsensor").on('shown.bs.modal', function(){
        platformSensorPropertiesTable.draw(false);
        updateDataTableScroll("platformSensorPropertiesTable");
    });

    $("#addPlatformSensorPropertyButton").on("click", function(e) {
        var action = [{
            "type": "delete",
            "onclick": null
        }];
        addBlankRowToDataTable("platformSensorPropertiesTable", true, action, propertiesTableInlineOptions);
        platformSensorPropertiesTable.draw(false);
    });

    $("#transportMediaModalCancel").on("click", function() {
        $("#displayedTmRowIndex").val("");
        $("#modal_transportmedia").modal("hide");
    });

    $("#platformSensorModalCancel").on("click", function() {
        $("#modal_platformsensor").modal("hide");
    });
    $("#transportMediaModalOk").on("click", function() {
        var tmRowIndex = $("#displayedTmRowIndex").val();

        var actions = [{
            "type": "delete",
            "onclick": null
        }];

        var newRowData = [$("#mediumTypeSelectbox").val(), 
            $("#dcpAddressTextbox").val(), 
            $("#decodingScriptTextbox").val(), 
            "chan=" + $("#channelNumTextbox").val(), 
            $("#timezoneTextbox").val(),
            $("#timeAdjustmentTextbox").val(),  
            $("#channelNumTextbox").val(), 
            $("#firstTransTimeTextbox").val(),
            $("#transmitIntervalTextbox").val(), 
            $("#transmitDurationTextbox").val(), 
            $("#preambleTextbox").val(), 
            $("#loggerTypeSelectbox").val(),
            isSwitchChecked("doLoginEnabled"),
            $("#tcpUsername").val(),
            $("#tcpPassword").val(),
            $("#baud").val(),
            $("#paritySelectbox").val(),
            $("#stopBitsSelectbox").val(),
            $("#dataBitsSelectbox").val(),
            createActionDropdown(actions)
            ];  
        changeArrayValueToNewValue(newRowData, null, "");
        if (tmRowIndex != null && tmRowIndex != "")
        {
            transportMediaTable.row(tmRowIndex).data(newRowData);
        }
        else
        {
            transportMediaTable.row.add(newRowData);
        }
        transportMediaTable.draw(false);
        $("#displayedTmRowIndex").val("");

        $("#modal_transportmedia").modal("hide");
    });
    $("#addTransportMediaButton").on("click", function() {
        openTmDialog(null);
    });
    $("#savePlatformModalButton").on("click", function() {
        var pn = $("#platformTitle").text();

        set_yesno_modal("Save Platform", 
                `Save ${pn}`, 
                `Are you sure you want to save the ${pn} platform?`, 
                "bg-info", 
                function() {
            var token = sessionStorage.getItem("token");
            var sensorInfoData = sensorInformationTable.data();
            var platformSensors = [];

            for (var x = 0; x < sensorInfoData.length; x++)
            {
                var curSensorData = sensorInfoData[x];
                var sensorProps = {};
                if (curSensorData[5] != '')
                {
                    sensorProps = JSON.parse(curSensorData[5]);
                }
                var newSensor = {
                        "sensorNum": curSensorData[0],
                        "sensorProps": sensorProps
                }
                if (curSensorData[4] != "")
                {
                    newSensor["actualSiteId"] = curSensorData[4];
                }
                if (curSensorData[6] != null && curSensorData[6] != '')
                {
                    newSensor["min"] = curSensorData[6]; 
                }
                if (curSensorData[7] != null && curSensorData[7] != '')
                {
                    newSensor["max"] = curSensorData[7]; 
                }
                if (curSensorData[8] != null && curSensorData[8] != '')
                {
                    newSensor["usgsDdno"] = curSensorData[8]; 
                }
                platformSensors.push(newSensor);
            }


            var transportMedia = [];

            var transportMediaData = getNonDeletedRowData("transportMediaTable");
            for (var x = 0; x < transportMediaData.length; x++)
            {
                var curTmRow = transportMediaData[x];

                var assignedTime = getSecondsFromHHMMSS(curTmRow[7]);
                var transportInterval = getSecondsFromHHMMSS(curTmRow[8]);
                var transportWindow = getSecondsFromHHMMSS(curTmRow[9]);

                var newTransportMedia = {
                        "assignedTime": assignedTime,
                        "baud": curTmRow[15],
                        "channelNum": curTmRow[6],
                        "dataBits": curTmRow[18],
                        "doLogin": curTmRow[12],
                        "loggerType": curTmRow[11],
                        "mediumId": curTmRow[1],
                        "mediumType": curTmRow[0],
                        "parity": curTmRow[16],
                        "password": curTmRow[14],
                        "scriptName": curTmRow[2],
                        "stopBits": curTmRow[17],
                        "timeAdjustment": curTmRow[5],
                        "timezone": curTmRow[4],
                        "transportInterval": transportInterval,
                        "transportWindow": transportWindow,
                        "username": curTmRow[13]
                //"preamble": curTmRow[10] //TODO: NEED THIS STILL
                };

                changeObjectPropertyValueToNewValue(newTransportMedia, "", null);
                transportMedia.push(newTransportMedia);
            }
            var props = {};

            var propData = propertiesTable.getNonDeletedRowData();
            
            
            for (var x = 0; x < propData.length; x++)
            {
                var curProp = propData[x];
                var propVal = curProp[1];
                if (propVal != null && propVal != "")
                {
                    props[curProp[0]] = curProp[1];
                }
            }
            var params = {
                    "agency": $("#ownerAgencyTextbox").val(),
                    "configId": parseInt($('#configSelectbox').find(":selected").attr("config_id"), 10),
                    "description": $("#descriptionTextbox").val(),
                    "designator": $("#designatorTextbox").val(),
                    "production": isSwitchChecked("isProduction"),
                    "siteId": parseInt($('#siteSelectbox').find(":selected").attr("site_id"), 10),
                    "platformSensors": platformSensors,
                    "transportMedia": transportMedia,
                    "properties": props
            }
            var pId = parseInt($("#displayedPlatformId").attr("value"), 10);
            var copy = $("#displayedPlatformId").attr("copy");
            if ((copy == "false") && !isNaN(pId))
            {
                params["platformId"] = pId;
            }
            $.ajax({
                url: `../api/gateway?token=${token}&opendcs_api_call=platform`,
                type: "POST",
                headers: {     
                    "Content-Type": "application/json"
                },
                data: JSON.stringify(params),
                success: function(response) {
                    hide_waiting_modal(500);
                    setTimeout(function() {show_notification_modal("Save Platform", 
                            "Platform saved successfully", 
                            `The platform ${response.name} has been saved successfully.`, 
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
                    handleErrorResponse(response, 
                            true, 
                            {
                        "title": "Error saving", 
                        "header": "Could not save the platform."
                            });
                }
            });
            show_waiting_modal();

        }, 
        "bg-info", 
        null, 
        "bg-secondary");


    });
}

/*****************Initialize Elements***********************/
function initializeElements()
{
    var elems = Array.prototype.slice.call(document.querySelectorAll('.form-check-input-switchery'));
    elems.forEach(function(html) {
        var switchery = new Switchery(html);
    });
}

/** **************Initialize Datatables******************* */
function initializeDataTables()
{
    platformsTable = $("#platformsTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'Bflrtip',
                "scrollCollapse": true,
                "autoWidth": true,
                "buttons": [
                    {
                        text: '+',
                        action: function ( e, dt, node, config ) {
                            openPlatformDialog(null);
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
    $('#platformsTable').on('click', 'tbody tr', openPlatformDialog);
    sensorInformationTable = $("#sensorInformationTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'flrtip',
                "searching": false,
                "ordering": true,
                "paging": false,
                "info": false,
                "buttons": [],
                "scrollY": 1,
                "scrollCollapse": true,
                "autoWidth": true,
                "columnDefs": [
                    {
                        "targets": [ 4,5,6,7,8 ],
                        "visible": false,
                        "searchable": false
                    }
                    ]

            });
    $('#sensorInformationTable').on('click', 'tbody tr', openSensorInfoDialog);
    transportMediaTable = $("#transportMediaTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'flrtip',
                "searching": false,
                "ordering": false,
                "paging": false,
                "info": false,
                "autoWidth": true,
                "buttons": [],
                "scrollY": 1,
                "scrollCollapse": true,
                "columnDefs": [
                    {
                        "targets": [ 4,5,6,7,8,9,10,11,12,13,14,15,16,17,18 ],
                        "visible": false,
                        "searchable": false
                    }
                    ]

            });

    $('#transportMediaTable').on('click', 'tbody tr', openTmDialog);

    propertiesTable = new PropertiesTable(
    		"propertiesTable", 
    		true);

    platformSensorPropertiesTable = $("#platformSensorPropertiesTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'Bflrtip',
                "searching": false,
                "ordering": true,
                "paging": false,
                "autoWidth": false,
                "info": false,
                "scrollCollapse": true,
                "scrollY": '200px',
                "autoWidth": true,
                "buttons": [],
                "columnDefs": []

            });

    $("#platformSensorModalOk").on("click", function(){
        var targetRowIndex = $("#displayedPlatformSensorRowId").data("value");
        var targetRowData = sensorInformationTable.row(targetRowIndex).data();
        var actualSiteId = $('#actualSiteSelectbox').find(":selected").attr("site_id");
        var platformMin = $("#platformSpecificMinTextbox").val();
        var platformMax = $("#platformSpecificMaxTextbox").val();
        var usgsDdno = $("#usgsDdnoTextbox").val();
        var sensorProps = {};
        var pspData = getNonDeletedRowData("platformSensorPropertiesTable");
        var propString = "";
        for (var x = 0; x < pspData.length; x++)
        {
            var curPspData = pspData[x];
            var sensPropValue = curPspData[1];
            if (sensPropValue != null && sensPropValue != "")
            {
                sensorProps[curPspData[0]] = curPspData[1];
                propString += curPspData[0] + " - " + curPspData[1] + "<br>";
            }
        }
        if (platformMin != "")
        {
            propString += "max" + " - " + platformMin + "<br>";
        }
        if (platformMax != "")
        {
            propString += "min" + " - " + platformMax + "<br>";
        }
        if (usgsDdno != "")
        {
            propString += "DDNO" + " - " + usgsDdno + "<br>";
        }
        var actualSiteString = "";
        if (actualSiteId != null)
        {
            var siteData = getSiteById(openDcsData.data.siterefs.data, actualSiteId);
            if (siteData != null)
            {
                for (var key in siteData.sitenames)
                {
                    var curSiteName = siteData.sitenames[key];
                    actualSiteString += key + " - " + curSiteName + "<br>";
                }
            }
        }
        if (actualSiteId == null)
        {
            actualSiteId = "";
        }
        targetRowData = [
            targetRowData[0],
            targetRowData[1],
            actualSiteString,
            propString,
            actualSiteId,
            JSON.stringify(sensorProps),
            platformMin,
            platformMax,
            usgsDdno
            ];
        sensorInformationTable.row(targetRowIndex).data(targetRowData);
        sensorInformationTable.draw(false);
        $("#modal_platformsensor").modal("hide");
    });

}
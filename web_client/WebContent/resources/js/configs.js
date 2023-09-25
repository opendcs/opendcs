//configs.js
//------------------------------
/**
 * Reference to the main table (DataTable).
 */
var configsTable;

/**
 * Stores all of the config refs once retrieved.
 */
var allConfigs = {};

/**
 * Properties table reference (DataTable)
 */
var propertiesTable;

/**
 * Stores the prop specs for config sensors once retrieved.
 */
var configSensorPropSpecs;

/**
 * Reference to the sensorProperties DataTable
 */
var sensorPropertiesTable;

/**
 * Reference to the decodingScript DataTable
 */
var decodingScriptTable;

/**
 * Reference to the formatStatement DataTable
 */
var formatStatementsTable;

/**
 * Reference to the sensorUnitConversions DataTable
 */
var sensorUnitConversionsTable;

/**
 * Stores the platform refs once retrieved from the API
 */
var platformRefs = {};

/**
 * Stores the tsdb properties once retrieved from the API.
 */
var tsdbProperties;

/**
 * For keeping track of the dragging of rows in the format statements table.
 */
var dragSrcRow;

/**
 * Inline options for the sensor unit conversion table.
 */
var sensorUnitConversionInlineOptions = {
        "columnDefs": [
            {
                "targets": [1,2,4,5,6,7,8,9],
                "type": "input",
                "data": null
            },
            {
                "targets": [3],
                "type": "select",
                "data": []
            }
            ]
};

/**
 * Keeps a reference of the open config data, which can be complex.
 */
var openConfig;

/**
 * Whether the open config is an edit or a new one.
 */
var editingConfig;

/**
 * Reference to the sensor DataTable.
 */
var sensorTable;

/**
 * Inline options for the properties datatable.
 */
var propertiesTableInlineOptions = {
        "columnDefs": [
            {
                "targets": [0,1],
                "type": "input",
                "data": null
            }
            ]
};

/**
 * Inline options for the format statements datatable.
 */
var formatStatementsTableInlineOptions = {
        "columnDefs": [
            {
                "targets": [1,2],
                "type": "input",
                "data": null
            }
            ]
};


/**
 * Initializing the recording mode cross refs (Single char to readable name).
 */
var recordingModeRefs = {
        "V": "Variable",
        "F": "Fixed"
}


/**
 * Gets the config refs for populating the main datatable.
 * 
 * @returns
 */
function getConfigs()
{
    var params = {
            "opendcs_api_call": "configrefs"
    };
    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: params,
        success: function(response) {
            allConfigs = response;
            updateConfigsTable(response);

            //Check if the page needs to auto load a config.
            var queryString = window.location.search;  
            var urlParams = new URLSearchParams(queryString);
            var autoLoad = urlParams.get("auto_load");
            var configId = urlParams.get("config_id");
            if (autoLoad != null && autoLoad.toLowerCase() == "true" && configId != null)
            {

                var autoLoadRow = getRowWithColumnData("configsTable", configId, 0);
                if (autoLoadRow != null)
                {
                    //Force pagination to be "all".  This makes sure the proper row will be visible and can be clicked.
                    configsTable.page.len(-1).draw();
                    $(autoLoadRow.row_html).click();

                }
                else
                {
                    show_notification_modal("Auto Load Config", 
                            "Auto Load Config Failed", 
                            `A config was supposed to be automatically loaded, but could not be loaded.  Please contact your system administrator.`, 
                            "OK", 
                            "bg-danger", 
                            "bg-secondary",
                            function() {
                        hide_notification_modal();
                    }
                    )
                }
            }

            hide_waiting_modal(500);
        },
        error: function(response) {
            show_notification_modal("Error retrieving config refs", 
                    "There was an issue getting the config refs from the OpenDCS REST API", 
                    `Please contact your system administrator.`, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    null);
            hide_waiting_modal(500);
        }
    });
}


/**
 * Clears the config table and updates it with the passed parameter data.
 * 
 * @param allConfigs An object containing all of the config data for the 
 *        datatable.
 * @returns
 */
function updateConfigsTable(allConfigs)
{
    configsTable.init();
    configsTable.clear();
    configsTable.draw(false);
    for (var configsKey in allConfigs)
    {
        var curConfig = allConfigs[configsKey];
        if (curConfig.name == null)
        {
            curConfig.name = "";
        }
        if (curConfig.equipmentId == null)
        {
            curConfig.equipmentId = "";
        }
        if (curConfig.numPlatforms == null)
        {
            curConfig.numPlatforms = "";
        }
        if (curConfig.description == null)
        {
            curConfig.description = "";
        }
        var params = {
                "objectType": "config",
                "objectTypeDisplayName": "Config",
                "objectIdIndex": 0,
                "objectNameIndex": 1,
                "urlIdName": "configid"
        };

        var actions = [{
            "type": "delete",
            "onclick_deprecated": "deleteSourceFromRow(event, this)",
            "onclick": `deleteOpendcsObject_default(event, this, ${JSON.stringify(params)})`
        },
        {
            "type": "copy",
            "onclick": "copyRow(event, this)"
        }];

        configsTable.row.add([curConfig.configId, curConfig.name, curConfig.equipmentId, curConfig.numPlatforms, curConfig.description, createActionDropdown(actions)]);
    }
    configsTable.draw(false);
}


/**
 * Clears the config dialog for reuse.
 * 
 * @returns
 */
function clearConfigsDialog()
{
    $("#configName").val("");
    $("#numPlatforms").val("0");
    $("#configDescription").val("");
    sensorTable.init();
    sensorTable.clear();
    sensorTable.draw(false);
    decodingScriptTable.init();
    decodingScriptTable.clear();
    decodingScriptTable.draw(false);
}

/**
 * opens the dialog for the selected record, but it's considered a new one
 * the configid is not set.
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
    openConfigDialog(clickedLink.closest("tr"), true);
}

/**
 * Performs the process for opening the main dialog.  This happens when either
 * the user clicks new, copy, or selects a row to be edited
 * 
 * @param rowClicked If a row is clicked, this will be populated with that row.
 *                   null if the new button is clicked. 
 * @param isCopyRow       true if the user selects copy on a new row.
 * @returns
 */
function openConfigDialog(rowClicked, isCopyRow)
{
    var thisObj;
    if (isCopyRow)
    {
        thisObj = rowClicked;
    }
    else
    {
        thisObj = this;
    }

    if (rowClicked != null)
    {
        //This determines if configId is passed into the save query.
        if (!isCopyRow)
        {
            editingConfig = true;
        }
        else
        {
            editingConfig = false;
        }
        var data = configsTable.row(thisObj).data();
        var token = sessionStorage.getItem("token");

        show_waiting_modal();

        var configId = data[0];

        $("#dcpAddressList").empty();
        for (var key in platformRefs)
        {
            var curPlatformRef = platformRefs[key];
            if (curPlatformRef.configId == configId)
            {
                for (var tmType in curPlatformRef.transportMedia)
                {
                    var tm = curPlatformRef.transportMedia[tmType];
                    $('#dcpAddressList').append(
                            $('<option />')
                            .text(tm)
                            .val(tm));
                }
            }
        }

        $.ajax({
            url: `../api/gateway`,
            type: "GET",
            data: {
                "opendcs_api_call": "config",
                "token": token,
                "configid": configId
            },
            success: function(response) {
                setTimeout(function(data) {
                    hide_waiting_modal(0);
                    show_config_modal(data);
                }, 500, response);
            },
            error: function(response) {
                show_notification_modal("Error retrieving config", 
                        "There was an issue getting the config from the OpenDCS REST API", 
                        `Please contact your system administrator.`, 
                        "OK", 
                        "bg-danger", 
                        "bg-secondary",
                        null);
                hide_waiting_modal(500);
            }
        });
    }
    else
    {
        editingConfig = false;
        show_config_modal(null);
    }
}

/**
 * Clears the decoding script dialog for use.
 * 
 * @returns
 */
function clearDecodingScriptDialog()
{
    formatStatementsTable.init();
    formatStatementsTable.clear();
    formatStatementsTable.draw(false);
    sensorUnitConversionsTable.init();
    sensorUnitConversionsTable.clear();
    sensorUnitConversionsTable.draw(false);
    $("#decodedDataDiv").empty();
    $("#sampleMessage").val("");
    $("#displayedDecodingScriptRowId").data("value", "");
    $("#scriptNameTextbox").val("");
    $("#dataOrderSelectbox").val($("#dataOrderSelectbox option")[0].value);
    $("#headerTypeSelectbox").val($("#headerTypeSelectbox option")[0].value);
    $("#dcpAddressTextbox").val("");
}

/**
 * Opens the decoding script dialog when the user clicks on a decoding script or
 * clicks new
 * 
 * @param rowClicked    A reference to the clicked row, which is what the 
 *                      dialog data will be based on.
 * @returns
 */
function openDecodingScriptDialog(rowClicked)
{
    var params = {};
    clearDecodingScriptDialog();
    var data = null;
    if (rowClicked != null)
    {       
        $("#displayedDecodingScriptRowId").data("value", decodingScriptTable.row(this).index());
        data = decodingScriptTable.row(this).data();
        $("#scriptNameTextbox").val(data[0]);
        var clickedScriptData = findObjectInListByPropValue(openConfig.scripts, "name", data[0]);
        var headerType = data[1];

        //Need to escape the colon for the query string.
        //This will select no value if the headerType is not available as an option.  Because it clears the dropdown above, it will essentially
        //select the default value of blank string (Decodes).
        $("#headerTypeSelectbox").find(`option[value=${headerType.replace(":", "\\:")}]`).prop("selected", true);
        if (clickedScriptData.index != -1)
        {
            $("#dataOrderSelectbox").val(clickedScriptData.object.dataOrder);
            //Sort the list by sequenceNum
            sortListByProperty(clickedScriptData.object.formatStatements, "sequenceNum", true); 
            var actions = [{
                "type": "delete",
                "onclick": null
            }];
            clickedScriptData.object.formatStatements.forEach(fs => {
                var newRow = ['<i class="move-cursor icon-arrow-resize8 mr-3 icon-1x"></i>', fs.label, fs.format, createActionDropdown(actions)];
                formatStatementsTable.row.add(newRow);
            });

            clickedScriptData.object.scriptSensors.forEach(ss => {
                var configSensor = findObjectInListByPropValue(openConfig.configSensors, "sensorNumber", ss.sensorNumber);
                if (configSensor.index != -1)
                {
                    var sensorName = configSensor != null ? configSensor.object.sensorName : "";
                    var newRow = [ss.sensorNumber, sensorName, ss.unitConverter.toAbbr, ss.unitConverter.algorithm, 
                        ss.unitConverter.a, ss.unitConverter.b, ss.unitConverter.c, ss.unitConverter.d, ss.unitConverter.e, ss.unitConverter.f];
                    sensorUnitConversionsTable.row.add(newRow);
                }
            });
        }

        sensorUnitConversionsTable.draw();
        sensorUnitConversion.runSensorUnitAlgoOnTable();
    }
    else
    {
        if (editingConfig)
        {
            for (var x = 0; x < openConfig.configSensors.length; x++)
            {
                var curConfigSensor = openConfig.configSensors[x];
                var newRow = [curConfigSensor.sensorNumber, curConfigSensor.sensorName, "raw", "none",0,0,0,0,0,0];
                sensorUnitConversionsTable.row.add(newRow);
            }
        }
        sensorUnitConversionsTable.draw();
    }
    show_decodingscript_modal(data);
}


/**
 * clears the sensor dialog to be ready to be populated with data.
 * 
 * @returns
 */
function clearSensorDialog()
{
    sensorPropertiesTable.init();
    sensorPropertiesTable.clear();
    sensorPropertiesTable.draw(false);
    $("#displayedConfigSensorRowId").data("value", "");
    $("#sensorNameTextbox").val("");
    $("#configurationText").text("");
    $("#sensorNumberText").text("");
    $("#standard1Selectbox").val($("#standard1Selectbox option")[0].value);
    $("#code1Textbox").val("");
    $("#standard2Selectbox").val($("#standard2Selectbox option")[0].value);
    $("#code2Textbox").val("");
    $("#standard3Selectbox").val($("#standard3Selectbox option")[0].value);
    $("#code3Textbox").val("");
    $("#usgsStatCodeTextbox").val("");
    $("#validRangeMin").val("");
    $("#validRangeMax").val("");
    $("#recordingModeSelectbox").val($("#recordingModeSelectbox option")[0].value);
    $("#firstSampleTime").val("00:00:00");
    $("#samplingInterval").val("00:00:00");
}

/**
 * When a sensor is clicked (or new one is), this will open the sensor dialog.
 * 
 * @param rowClicked
 * @returns
 */
function openSensorDialog(rowClicked)
{
    var params = {}
    clearSensorDialog();

    var nextSensorNum = getMaxFloatFromColumn("sensorTable", 0);
    if (nextSensorNum != null)
    {
        nextSensorNum++;
        $("#sensorNumberText").text(nextSensorNum);
    }
    else
    {
        nextSensorNum = 1;
    }
    $("#configurationText").text(openConfig.name);
    $("#displayedConfigSensorRowId").data("value", sensorTable.row(this).index());
    if (rowClicked != null)
    {
        var data = sensorTable.row(this).data();

        $("#sensorNumberText").text(data[0]);
        $("#sensorNameTextbox").val(data[1]);
        var standards = JSON.parse(data[12]);
        var counter = 1;
        for (var key in standards)
        {
            $(`#standard${counter}Selectbox`).val(key.toLowerCase());
            $(`#code${counter}Textbox`).val(standards[key]);
            counter++;
        }
        $("#usgsStatCodeTextbox").val(data[11]);
        $("#validRangeMin").val(data[7]);
        $("#validRangeMax").val(data[8]);


        for (var key in recordingModeRefs)
        {
            var recordingModeVal = recordingModeRefs[key];
            if (data[3].toLowerCase() === recordingModeVal.toLowerCase())
            {
                $("#recordingModeSelectbox").val(key);
            }
        }

        $("#firstSampleTime").val(getHhmmssFromSeconds(data[10]));

        $("#samplingInterval").val(getHhmmssFromSeconds(data[9]));

        var properties = JSON.parse(data[13]);

        var propSpecMeta = {};
        for (var propSpecObj of configSensorPropSpecs)
        {
            if (!(propSpecObj.name in properties))
            {
                properties[propSpecObj.name] = "";
            }
            propSpecMeta[propSpecObj.name] = {
                    "hover": propSpecObj.description,
                    "type": propSpecObj.type
            }
        }

        for (var key in properties)
        {
            var actions = [{
                "type": "delete",
                "onclick": null
            }];
            var newRow = [key, properties[key], createActionDropdown(actions)];
            sensorPropertiesTable.row.add(newRow);
        }

        sensorPropertiesTable.draw(false);
        makeTableInline("sensorPropertiesTable", propertiesTableInlineOptions);

        var rowCount = sensorPropertiesTable.rows().count();
        for (var x = 0; x < rowCount; x++)
        {
            var rowData = sensorPropertiesTable.row(x).data();
            var propName = rowData[0];
            if (propSpecMeta[propName] != null)
            {
                $(sensorPropertiesTable.row(x).node()).attr("title", propSpecMeta[propName].hover);
            }
        }
    }
    else
    {
        $("#sensorNumberText").text(nextSensorNum);

    }
    show_sensor_modal(data);
}

/**
 * Shows the sensor modal
 * 
 * @param sensorData
 * @returns
 */
function show_sensor_modal()
{
    $("#modal_configsensor").modal("show");
}


/**
 * Shows the decoding script modal.
 * 
 * @param dsData
 * @returns
 */
function show_decodingscript_modal()
{
    $("#modal_decodingscript").modal("show");
}

/**
 * Shows the config modal.
 * 
 * @param configData  The data for the config that will be displayed in the
 *                    modal.  This is used to populate the modal when opened.
 * @returns
 */
function show_config_modal(configData)
{
    if (configData != null)
    {
        openConfig = configData;
    }
    else
    {
        openConfig = {
                configSensors: [],
                description: "",
                name: "",
                numPlatforms: 0,
                scripts: []
        }
    }
    clearConfigsDialog();

    if (configData != null)
    {
        if (editingConfig)
        {
            $("#configName").val(configData.name);
        }
        $("#numPlatforms").val(configData.numPlatforms);
        $("#configDescription").val(configData.description);
        var configId = configData.configId;
        for (var x = 0; x < configData.configSensors.length; x++)
        {
            var curConfigSensor = configData.configSensors[x];

            var dataTypes = "";
            var samplingTimes = "????";
            var properties = "";
            var actions = [{
                "type": "delete",
                "onclick": null
            }];

            var recordingMode = recordingModeRefs[curConfigSensor.recordingMode];


            for (var key in curConfigSensor.dataTypes)
            {
                dataTypes += key + " - " + curConfigSensor.dataTypes[key] + "<br>";
            }

            for (var key in curConfigSensor.properties)
            {
                properties += key + " - " + curConfigSensor.properties[key] + "<br>";
            }

            samplingTimes = getHhmmssFromSeconds(curConfigSensor.timeOfFirstSample) + " " + getHhmmssFromSeconds(curConfigSensor.recordingInterval);

            var newRow = [curConfigSensor.sensorNumber,
                curConfigSensor.sensorName,
                dataTypes,
                recordingMode,
                samplingTimes,
                properties,
                createActionDropdown(actions),
                curConfigSensor.absoluteMax,
                curConfigSensor.absoluteMin,
                curConfigSensor.recordingInterval,
                curConfigSensor.timeOfFirstSample,
                curConfigSensor.usgsStatCode,
                JSON.stringify(curConfigSensor.dataTypes),
                JSON.stringify(curConfigSensor.properties)];
            changeArrayValueToNewValue(newRow, null, "")
            sensorTable.row.add(newRow);

        }
        sensorTable.draw(false);
        for (var x = 0; x < configData.scripts.length; x++)
        {
            var curScript = configData.scripts[x];
            var newRow = [curScript.name, curScript.headerType, createActionDropdown(actions)];
            decodingScriptTable.row.add(newRow);
        }
        decodingScriptTable.draw(false);
    }
    else
    {
        $("#configName").val(openConfig.name); //This was set by default.
    }

    $("#modal_config").modal("show");
}

/**
 * Builds out the decoding script json to be sent out.  This is built based
 * on the data on the decoding script dialog.
 * 
 * @returns
 */
function buildOpenDecodingScriptJson()
{
    var json = {
            "formatStatements": [],
            "scriptSensors": []
    };
    json.dataOrder = $("#dataOrderSelectbox").val();
    json.name = $("#scriptNameTextbox").val();
    json.headerType = $("#headerTypeSelectbox").val();

    var formatStatementData = getNonDeletedRowData("formatStatementsTable");
    for (var x = 0; x < formatStatementData.length; x++)
    {
        var curFs = formatStatementData[x];
        json.formatStatements.push({
            "format": curFs[2],
            "label": curFs[1],
            "sequenceNum": x
        });
    }
    var sucData = getNonDeletedRowData("sensorUnitConversionsTable");
    for (var x = 0; x < sucData.length; x++)
    {
        var curSucData = sucData[x];
        json.scriptSensors.push({
            "sensorNumber": curSucData[0],
            "unitConverter": {
                "algorithm": curSucData[3],
                "toAbbr": curSucData[2],
                "a": curSucData[4],
                "b": curSucData[5],
                "c": curSucData[6],
                "d": curSucData[7],
                "e": curSucData[8],
                "f": curSucData[9]
            }
        });
    }
    return json;
}

/**
 * Builds out the config object based mostly on all of the datatables and 
 * modal inputs.
 * @returns
 */
function buildConfigObject()
{
    var saveData = {
            configSensors: [],
            description: $("#configDescription").val(),
            name: $("#configName").val(),
            numPlatforms: $("#numPlatforms").val(),
            scripts: [],
            configId: openConfig.configId
    }

    var nonDeletedScripts = getNonDeletedRowData("decodingScriptTable");
    var allNames = openConfig.scripts.map(a => a.name)
    //Build non deleted script save object
    for (var x = 0; x < nonDeletedScripts.length; x++)
    {
        var curScript = nonDeletedScripts[x];
        var openConfigIndex = allNames.indexOf(curScript[0]);
        if (openConfigIndex != -1)
        {
            saveData.scripts.push(openConfig.scripts[openConfigIndex]);
        }
    }
    var sensorData = getNonDeletedRowData("sensorTable");
    for (var x = 0; x < sensorData.length; x++)
    {
        var curSensorRow = sensorData[x];

        var recordingModeAbbr = "";
        var recordingMode = curSensorRow[3];
        for (var key in recordingModeRefs)
        {
            var recordingModeVal = recordingModeRefs[key];
            if (recordingMode.toLowerCase() === recordingModeVal.toLowerCase())
            {
                recordingModeAbbr = key;
            }
        }

        var newSensorData = {
                dataTypes: JSON.parse(curSensorRow[12]),
                properties: JSON.parse(curSensorRow[13]),
                recordingInterval: curSensorRow[9],
                recordingMode: recordingModeAbbr,
                sensorName: curSensorRow[1],
                sensorNumber: curSensorRow[0],
                timeOfFirstSample: curSensorRow[10],
                usgsStatCode: curSensorRow[11]
        };

        var absMax = curSensorRow[7];
        if (absMax != "")
        {
            newSensorData["absoluteMax"] = absMax;
        }
        var absMin = curSensorRow[8];
        if (absMin != "")
        {
            newSensorData["absoluteMin"] = absMin;
        }

        saveData.configSensors.push(newSensorData);
    }
    return saveData;
}


document.addEventListener('DOMContentLoaded', function() {
    $("#addSensorPropertyButton").on("click", function(e) {
        var targetId = "sensorPropertiesTable";
        var action = [{
            "type": "delete",
            "onclick": null
        }];
        addBlankRowToDataTable(targetId, true, action, propertiesTableInlineOptions);
    });
    $("#addSensorButton").on("click", function ( e, dt, node, config ) {
        openSensorDialog(null);
    });
    $("#addDecodingScriptButton").on("click", function ( e, dt, node, config ) {
        openDecodingScriptDialog(null);
    });
    $.ajax({
        url: `../api/gateway?opendcs_api_call=propspecs`,
        type: "GET",
        headers: {     
            "Content-Type": "application/json"
        },

        data: {
            "class": "decodes.db.ConfigSensor"
        },
        success: function(response) {
            configSensorPropSpecs = response;

        },
        error: function(response) {
            show_notification_modal("Error retrieving propspecs", 
                    "There was an issue getting the propspecs from the OpenDCS REST API", 
                    `Please contact your system administrator.`, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    null);
        }
    });

    $.ajax({
        url: `../api/gateway`,
        data: {
            "opendcs_api_call": "platformrefs"
        },
        type: "GET",
        success: function(response) {
            platformRefs = response;
        },
        error: function(response) {
            show_notification_modal("Error retrieving platform refs", 
                    "There was an issue getting the platform refs from the OpenDCS REST API", 
                    `Please contact your system administrator.`, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    null);
        }
    });

    $("#decodingScriptModalOk").on("click", function(){
        console.log("Clicked ok button.");
        var decodingScriptJson = buildOpenDecodingScriptJson();
        $("#modal_decodingscript").modal("hide");
        var clickedRowId = $("#displayedDecodingScriptRowId").data("value");
        var actions = [{
            "type": "delete",
            "onclick": null
        }];
        var newRow = [decodingScriptJson.name, decodingScriptJson.headerType, createActionDropdown(actions)];
        if (clickedRowId === null || clickedRowId === "")
        {
            openConfig.scripts.push(decodingScriptJson);
            decodingScriptTable.row.add(newRow).draw(false);
        }
        else
        {
            var clickedRowData = decodingScriptTable.data()[clickedRowId];
            var searchedVal = findObjectInListByPropValue(openConfig.scripts, "name", clickedRowData[0]);
            if (searchedVal.index == -1)
            {
                show_notification_modal("Decoding Script Error.", 
                        "Could not find the proper decoding script.", 
                        `Please contact your system administrator.`, 
                        "OK", 
                        "bg-danger", 
                        "bg-secondary",
                        null);
            }
            else
            {
                openConfig.scripts[searchedVal.index] = decodingScriptJson;
                decodingScriptTable.row(clickedRowId).data(newRow).draw(false);
            }
        }

    });

    configsTable = $("#configsTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'Bflrtip',
                "scrollCollapse": true,
                "autoWidth": true,
                "columnDefs": [
                    {
                        "targets": [ 0 ],
                        "visible": false,
                        "searchable": false
                    }
                    ],
                    "buttons": [
                        {
                            text: '+',
                            action: function ( e, dt, node, config ) {
                                openConfigDialog(null);
                            },
                            className: "btn main-table-button"
                        }
                        ]

            });

    $('#configsTable').on('click', 'tbody tr', openConfigDialog);

    getConfigs();

    sensorTable = $("#sensorTable").DataTable(
            {
                "searching": false,
                "bPaginate": false,
                "dom": 'Bfrt',
                "scrollY":        "500px",
                "scrollCollapse": true,
                "autoWidth": true,
                "columnDefs": [
                    {
                        "targets": [ 7,8,9,10,11,12,13 ],
                        "visible": false,
                        "searchable": false
                    }
                    ],
                    "buttons": [],
                    initComplete: function ()
                    {
                    }
            });
    $('#sensorTable').on('click', 'tbody tr', openSensorDialog);

    decodingScriptTable = $("#decodingScriptTable").DataTable(
            {
                "searching": false,
                "bPaginate": false,
                "dom": 'Bfrt',
                "scrollY":        "500px",
                "autoWidth": true,
                "scrollCollapse": true,
                "columnDefs": [
                    { width: '5%', targets: 2 }
                    ],
                    initComplete: function ()
                    {
                    },
                    "buttons": []
            });

    $('#decodingScriptTable').on('click', 'tbody tr', openDecodingScriptDialog);

    sensorPropertiesTable = $("#sensorPropertiesTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'Bflrtip',
                "searching": false,
                "ordering": false,
                "paging": false,
                "autoWidth": true,
                "info": false,
                "scrollCollapse": true,
                "buttons": [],
                "columnDefs": []
            });

    formatStatementsTable = $("#formatStatementsTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'Bflrtip',
                "searching": false,
                "ordering": false,
                "paging": false,
                "autoWidth": false,
                "info": false,
                "buttons": [
                    {
                        text: '+',
                        action: function ( e, dt, node, config ) {
                            var action = [{
                                "type": "delete",
                                "onclick": null
                            }];
                            addBlankRowToDataTable("formatStatementsTable", true, action, formatStatementsTableInlineOptions);
                            formatStatementsTable.draw(false);
                            makeTableInline("formatStatementsTable", formatStatementsTableInlineOptions);
                            formatStatementsTable.draw(false);
                        },
                        className: "btn btn-secondary captionButton"
                    }
                    ],
                    "scrollCollapse": true,
                    "autoWidth": false,
                    createdRow: function ( row, data, dataIndex, cells ) {
                        $(row).attr('draggable', 'true');
                    },
                    order: [[0, "asc"]],
                    columnDefs: [
                        {
                            orderable: true, 
                            targets: 0
                        }
                        ],
                        drawCallback: function () {
                            openDcsRowDrag(this);
                        }
            });

    formatStatementsTable.draw(false);
    sensorUnitConversionsTable = $("#sensorUnitConversionsTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'flrtip',
                "searching": false,
                "ordering": false,
                "paging": false,
                "autoWidth": false,
                "info": false,
                //"scrollY": 1,
                "scrollCollapse": true,
                "autoWidth": false,
                "columnDefs": []
            });
    sensorUnitConversionsTable.draw(false);
    sensorUnitConversion = new SensorUnitConversion("sensorUnitConversionsTable", 3, 4, false, function(thisObject) {
        sensorUnitConversionInlineOptions.columnDefs[1].data = Object.keys(thisObject.openDcsUnitConversionAlgorithms);
    });

    $('#modal_success').on('shown.bs.modal', function () {
        sensorTable.columns.adjust();
    });

    $('#modal_success').on('shown.bs.modal', function () {
        decodingScriptTable.columns.adjust();
    });

    $("#modal_decodingscript").on('shown.bs.modal', function(){
        formatStatementsTable.draw(false);

        makeTableInline("formatStatementsTable", formatStatementsTableInlineOptions);

        sensorUnitConversionsTable.draw(false);
        makeTableInline("sensorUnitConversionsTable", sensorUnitConversionInlineOptions, null, function(e) {
            sensorUnitConversion.setSensorUnitAlgo(e);
        });
        sensorUnitConversionsTable.draw(false);
    });

    sensorTable.init();
    decodingScriptTable.init()

    $("#modal_config").on('shown.bs.modal', function(){
        sensorTable.draw(false);
        decodingScriptTable.draw(false);
        updateDataTableScroll("sensorTable");
        updateDataTableScroll("decodingScriptTable");
    });

    // Time picker
    $('#firstSampleTime').AnyTime_picker({
        format: '%H:%i:%S'
    });


    $('#samplingInterval').AnyTime_picker({
        format: '%H:%i:%S'
    });

    $("#decodingScriptModalCancel").on("click", function() {
        $("#modal_decodingscript").modal("hide");
    });

    $("#decodingScriptModalOk").on("click", function() {
    });

    $("#configSensorModalCancel").on("click", function() {
        $("#modal_configsensor").modal("hide");
    });

    $("#saveConfigsModalButton").on("click", function() {

        var n = $("#configName").val();

        set_yesno_modal("Save Platform", 
                `Save ${n}`, 
                `Are you sure you want to save the ${n} config?`, 
                "bg-info", 
                function() {

            var saveData = buildConfigObject();


            //Need to set openConfig to null on new config create.
            if (editingConfig)
            {
                saveData["configId"] = openConfig.configId;
            }
            else
            {
                delete saveData["configId"];
            }


            var token = sessionStorage.getItem("token");

            $.ajax({
                url: `../api/gateway?token=${token}&opendcs_api_call=config`,
                type: "POST",
                headers: {     
                    "Content-Type": "application/json"
                },
                data: JSON.stringify(saveData),
                success: function(response) {

                    setTimeout(function() {show_notification_modal("Save Config", 
                            "Config saved successfully", 
                            `The config ${response.name} has been saved successfully.`, 
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
                    set_notification_modal("There was an error saving the config", undefined, "The config could not be saved.", undefined, "bg-danger", "bg-danger");
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

    $("#configSensorModalOk").on("click", function() {

        var rowId = $("#displayedConfigSensorRowId").data("value");

        var dataTypes = {};
        var code1 = $("#code1Textbox").val();
        var code2 = $("#code2Textbox").val();
        var code3 = $("#code3Textbox").val();
        if (code1 != "")
        {
            dataTypes[$("#standard1Selectbox").val()] = code1;
        }
        if (code2 != "")
        {
            dataTypes[$("#standard2Selectbox").val()] = code2;
        }
        if (code3 != "")
        {
            dataTypes[$("#standard3Selectbox").val()] = code3;
        }

        var properties = {};
        var propData = getNonDeletedRowData("sensorPropertiesTable");
        for (var x = 0; x < propData.length; x++)
        {
            var curPropData = propData[x];
            var propVal = curPropData[1];
            if (propVal != "")
            {
                properties[curPropData[0]] = propVal;
            }
        }

        var actions = [{
            "type": "delete",
            "onclick": null
        }];

        var recordingMode = recordingModeRefs[$("#recordingModeSelectbox").val()];

        var newRowData = [
            $("#sensorNumberText").text(),
            $("#sensorNameTextbox").val(),
            keyValuePairToString(dataTypes, " - ", "<br>"),
            recordingMode,
            $("#firstSampleTime").val() + " " + $("#samplingInterval").val(),
            keyValuePairToString(properties, " - ", "<br>"),
            createActionDropdown(actions),
            $("#validRangeMin").val(),
            $("#validRangeMax").val(),
            getSecondsFromHHMMSS($("#samplingInterval").val()),
            getSecondsFromHHMMSS($("#firstSampleTime").val()),
            $("#usgsStatCodeTextbox").val(),
            JSON.stringify(dataTypes),
            JSON.stringify(properties)
            ];

        if (rowId !== null && rowId !== "")
        {
            sensorTable.row(rowId).data(newRowData);
        }
        else
        {
            sensorTable.row.add(newRowData);
        }

        sensorTable.draw(false);

        $("#modal_configsensor").modal("hide");

    });

    var token = sessionStorage.getItem("token");

    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: {
            "opendcs_api_call": "reflists",
            "name": "DataTypeStandard",
            "token": token
        },
        success: function(response) {
            var dataTypeStandards = response.DataTypeStandard.items;
            for (var key in dataTypeStandards)
            {
                var newOpt = $('<option>', {
                    text: key,
                    value: key,
                    title: dataTypeStandards[key].description
                });
                $("#standard1Selectbox").append(newOpt);
                $("#standard2Selectbox").append(newOpt.clone());
                $("#standard3Selectbox").append(newOpt.clone());
            }
        },
        error: function(response) {
        }
    });

    $.ajax({
        url: `../api/gateway`,
        crossDomain: true,
        type: "GET",
        data: {
            "opendcs_api_call": "reflists",
            "name": "TransportMediumType",
            "token": token
        },
        success: function(response) {
            allTransportMediumTypes = response.TransportMediumType.items;
            var responseJson = response;
            transportMediumTypes = Object.keys(responseJson.TransportMediumType.items);
            var blankO = new Option("", "Decodes");
            $("#headerTypeSelectbox").append(blankO);
            for (var x = 0; x < transportMediumTypes.length; x++)
            {
                var o = new Option(transportMediumTypes[x], `Decodes:${transportMediumTypes[x]}`);
                $("#headerTypeSelectbox").append(o);
            }
        },
        error: function(response) {
        }
    });

    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: {
            "opendcs_api_call": "tsdb_properties",
        },
        success: function(response) {
            tsdbProperties = response;
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
                            value: curDataSource.name,
                            text: curDataSource.name
                        });

                        $("#dataSourceSelect").append(newOption);

                        if ("api.datasource" in tsdbProperties)
                        {
                            var val = tsdbProperties["api.datasource"];
                            $(`#dataSourceSelect option[value="${val}"]`).prop('selected', true);
                        }
                    }
                },
                error: function(response) {
                    show_notification_modal("Error retrieving data source refs", 
                            "There was an issue getting the data source refs from the OpenDCS REST API", 
                            `Please contact your system administrator.`, 
                            "OK", 
                            "bg-danger", 
                            "bg-secondary",
                            null);
                    hide_waiting_modal(500);
                }
            });
        },
        error: function(response) {
            show_notification_modal("Error retrieving tsdb properties", 
                    "There was an issue getting the tsdb properties from the OpenDCS REST API", 
                    `Please contact your system administrator.`, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    null);
        }
    });

    $("#loadMessageButton").on("click", function(e) {
        var token = sessionStorage.getItem("token");

        show_waiting_modal();

        var token = sessionStorage.getItem("token");

        var tsdbProps = {"api.datasource": $( "#dataSourceSelect option:selected" ).text()};

        $.ajax({
            url: `../api/gateway?opendcs_api_call=tsdb_properties&token=${token}`,
            type: "POST",
            headers: {     
                "Content-Type": "application/json"
            },
            data: JSON.stringify(tsdbProps),
            success: function(response) {
                var token = sessionStorage.getItem("token");
                var tmId = $("#dcpAddressTextbox").val();
                var params = {
                        "tmid": tmId,
                        "opendcs_api_call": "message",
                        "token": token
                };
                var token = sessionStorage.getItem("token");
                $.ajax({
                    url: `../api/gateway`,
                    type: "GET",
                    data: params,
                    success: function(response) {
                        setTimeout(function(data) {
                            hide_waiting_modal(0);
                            var decodedMessage = atob(data.base64);
                            $("#sampleMessage").val(decodedMessage);
                        }, 500, response);
                    },
                    error: function(response) {
                        show_notification_modal("Load Message", 
                                "Error loading message", 
                                `The message could not be retrieved.  Please make sure you entered the correct information and retry.`, 
                                "OK", 
                                "bg-danger", 
                                "bg-secondary",
                                function() {
                            hide_notification_modal();
                        }
                        )
                        hide_waiting_modal(500);
                    }
                });
            },
            error: function(response) {
            }
        });
    });

    $("#decodeMessageButton").on("click", function(e) {
        var messageToDecode = $("#sampleMessage").val();
        params = {
                "rawmsg": {
                    "base64": btoa(messageToDecode)
                },
                "config": buildConfigObject()
        };
        var openDecodingJson = buildOpenDecodingScriptJson();
        var openScriptRowId = $("#displayedDecodingScriptRowId").data("value");
        if (openScriptRowId === null || openScriptRowId === "")
        {
            console.log("This decoding script needs to be saved before using the decoding feature.");
            return;
        }
        var clickedScriptName = decodingScriptTable.row(openScriptRowId).data()[0];
        var curScriptInfo = findObjectInListByPropValue(params.config.scripts, "name", $("#scriptNameTextbox").val());
        params["config"]["scripts"][curScriptInfo["index"]]["formatStatements"] = openDecodingJson["formatStatements"];
        params["config"]["scripts"][curScriptInfo["index"]]["scriptSensors"] = openDecodingJson["scriptSensors"];
        var token = sessionStorage.getItem("token");

        $.ajax({
            url: `../api/gateway?token=${token}&script=${clickedScriptName}&opendcs_api_call=decode`,
            type: "POST",
            headers: {     
                "Content-Type": "application/json"
            },
            data: JSON.stringify(params),
            success: function(response) {
                var jqTargetDiv = $("#decodedDataDiv");
                jqTargetDiv.empty();
                var tableId = "decodedDataTable";
                var newTableHtml = `<table id="${tableId}" class="table table-hover datatable-responsive">
                    <caption class="captionTitleCenter">Sensors</caption>
                    <thead>
                    <tr>
                    <th>Sensor</th>
                    </tr>
                    </thead>
                    <tbody>
                    </tbody>
                    </table>`;
                jqTargetDiv.append(newTableHtml);

                var uniqueTimestamps = [];
                for (var x = 0; x < response.timeSeries.length; x++)
                {
                    for (var y = 0; y < response.timeSeries[x].values.length; y++)
                    {
                        var curVal =  response.timeSeries[x].values[y];
                        var curTs = new Date(curVal.time.split("[")[0]);
                        var timeFound = false;
                        uniqueTimestamps.forEach(time => {
                            if (time.getTime() == curTs.getTime())
                            {
                                timeFound = true;
                            }
                        });
                        if (!timeFound)
                        {
                            uniqueTimestamps.push(curTs);
                        }
                    }
                }
                uniqueTimestamps.sort((a, b) => a - b);
                for (var x = 0; x < uniqueTimestamps.length; x++)
                {
                    var tsDate = uniqueTimestamps[x];
                    var tsString = tsDate.getUTCMonth().toString().padStart(2, "0") + "/"
                    + tsDate.getUTCDate().toString().padStart(2, "0") + "<br>"
                    + tsDate.getUTCHours().toString().padStart(2, "0") + ":"
                    + tsDate.getUTCMinutes().toString().padStart(2, "0");
                    $(`#${tableId} thead tr`).append(`<th>${tsString}</th>`);
                }
                var dt = $(`#${tableId}`).DataTable({
                    "dom": 'flrtip',
                    "searching": false,
                    "ordering": false,
                    "paging": false,
                    "info": false,
                    "buttons": [],
                    "autoWidth": false,
                    "columnDefs": []

                });

                for (var x = 0; x < response.timeSeries.length; x++)
                {
                    var newRow = new Array(uniqueTimestamps.length+1);
                    var curTimeseries = response.timeSeries[x];
                    newRow[0] = curTimeseries.sensorName;
                    if (curTimeseries.units != null)
                    {
                        newRow[0] += ` (${curTimeseries.units.replace("unknown", "?")})`;
                    }
                    for (var y = 0; y < curTimeseries.values.length; y++)
                    {
                        var curVal =  curTimeseries.values[y];
                        var curTs = new Date(curVal.time.split("[")[0]);
                        var idx = uniqueTimestamps.map(Number).indexOf(+curTs);
                        if (idx != -1)
                        {
                            var val = curVal.value;
                            if (val.indexOf(".") != -1)
                            {
                                val = parseFloat(val).toFixed(2).toString();
                            }
                            newRow[idx+1] = val;
                        }
                    }
                    //empty values must be assigned empty string.
                    for (var y = 0; y < newRow.length; y++)
                    {
                        var curVal = newRow[y];
                        newRow[y] = curVal == null ? "" : curVal;
                    }
                    dt.row.add(newRow);
                }

                dt.draw(false);

            },
            error: function(response) {
                alert(JSON.parse(response.responseText).errMessage);
            }
        });
    });

    $("#inputFile").on("change", getFile);      
});


/**
 * Allows the user to select a file for a message.
 * 
 * @param event   The event handler for the file moduule.  
 *                This is how the file is retrieved.
 * @returns
 */
function getFile(event) {
    const input = event.target
    if ('files' in input && input.files.length > 0) {
        placeFileContent(
                document.getElementById('sampleMessage'),
                input.files[0])
    }
}

/**
 * Places the file content into a target location.
 * 
 * @param target  the target location to place the file content.
 * @param file    File object.
 * @returns
 */
function placeFileContent(target, file) {
    readFileContent(file).then(content => {
        target.value = content
    }).catch(error => console.log(error))
}

/**
 * Reads a file that has been loaded into an input object.
 * 
 * @param file   The file from the input dom object.
 * @returns
 */
function readFileContent(file) {
    const reader = new FileReader()
    return new Promise((resolve, reject) => {
        reader.onload = event => resolve(event.target.result)
        reader.onerror = error => reject(error)
        reader.readAsText(file)
    })
}


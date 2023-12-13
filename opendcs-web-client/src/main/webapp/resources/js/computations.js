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

//computatoins.js
/**
 * Retains the list of algorithms for the page to use once retrieved.
 */
var algorithmList;
var mainTable;
var propertiesTable;
var parametersTable;
var algorithmTable;
var toFromGroup;

/**
 * This gets populated as the user clicks on the algorithmTable.  The api will 
 * be called for the class prop specs and will populate this object with the 
 * class name as the property and the response as the value. 
 */
var algorithmDetails = {}; 

/**
 * The actions at the side of the datatable: i.e. delete, copy. 
 */
var modalTableActions = [{
    "type": "delete",
    "onclick": null
}];

/**
 * The datatable inline options for the properties table.
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
 * The datatable inline options for the parameters table.  It gets populated
 * with actual data when certain data is retreived from the api.
 */
var parametersTableInlineOptions = {};

/**
 * Params for the main datatable actions.  Mostly this is so common code can
 * be used to perform the delete functionality.
 */
var params = {
        "objectType": "computation",
        "objectTypeDisplayName": "Computation",
        "objectIdIndex": 0,
        "objectNameIndex": 1,
        "urlIdName": "computationid"
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
 * Builds the list of versions in the paramaters data table.
 */
var versions = {};

/**
 * The OpenDcsData object.  Retrieves all the data you need at the beginning
 * of the page load so it can be used throughout the use of the page without
 * the need to retrieve common data multiple times.
 */
var openDcsData;

/*
 * Run this code when all of the content has been loaded.
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded computations.js.");
    openDcsData = new OpenDcsData();
    openDcsData.getData(["tsrefs"], 
            function(classInstance, response) {
        for (var x = 0; x < classInstance.data.tsrefs.data.length; x++)
        {
            var curTs = classInstance.data.tsrefs.data[x];
            var splitTsName = curTs.uniqueString.split(".");
            var version = splitTsName[splitTsName.length-1];
            if (!(version in versions))
            {
                versions[version] = 0;
            }
            versions[version]++;
        }
        classInstance.getData(["unitlist"], function(classInstance, response) {
            var params = {
                    "opendcs_api_call": "datatypelist"
            }
            $.ajax({
                url: `../api/gateway`,
                type: "GET",
                data: params,
                success: function(response) {
                    var dataTypeObjectList = response;
                    var params = {
                            "opendcs_api_call": "siterefs"
                    }
                    $.ajax({
                        url: `../api/gateway`,
                        type: "GET",
                        data: params,
                        success: function(response) {
                            var dataTypeList = [];
                            var sitesList = [];
                            for (var x = 0; x < response.length; x++)
                            {
                                var curSite = response[x];
                                if ("sitenames" in curSite)
                                {
                                    var sn = getParameterCaseInsensitive(curSite.sitenames, "cwms");
                                    if (sn != null)
                                    {
                                        sitesList.push(sn);
                                    }
                                }
                            }
                            sitesList.sort();
                            for (var x = 0; x < dataTypeObjectList.length; x++)
                            {
                                var curDtol = dataTypeObjectList[x];
                                dataTypeList.push(curDtol.code);
                                var splitDtol = curDtol.code.split("-");
                                if (splitDtol.length > 1)
                                {
                                    var base = splitDtol[0] + "-*";
                                    splitDtol.shift();
                                    var sub = "*-" + splitDtol.join("-");
                                    dataTypeList.push(base);
                                    dataTypeList.push(sub);
                                }
                            }

                            //set the inline options now that we have the data 
                            //back fromthe api
                            parametersTableInlineOptions = {
                                    "columnDefs": [
                                        {
                                            "targets": [0],
                                            "type": "input",
                                            "data": null,
                                            "bgcolor": {
                                                "change": "#c4eeff"
                                            }
                                        },
                                        {
                                            "targets": [1],
                                            "type": "searchable_select",
                                            "data": sitesList,
                                            "bgcolor": {
                                                "change": "#c4eeff"
                                            }
                                        },
                                        {
                                            "targets": [2],
                                            "type": "editable_select",
                                            "data": dataTypeList,
                                            "bgcolor": {
                                                "change": "#c4eeff"
                                            }
                                        },
                                        {
                                            "targets": [3],
                                            "type": "editable_select",
                                            "data": ["Inst", "Ave", "Min", 
                                                "Max", "Total"],
                                                "bgcolor": {
                                                    "change": "#c4eeff"
                                                }
                                        },
                                        {
                                            "targets": [4, 5],
                                            "type": "editable_select",
                                            "data": [
                                                "1 Minute", 
                                                "2 Minutes", 
                                                "3 Minutes", 
                                                "5 Minutes",
                                                "10 Minutes",
                                                "15 Minutes",
                                                "20 Minutes",
                                                "25 Minutes",
                                                "30 Minutes",
                                                "40 Minutes",
                                                "50 Minutes",
                                                "60 Minutes",
                                                "1 Hour", 
                                                "2 Hours", 
                                                "3 Hours", 
                                                "4 Hours", 
                                                "5 Hours", 
                                                "6 Hours", 
                                                "7 Hours", 
                                                "8 Hours", 
                                                "9 Hours", 
                                                "10 Hours", 
                                                "11 Hours", 
                                                "12 Hours", 
                                                "16 Hours", 
                                                "18 Hours", 
                                                "20 Hours", 
                                                "22 Hours", 
                                                "24 Hours", 
                                                "1 Day", 
                                                "2 Days", 
                                                "3 Days", 
                                                "4 Days", 
                                                "5 Days", 
                                                "6 Days", 
                                                "14 Days", 
                                                "21 Days", 
                                                "28 Days", 
                                                "35 Days", 
                                                "40 Days", 
                                                "50 Days", 
                                                "60 Days", 
                                                "100 Days", 
                                                "180 Days", 
                                                "200 Days", 
                                                "250 Days", 
                                                "300 Days", 
                                                "350 Days", 
                                                "365 Days",
                                                "1 Month",
                                                "2 Months",
                                                "3 Months",
                                                "4 Months",
                                                "5 Months",
                                                "6 Months",
                                                "7 Months",
                                                "8 Months",
                                                "9 Months",
                                                "10 Months",
                                                "11 Months",
                                                "12 Months" 
                                                ],
                                                "bgcolor": {
                                                    "change": "#c4eeff"
                                                }
                                        },
                                        {
                                            "targets": [6],
                                            "type": "searchable_select",
                                            "data": Object.keys(versions),
                                            "bgcolor": {
                                                "change": "#c4eeff"
                                            }
                                        },
                                        {
                                            "targets": [7],
                                            "type": "editable_select",
                                            "data": ["1 Minutes", "1 Seconds", "1 Minutes", "1 Hours", "1 Days", "1 Weeks", "1 Months", "1 Years"],
                                            "bgcolor": {
                                                "change": "#c4eeff"
                                            }
                                        },
                                        {
                                            "targets": [8],
                                            "type": "editable_select",
                                            "data": listOfObjectsToListByProperty(openDcsData.data["unitlist"].data, "abbr"),
                                            "bgcolor": {
                                                "change": "#c4eeff"
                                            }
                                        }
                                        ]
                            };
                        },
                        error: function(response) {
                            show_notification_modal("Site Refs Data Retreival", 
                                    "There was an issue getting site refs from the OpenDCS REST API", 
                                    `Please contact your system administrator.`, 
                                    "OK", 
                                    "bg-danger", 
                                    "bg-secondary",
                                    null);
                        }
                    });            
                },
                error: function(response) {
                    show_notification_modal("DataTypeList Data Retreival", 
                            "There was an issue getting the data types from the OpenDCS REST API", 
                            `Please contact your system administrator.`, 
                            "OK", 
                            "bg-danger", 
                            "bg-secondary",
                            null);
                }
            });
        });
    },
    function(response) {
        show_notification_modal("TS Ref Retreival", 
                "There was an issue getting the TS Refs from the OpenDCS REST API", 
                `Please contact your system administrator.`, 
                "OK", 
                "bg-danger", 
                "bg-secondary",
                null);
    });


    //Configures the effective Start and Effective End functionality (in the modal).
    toFromGroup = new ToFromGroup("#toFromGroup", ["nolimit", "nowminus", "calendar"], ["nolimit", "now", "nowplus", "calendar", "nowminus"], "", "", 
            function(e) {
    }, 
    function(e) {
    });

    //Opens the algorithm modal to be selected when the user clicks it.
    $("#algorithmNameButton").on("click", function(e) {
        $("#modal_algorithm").modal("show");
    });

    //populates the edit computation modal with the algorithm data when clicked.
    //pops up a warning if certain things will change, making the user
    //acknowledge it.
    $("#submitAlgoButton").on("click", function(e) {
        var selectedRow = algorithmTable.row({selected:  true});
        var selectedData = selectedRow.data();
        var algorithmId = selectedData[0];
        var algorithmName = selectedData[1];
        var params = {
                "opendcs_api_call": "algorithm",
                "algorithmid": algorithmId
        }
        //Get the clicked algorithm data and populate the modal with that data.
        $.ajax({
            url: `../api/gateway`,
            type: "GET",
            data: params,
            success: function(response) {
                var paramsToBeRemoved = [];
                var paramRowsToSave = [];
                var curParams = parametersTable.data();
                var curParamNames = curParams.map(function(x) 
                        {
                    return x[0]
                        });
                for (var x = 0; x < curParamNames.length; x++)
                {
                    var cpn = curParamNames[x];
                    var found = false;
                    for (var y = 0; y < response.parms.length; y++)
                    {
                        if (cpn == response.parms[y].roleName)
                        {
                            found = true;
                            paramRowsToSave.push(curParams[x]);
                            break;
                        }
                    }
                    if (!found)
                    {
                        paramsToBeRemoved.push(cpn);
                    }
                }
                var propsToBeRemoved = [];
                var propsToSave = {};
                var curProps = propertiesTable.data();
                var curPropNames = curProps.map(function(x) 
                        {
                    if (x[1] != null && x[1] != "")
                    {
                        return x[0]
                    }
                    return null;
                        });
                for (var x = 0; x < curPropNames.length; x++)
                {
                    var cpn = curPropNames[x];
                    if (!(cpn in response.props))
                    {
                        propsToBeRemoved.push(cpn);
                    }
                    else
                    {
                        propsToSave[curProps[x][0]] = curProps[x][1];
                    }
                }
                for (propName in response.props)
                {
                    if (!(propName in propsToSave))
                    {
                        propsToSave[propName] = response.props[propName]
                    }
                }

                var selectAlgoMessage = `Are you sure you want to select the ${algorithmName} algorithm?`;
                if (propsToBeRemoved.length > 0)
                {
                    selectAlgoMessage += `\nProperties to be removed: '${propsToBeRemoved.join(",")}'.`;
                }
                if (paramsToBeRemoved.length > 0)
                {
                    selectAlgoMessage += `\nParams to be removed: '${paramsToBeRemoved.join(",")}'.`;
                }
                set_yesno_modal("Select Algorithm", 
                        `Select ${algorithmName} Engineering Unit`, 
                        selectAlgoMessage, 
                        "bg-warning", 
                        function() {
                    var selectedRow = algorithmTable.row({selected:  true});
                    var selectedData = selectedRow.data();
                    var algorithmId = selectedData[0];
                    var algorithmName = selectedData[1];
                    var execClass = selectedData[2];
                    $("#algorithmNameButton").text(algorithmName);

                    propertiesTable.init();
                    propertiesTable.clear();
                    propertiesTable.draw(false);
                    var propSpecs = [];
                    if (execClass in algorithmDetails)
                    {
                        propSpecs = algorithmDetails[execClass];
                    }
                    setOpendcsPropertiesTable("propertiesTable", 
                            propSpecs, 
                            propsToSave, 
                            true, 
                            propertiesTableInlineOptions, 
                            modalTableActions);

                    parametersTable.init();
                    parametersTable.clear();
                    parametersTable.draw(false);

                    var paramRows = paramRowsToSave;
                    for (var y = 0; y < response.parms.length; y++)
                    {
                        var curParm = response.parms[y].roleName;
                        var found = false;
                        for (var x = 0; x < paramRowsToSave.length; x++)
                        {
                            var curPrts = paramRowsToSave[x];
                            var curName = curPrts[0];
                            if (curParm == curName)
                            {
                                found = true;
                            }
                        }
                        if (!found)
                        {
                            var newRow = [curParm, "", "", "", "", "", "", "", "", createActionDropdown(modalTableActions)];
                            paramRows.push(newRow);
                        }
                    }
                    for (var x = 0; x < paramRows.length; x++)
                    {
                        parametersTable.row.add(paramRows[x]);
                        makeTableInline("parametersTable", parametersTableInlineOptions);
                        parametersTable.draw(false);
                    }
                    makeTableInline("parametersTable", parametersTableInlineOptions);
                    parametersTable.draw(false);


                    hide_yesno_modal();
                    $("#modal_algorithm").modal("hide");

                }, 
                "bg-warning", 
                null, 
                "bg-secondary");
            },
            error: function(response) {
                show_notification_modal("Error retrieving algorithm", 
                        "There was an issue getting the algorithm from the OpenDCS REST API", 
                        `Please contact your system administrator.`, 
                        "OK", 
                        "bg-danger", 
                        "bg-secondary",
                        null);
            }
        });

    });

    params = {
            "opendcs_api_call": "apprefs"
    }
    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: params,
        success: function(response) {
            var processList = response;
            var optionAttributes = {
                    "value": "",
                    "id": "",
                    "comment": "",
                    "last_modified": ""
            };
            var newOption = $("<option>").attr(optionAttributes).html("(none)");
            $("#processSelectbox").append(newOption);
            for (var x = 0; x < processList.length; x++)
            {
                var curProcess = processList[x];
                optionAttributes = {
                        "value": curProcess.appName,
                        "id": curProcess.appId,
                        "comment": curProcess.comment,
                        "last_modified": curProcess.lastModified
                };

                var newOption = $("<option>").attr(optionAttributes).html(`${curProcess.appId}: ${curProcess.appName}`);
                $("#processSelectbox").append(newOption);
            }
        },
        error: function(response) {
            show_notification_modal("Error retrieving apprefs", 
                    "There was an issue getting the apprefs from the OpenDCS REST API", 
                    `Please contact your system administrator.`, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    null);
        }
    });

    params = {
            "opendcs_api_call": "tsgrouprefs"
    }
    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: params,
        success: function(response) {
            var tsGroupRefs = response;
            var optionAttributes = {
                    "value": "",
                    "id": "",
                    "description": "",
                    "type": ""
            };
            var newOption = $("<option>").attr(optionAttributes).html("");
            $("#groupSelectbox").append(newOption);
            for (var x = 0; x < tsGroupRefs.length; x++)
            {
                var curGroup = tsGroupRefs[x];
                optionAttributes = {
                        "value": curGroup.groupName,
                        "id": curGroup.groupId,
                        "description": curGroup.description,
                        "type": curGroup.groupType
                };
                var newOption = $(`<option title="${curGroup.description}">`).attr(optionAttributes).html(`${curGroup.groupType}: ${curGroup.groupName}`);
                $("#groupSelectbox").append(newOption);
            }
        },
        error: function(response) {
            show_notification_modal("Error retrieving ts group refs", 
                    "There was an issue getting the ts group refs from the OpenDCS REST API", 
                    `Please contact your system administrator.`, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    null);
        }
    });

    params = {
            "opendcs_api_call": "algorithmrefs"
    };
    //Ajax call to load the list of algorithms into the algorithm datatable.
    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: params,
        success: function(response) {
            algorithmList = response;
            for (var x = 0; x < algorithmList.length; x++)
            {
                var curRow = algorithmList[x];
                var newRow = [curRow.algorithmId, curRow.algorithmName, curRow.execClass, curRow.numCompsUsing, curRow.description];
                algorithmTable.row.add(newRow);
                algorithmTable.draw(false);
            }
            algorithmTable.draw(false);
        },
        error: function(response) {
            show_notification_modal("Error retrieving algorithm refs", 
                    "There was an issue getting the algorithm from the OpenDCS REST API", 
                    `Please contact your system administrator.`, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    null);
        }
    });

    //when clicked, the main table dialog is opened.
    $('#mainTable').on('click', 'tbody tr', beginOpenMainTableDialog);

    //Initialize the main datatable.
    mainTable = $("#mainTable").DataTable(
            {
                "dom": 'flrtip',
                "searching": true,
                "ordering": true,
                "paging": false,
                "info": false,
                "buttons": [],
                //"scrollY": 1,
                "scrollCollapse": true,
                "autoWidth": true,
                "columnDefs": [] 
            });

    //initialize the properties table.
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

    //initialize the algorithm table.
    algorithmTable = $("#algorithmTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'flrtip',
                "searching": true,
                "ordering": true,
                "paging": false,
                "autoWidth": true,
                "info": false,
                "scrollY": 1,
                "scrollCollapse": true,
                "buttons": [],
                "select": {
                    "style": "single"
                },
                "columnDefs": []
            });

    //When clicked, pops up the algorithm dialog, where an algorithm can be 
    //selected.
    $('#algorithmTable').on('click', 'tbody tr', function(e) {
        var clickedData = algorithmTable.row(this).data();
        var className = clickedData[2];
        var params = {
                "opendcs_api_call": "propspecs",
                "class": className
        };
        $.ajax({
            url: `../api/gateway`,
            type: "GET",
            data: params,
            success: function(response) {
                var paramString = this.url.split("?")
                var usp = new URLSearchParams(paramString[1]);
                var className = usp.get("class");
                algorithmDetails[className] = response;
            },
            error: function(response) {
                show_notification_modal("Error retrieving propspecs", 
                        "There was an issue getting the algorithm propspecs from the OpenDCS REST API", 
                        `Please contact your system administrator.`, 
                        "OK", 
                        "bg-danger", 
                        "bg-secondary",
                        null);
            }
        });
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

    //On show, redraw and reinitialize the view of these tables.
    //They cannot be initialized until they are visible (on the modal).
    $("#modal_algorithm").on('shown.bs.modal', function(){
        algorithmTable.draw();
        updateDataTableScroll("algorithmTable");
        algorithmTable.draw();
    });


    //add property row to the property table when clicked.
    $("#addPropertyButton").on("click", function() {
        var action = [{
            "type": "delete",
            "onclick": null
        }];
        addBlankRowToDataTable("propertiesTable", true, action, propertiesTableInlineOptions);
    });

    //add parameter row to the parameter table when clicked.
    $("#addParameterButton").on("click", function() {
        var action = [{
            "type": "delete",
            "onclick": null
        }];
        addBlankRowToDataTable("parametersTable", true, action, propertiesTableInlineOptions);
    });

    //Save the computation.  Pops up a yes/no modal requesting confirmation.
    $("#saveButton").on("click", function(e) {
        set_yesno_modal(
                `Save Computation`, 
                `Are you sure you want to save this computation?`, 
                ``, 
                "bg-info", 
                function(e) {
                    var startType = toFromGroup.getDropdownSelectionText("from");
                    var endType = toFromGroup.getDropdownSelectionText("to");
                    var fromVal = toFromGroup.getSelectedVal("from");
                    var toVal = toFromGroup.getSelectedVal("to");

                    var algorithmName = $("#algorithmNameButton").text();
                    var algorithmId = findObjectInListByPropValue(algorithmList, "algorithmName", algorithmName).object.algorithmId;

                    var paramData = getNonDeletedRowData("parametersTable");
                    var paramList =[];
                    for (var x = 0; x < paramData.length; x++)
                    {

                        var curRow = paramData[x];
                        var fullDeltaT = curRow[7].replaceAll(" ", "");
                        var deltaTSearch = fullDeltaT.match(/^\d+/g);
                        var deltaT = 0;
                        var deltaTUnits = "";
                        if (deltaTSearch != null && deltaTSearch.length > 0)
                        {
                            deltaT = deltaTSearch[0];
                            deltaTUnits = fullDeltaT.replace(deltaT, "");
                        }
                        var curParam = {
                                "algoParmType": curRow[0],
                                "algoRoleName": curRow[0],
                                "dataType": curRow[2],
                                "interval": curRow[4],
                                "deltaT": deltaT,
                                "deltaTUnits": deltaTUnits,
                                "unitsAbbr": curRow[8],
                                "siteName": curRow[1],
                                "tableSelector": null,
                                "modelId": null,
                                "paramType": curRow[3],
                                "duration": curRow[5],
                                "version": curRow[6]
                        };
                        paramList.push(curParam);
                    }

                    var saveData = {
                            "name": $("#compNameTextbox").val(),
                            "comment": $("#commentsTextarea").val(),
                            "enabled": isSwitchChecked("enabledCheckbox"),
                            "effectiveStartType": startType,
                            "effectiveEndType": endType,
                            "algorithmId": algorithmId,
                            "algorithmName": algorithmName,
                            "props": getPropertiesTableData("propertiesTable"),
                            "parmList": paramList
                    };

                    var groupName = $("#groupSelectbox").find("option:selected").val();
                    if (groupName != null && groupName != "")
                    {
                        var groupId = $("#groupSelectbox").find("option:selected").attr("id");
                        saveData["groupId"] = groupId;
                        saveData["groupName"] = groupName;
                    }

                    if (startType.toLowerCase() == "calendar")
                    {
                        //This is added so the API can take the timestamp and convert it in java
                        saveData["effectiveStartDate"] = fromVal + ":00.000Z[UTC]"; 
                    }
                    else if (startType.toLowerCase() != "now")
                    {
                        saveData["effectiveStartInterval"] = fromVal;
                    }

                    if (endType.toLowerCase() == "calendar")
                    {
                        //This is added so the API can take the timestamp and convert it in java
                        saveData["effectiveEndDate"] = toVal + ":00.000Z[UTC]"; 
                    }
                    else if (endType.toLowerCase() != "now")
                    {
                        saveData["effectiveEndInterval"] = toVal;
                    }

                    var computationId = $("#compIdTextbox").val();
                    if (computationId != null && computationId != "")
                    {
                        saveData["computationId"] = computationId;
                    }
                    var appId = $("#processSelectbox").find("option:selected").attr("id");
                    if (appId != null && appId != "")
                    {
                        var appName = $("#processSelectbox").find("option:selected").text();
                        saveData["appId"] = appId;
                        saveData["applicationName"] = appName;
                    }
                    show_waiting_modal();

                    var token = sessionStorage.getItem("token");
                    $.ajax({
                        url: `../api/gateway?token=${token}&opendcs_api_call=computation`,
                        type: "POST",
                        headers: {     
                            "Content-Type": "application/json"
                        },
                        dataType: "text",
                        data: JSON.stringify(saveData),
                        success: function(response) {

                            setTimeout(function() {
                                hide_waiting_modal();
                                show_notification_modal("Save Computation", 
                                        "Computation saved successfully", 
                                        `The computation has been saved successfully.`, 
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
                                show_notification_modal("There was an error saving the computation", 
                                        null, 
                                        "The computation could not be saved.", 
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
            "opendcs_api_call": "computationrefs"
    };
    //Opens the main table dialog (this is how to add a new element to the 
    //main datatable.
    show_waiting_modal();
    $.ajax({
        url: `../api/gateway`,
        type: "GET",
        data: params,
        success: function(response) {
            
            var computationRefs = response;
            for (var x = 0; x < computationRefs.length; x++)
            {
                var curRow = computationRefs[x];
                var isProdDisplay = curRow.enabled ? '<i class="icon-checkmark4 mr-3 icon-1x"></i>' : "";
                var newRow = [curRow.computationId, curRow.name, curRow.algorithmName, curRow.processName, isProdDisplay, curRow.description, createActionDropdown(dtActions)];
                mainTable.row.add(newRow);
                mainTable.draw(false);
            }
            hide_waiting_modal(500);
        },
        error: function(response) {
            show_notification_modal("Error retrieving algorithm refs", 
                    "There was an issue getting the algorithm refs from the OpenDCS REST API", 
                    `Please contact your system administrator.`, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    null);
            
        }
    });
});


/**
 * Clears the dialog before it's opened.
 * 
 * @returns
 */
function clearDialog()
{
    $("#displayedId").attr("value", "");
    $("#compNameTextbox").val("");
    $("#algorithmNameButton").text("");
    $("#compIdTextbox").val("");
    $("#lastModifiedTextbox").val("");

    $("#processSelectbox").val("");
    $("#commentsTextarea").val("");
    $("#groupSelectbox").val("");

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
            $("#compIdTextbox").val(clickedData[0]);
            $("#compNameTextbox").val(clickedData[1]);
        }
        else
        {
            clickedData = mainTable.row(rowClicked).data();
        }
        $("#algorithmNameButton").text(clickedData[2]);
        $("#commentsTextarea").val(clickedData[5]);
        var enabled = clickedData[4];
        //If it's enabled, it's a string with more than one character.  If it's not enabled, it's an empty string.
        updateSwitchValue("enabledCheckbox", enabled); 

        var params = {
                "computationid": clickedData[0],
                "opendcs_api_call": "computation"
        };
        show_waiting_modal();
        $.ajax({
            url: `../api/gateway`,
            type: "GET",
            data: params,
            success: function(response) {
                $("#lastModifiedTextbox").val(response.lastModified);

                if (response.appId == null || response.appId == "")
                {
                    //Selects "(none)"
                    $("#processSelectbox").find("[value='']").attr("selected", true);
                }
                else
                {
                    $("#processSelectbox").find(`[id|=${response.appId}]`).attr("selected", true);
                }

                var effectiveStartType = response.effectiveStartType.toLowerCase();
                if (effectiveStartType == "no limit")
                {
                    toFromGroup.changeDropdownSelection("from", "nolimit");
                }
                else if (effectiveStartType == "now -")
                {
                    var strippedTimeframe = response.effectiveStartInterval.replaceAll(/\s/g,'');
                    toFromGroup.changeDropdownSelection("from", "nowminus");
                    toFromGroup.setSelectedVal("from", strippedTimeframe, "startswith");
                }
                else if (effectiveStartType == "calendar")
                {
                    var cleanedDate = response.effectiveStartDate.split("[")[0];
                    //UTC true because it's stored that way on the opendcs server
                    var convertedStartDate = getDashDatestringFromDate(new Date(cleanedDate), true); 
                    toFromGroup.changeDropdownSelection("from", "calendar");
                    toFromGroup.setSelectedVal("from", convertedStartDate);
                }

                var effectiveEndType = response.effectiveEndType.toLowerCase();
                if (effectiveEndType == "no limit")
                {
                    toFromGroup.changeDropdownSelection("to", "nolimit");
                }
                else if (effectiveEndType == "now")
                {
                    toFromGroup.changeDropdownSelection("to", "now");
                }
                else if (effectiveEndType == "now +")
                {
                    var strippedTimeframe = response.effectiveEndInterval.replaceAll(/\s/g,'');
                    toFromGroup.changeDropdownSelection("to", "nowplus");
                    toFromGroup.setSelectedVal("to", strippedTimeframe, "startswith");
                }
                else if (effectiveEndType == "now -")
                {
                    var strippedTimeframe = response.effectiveEndInterval.replaceAll(/\s/g,'');
                    toFromGroup.changeDropdownSelection("to", "nowminus");
                    toFromGroup.setSelectedVal("to", strippedTimeframe, "startswith");
                }
                else if (effectiveEndType == "calendar")
                {
                    var cleanedDate = response.effectiveEndDate.split("[")[0];
                    //UTC true because it's stored that way on the opendcs server
                    var convertedStartDate = getDashDatestringFromDate(new Date(cleanedDate), true); 
                    toFromGroup.changeDropdownSelection("to", "calendar");
                    toFromGroup.setSelectedVal("to", convertedStartDate);
                }

                setOpendcsPropertiesTable("propertiesTable", 
                        [], 
                        response.props, 
                        true, 
                        propertiesTableInlineOptions, 
                        modalTableActions);

                for (var x = 0; x < response.parmList.length; x++)
                {
                    var curParam = response.parmList[x];
                    var deltaT = `${curParam.deltaT} ${curParam.deltaTUnits}`;
                    var newRow = [curParam.algoRoleName, curParam.siteName, 
                        curParam.dataType, curParam.paramType, curParam.interval, 
                        curParam.duration, curParam.version, deltaT, 
                        curParam.unitsAbbr, 
                        createActionDropdown(modalTableActions)];
                    parametersTable.row.add(newRow);
                    makeTableInline("parametersTable", parametersTableInlineOptions);
                    parametersTable.draw(false);
                }
                makeTableInline("parametersTable", parametersTableInlineOptions);
                parametersTable.draw(false);

                if (response.groupId != null && response.groupId != "")
                {
                    $(`#groupSelectbox option[id=${response.groupId}]`).prop("selected", true);
                }

                hide_waiting_modal(500);
                return;
            },
            error: function(response) {
                show_notification_modal("Error retrieving computation", 
                        "There was an issue getting the computation from the OpenDCS REST API", 
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
        return;
    }
}

/**
 * opens the dialog for the selected record, but it's considered a new one
 * the computationid is not set.
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
    beginOpenMainTableDialog(clickedLink.closest("tr"), true);
}
document.addEventListener('DOMContentLoaded', function() {
    console.log("opendcs_utilities.js");
});

class ToFromGroup {
    constructor(jquerySelector, fromOptionList, toOptionList, fromDefaultValue, toDefaultValue, runAtEndOfFromChange, runAtEndOfToChange) {
        var optionAttributes = {
                "value": "",
                "id": "",
                "comment": "",
                "last_modified": ""
        };
        var newOption = $("<option>").attr(optionAttributes).html("");

        var timeframeDivTemplate = `<div timeframe="<!--TYPE-->" class="row">
            <label class="col-form-label col-lg-3"><!--TEXT--></label>
            <div div_type="timeframe_div" class="col-lg-4">
            <select dropdown_type="timeframe" type="text" class="form-control"></select>
            </div>
            <div div_type="value_div" class="col-lg-5"></div>
            </div>`;
        var nowPlusMinusSelectHtmlTemplate = `<select timeframe_type="<!--TIMEFRAME_TYPE-->" class="form-control">
            <option value="1hour">1 hour</option>
            <option value="2hours">2 hours</option>
            <option value="3hours">3 hours</option>
            <option value="4hours">4 hours</option>
            <option value="5hours">5 hours</option>
            <option value="6hours">6 hours</option>
            <option value="7hours">7 hours</option>
            <option value="8hours">8 hours</option>
            <option value="9hours">9 hours</option>
            <option value="10hours">10 hours</option>
            <option value="11hours">11 hours</option>
            <option value="12hours">12 hours</option>
            <option value="18hours">18 hours</option>
            <option value="1day">1 day</option>
            <option value="2days">2 days</option>
            <option value="3days">3 days</option>
            <option value="4days">4 days</option>
            <option value="5days">5 days</option>
            <option value="6days">6 days</option>
            <option value="7days">7 days</option>
            <option value="14days">14 days</option>
            <option value="21days">21 days</option>
            <option value="28days">28 days</option>
            <option value="35days">35 days</option>
            <option value="40days">40 days</option>
            <option value="50days">50 days</option>
            <option value="60days">60 days</option>
            <option value="70days">70 days</option>
            <option value="80days">80 days</option>
            <option value="90days">90 days</option>
            <option value="100days">100 days</option>
            <option value="150days">150 days</option>
            <option value="200days">200 days</option>
            <option value="250days">250 days</option>
            <option value="300days">300 days</option>
            <option value="350days">350 days</option>
            <option value="365days">365 days</option>
            </select>`;
        this.allOptions = [
            {
                "value": "nolimit",
                "timeframe_type": "nolimit",
                "text": "No Limit",
                "events": 
                {
                    "onchange": function(e) 
                    {
                        console.log("Function nolimit has been changed.");
                    }
                },
                "option": `<option timeframe_type="nolimit" value="nolimit">No Limit</option>`,
                "dom": `<div timeframe_type="nolimit">No Limit</div>`
            },
            {
                "value": "now",
                "timeframe_type": "now",
                "text": "Now",
                "events": 
                {
                    "onchange": function(e) 
                    {
                        console.log("Function now has been changed.");
                    }
                },
                "option": `<option timeframe_type="now" value="now">Now</option>`,
                "dom": `<div timeframe_type="now">No Future Data</div>`
            },
            {
                "value": "nowplus",
                "timeframe_type": "nowplus",
                "text": "Now +",
                "function": function(e) {
                    console.log("Function now plus has been changed.");
                },
                "option": `<option timeframe_type="nowplus" value="nowplus">Now +</option>`,
                "dom": nowPlusMinusSelectHtmlTemplate.replace("<!--TIMEFRAME_TYPE-->", "nowplus")
            },
            {
                "value": "nowminus",
                "timeframe_type": "nowminus",
                "text": "Now -",
                "function": function(e) {
                    console.log("Function now minus has been changed.");
                },
                "option": `<option timeframe_type="nowminus" value="nowminus">Now -</option>`,
                "dom": nowPlusMinusSelectHtmlTemplate.replace("<!--TIMEFRAME_TYPE-->", "nowminus")
            },
            {
                "value": "calendar",
                "timeframe_type": "calendar",
                "text": "Calendar",
                "events": 
                {
                    "onchange": function(e) 
                    {
                        console.log("Function calendar has been changed.");
                    }
                },
                "option": `<option timeframe_type="calendar" value="calendar">Calendar</option>`,
                "dom": `<input timeframe_type="calendar" class="form-control" type="datetime-local" name="datetime-local"></input>`
            },
            {
                "value": "filetime",
                "timeframe_type": "filetime",
                "text": "File Time",
                "events": 
                {
                    "onchange": function(e) 
                    {
                        console.log("Function filetime has been changed.");
                    }
                },
                "option": `<option timeframe_type="filetime" value="filetime">File Time</option>`,
                "dom": `<input timeframe_type="filetime"></input>`
            }
            ]


        this.mainJq = $(jquerySelector);
        this.mainJq.append(timeframeDivTemplate.replace("<!--TYPE-->", "from").replace("<!--TEXT-->", "Effective Start"));
        this.mainJq.append(timeframeDivTemplate.replace("<!--TYPE-->", "to").replace("<!--TEXT-->", "Effective End"));
        this.fromDiv = this.mainJq.find("[timeframe=from]");
        this.fromDropdown = this.fromDiv.find("[dropdown_type=timeframe]");
        this.toDiv = this.mainJq.find("[timeframe=to]");
        this.toDropdown = this.toDiv.find("[dropdown_type=timeframe]");
        this.fromOptionList = fromOptionList;
        this.toOptionList = toOptionList;
        this.runAtEndOfFromChange = runAtEndOfFromChange;
        this.runAtEndOfToChange = runAtEndOfToChange;

        var valueDiv = this.fromDiv.find("[div_type=value_div]");
        for (var x = 0; x < fromOptionList.length; x++)
        {
            var curOpt = fromOptionList[x];
            for (var y = 0; y < this.allOptions.length; y++)
            {
                var curAo = this.allOptions[y];
                if (curOpt == curAo.value)
                {
                    valueDiv.append(curAo.dom);
                    var newDiv = this.fromDiv.find(`[timeframe_type=${curAo.timeframe_type}]`);
                    newDiv.addClass("displayNone");
                    var newOption = $(curAo.option);
                    this.fromDropdown.append(newOption);
                    break;
                }
            }
        }

        valueDiv = this.toDiv.find("[div_type=value_div]");
        for (var x = 0; x < toOptionList.length; x++)
        {
            var curOpt = toOptionList[x];
            for (var y = 0; y < this.allOptions.length; y++)
            {
                var curAo = this.allOptions[y];
                if (curOpt == curAo.value)
                {
                    valueDiv.append(curAo.dom);
                    var newDiv = this.toDiv.find(`[timeframe_type=${curAo.timeframe_type}]`);
                    newDiv.addClass("displayNone");
                    var newOption = $(curAo.option);
                    this.toDropdown.append(newOption);
                    break;
                }
            }
        }

        var thisObject = this;
        this.fromDropdown.on("change", function(e) {
            thisObject.makeProperDivVisible(e.target);
            if (thisObject.runAtEndOfFromChange != null)
            {
                thisObject.runAtEndOfFromChange();
            }
        });
        this.toDropdown.on("change", function(e) {
            thisObject.makeProperDivVisible(e.target);
            if (thisObject.runAtEndOfToChange != null)
            {
                thisObject.runAtEndOfToChange();
            }
        });
        this.fromDropdown.trigger("change");
        this.toDropdown.trigger("change");
    }

    makeProperDivVisible(dropdown) {
        var selectedTimeframeType = $(dropdown).find(":selected").attr("timeframe_type");
        var valueDiv = $(dropdown).closest("[timeframe]").find("[div_type=value_div]");
        valueDiv.find("[timeframe_type]").addClass("displayNone");
        valueDiv.find(`[timeframe_type=${selectedTimeframeType}]`).removeClass("displayNone");
    }

    changeDropdownSelection(fromToString, value) {
        var targetDiv = this.mainJq.find(`[timeframe=${fromToString}]`);
        var targetDropdown = targetDiv.find("[dropdown_type=timeframe]");
        targetDropdown.find(`option[value=${value}]`).prop('selected', true);
        targetDropdown.trigger("change");
    }

    getDropdownSelectionText(fromToString) {
        var targetDiv = this.mainJq.find(`[timeframe=${fromToString}]`).find("[div_type=timeframe_div]");
        var targetDomObject = targetDiv.find("[dropdown_type=timeframe]"); //Active value dom object.
        var txt = targetDomObject.find("option:selected").text();
        return txt;
    }

    getDropdownSelectionVal(fromToString) {
        var targetDiv = this.mainJq.find(`[timeframe=${fromToString}]`).find("[div_type=timeframe_div]");
        var targetDomObject = targetDiv.find("[dropdown_type=timeframe]"); //Active value dom object.
        var txt = targetDomObject.find("option:selected").val();
        return txt;
    }

    getSelectedVal(fromToString) {
        var targetDiv = this.mainJq.find(`[timeframe=${fromToString}]`).find("[div_type=value_div]");
        var targetDomObject = targetDiv.children().not(".displayNone"); //Active value dom object.
        var timeframeType = targetDomObject.attr("timeframe_type");
        var value = null;
        if (timeframeType == "nolimit")
        {
            value = targetDomObject.text();
        }
        else if (timeframeType == "now")
        {
            value = null;
        } 
        else if (timeframeType == "nowminus")
        {
            value = targetDomObject.val();
        }
        else
        {
            value = targetDomObject.val();
        }
        return value;
    }

    getSelectedText(fromToString) {
        var targetDiv = this.mainJq.find(`[timeframe=${fromToString}]`).find("[div_type=value_div]");
        var targetDomObject = targetDiv.children().not(".displayNone"); //Active value dom object.
        var timeframeType = targetDomObject.attr("timeframe_type");
        var txt = targetDomObject.find("option:selected").text();
        return txt;
    }

    setSelectedVal(fromToString, value, searchType) {
        if (searchType == null)
        {
            searchType = "equals";
        }
        else
        {
            searchType = searchType.toLowerCase();
        }
        var targetDiv = this.mainJq.find(`[timeframe=${fromToString}]`).find("[div_type=value_div]");
        var targetDomObject = targetDiv.children().not(".displayNone"); //Active value dom object.
        if (targetDomObject.is("select"))
        {

            var selector = `value=${value}`;
            if (searchType == "contains")
            {
                selector = selector.replace("=", "*=");
            }
            else if (searchType == "startswith")
            {
                selector = selector.replace("=", "^=");
            }
            else if (searchType == "endswith")
            {
                selector = selector.replace("=", "$=");
            }

            //toFromGroup.toDiv.find("[div_type=value_div] [timeframe_type=nowplus] option[value^=8hour]")[0]
            targetDomObject.find(`option[${selector}]`).prop('selected', true);
        }
        else if (targetDomObject.is("input"))
        {
            targetDomObject.val(value);
        }
        else
        {
            //No Value Needed.
        }
    }
}

class SensorUnitConversion {
    constructor(table, algorithmColumnNumber, aColumnIndex, lastColumnActive, runAfterUnitConversionAlgorithmRetrieval) {
        if (typeof(table) == "string" && !table.startsWith("#"))
        {
            table = "#" + table;
        }
        this.jqTable = $(table);
        this.algorithmColumnNumber = algorithmColumnNumber;
        this.aColumnIndex = aColumnIndex;
        this.getUnitConversionAlgorithms(runAfterUnitConversionAlgorithmRetrieval);
        this.lastColumnActive = lastColumnActive;

        var targetTable = this.jqTable.DataTable();
        var visibleColumns = targetTable.columns().visible();


        this.dtAlgorithmColumnNumber = this.algorithmColumnNumber;
        for (var x = 0; x < this.algorithmColumnNumber; x++)
        {
            if (!visibleColumns[x])
            {
                this.dtAlgorithmColumnNumber--;
            }
        }

        this.dtA_ColumnIndex = this.aColumnIndex;
        for (var x = 0; x < this.aColumnIndex; x++)
        {
            if (!visibleColumns[x])
            {
                this.dtA_ColumnIndex--;
            }
        }

    }

    runSensorUnitAlgoOnTable() {
        var dtTable = this.jqTable.DataTable();
        var rowNodes = dtTable.rows().nodes();
        for (var x = 0; x < rowNodes.length; x++)
        {
            var curNode = rowNodes[x];
            var algoNode = $(curNode).find("td")[this.dtAlgorithmColumnNumber];
            this.setSensorUnitAlgo(algoNode);
        }
    }

    setSensorUnitAlgo(e) {
        var jqCell = $(e);
        var sensDtTable = $(e).closest("table").DataTable();

        var clickedRow = $(sensDtTable.row(e.closest("tr")).node()).find("td");

        var algoCell = clickedRow[this.dtAlgorithmColumnNumber];
        var activeCellIndexes = this.getInlineColumnsFromEquation(this.openDcsUnitConversionAlgorithms[$(algoCell).text()].description);

        for (var x = this.dtA_ColumnIndex; x < clickedRow.length; x++)
        {
            var curCell = $(clickedRow[x])
            if (activeCellIndexes.indexOf(x) != -1)
            {
                curCell.removeAttr("clickable");
                curCell.removeClass("invisible");
            }
            else
            {
                curCell.attr("clickable", "false");
                curCell.addClass("invisible");
            }
        }
        if (this.lastColumnActive)
        {
            clickedRow.last().removeAttr("clickable");
            clickedRow.last().removeClass("invisible");
        }

        //var targetRow = sensorUnitConversionsTable.row(e.closest("tr")).data();
    }

    getInlineColumnsFromEquation(equation)
    {
        var returnColumns = [];
        //If it is not an equation, don't allow any cells to be edited..Return an empty array.
        if (!equation.toLowerCase().startsWith("y ="))
        {
            return returnColumns;
        }
        if (equation.indexOf("A") != -1)
        {
            returnColumns.push(this.dtA_ColumnIndex + 0);
        }
        if (equation.indexOf("B") != -1)
        {
            returnColumns.push(this.dtA_ColumnIndex + 1);
        }
        if (equation.indexOf("C") != -1)
        {
            returnColumns.push(this.dtA_ColumnIndex + 2);
        }
        if (equation.indexOf("D") != -1)
        {
            returnColumns.push(this.dtA_ColumnIndex + 3);
        }
        if (equation.indexOf("E") != -1)
        {
            returnColumns.push(this.dtA_ColumnIndex + 4);
        }
        if (equation.indexOf("F") != -1)
        {
            returnColumns.push(this.dtA_ColumnIndex + 5);
        }
        return returnColumns;
    }

    getUnitConversionAlgorithms(runAfterUnitConversionRetrieval)
    {
        var thisClass = this;
        $.ajax({
            //url: "/OHydroJson/reflists?name=UnitConversionAlgorithm&token=" + token,
            url: `../api/gateway`,
            type: "GET",
            data: {
                "opendcs_api_call": "reflists",
                "name": "UnitConversionAlgorithm"
            },
            success: function(response) {
                thisClass.openDcsUnitConversionAlgorithms = response.UnitConversionAlgorithm.items;
                if (runAfterUnitConversionRetrieval != null)
                {
                    runAfterUnitConversionRetrieval(thisClass);
                }
            },
            error: function(response) {
                console.log("Error getting UnitConversionAlgorithms.");s
            }
        });
    }
}

class OpenDcsData {
    constructor() {
        this.data = {};
        this.propspecs = {}
    }

    getPropspecs(propspecs)
    {
        var thisClass = this;
        for (var x = 0; x < propspecs.length; x++)
        {
            var opendcsApiCall = "propspecs";
            var propspec = propspecs[x];

            var onSuccess = function(response) {
                const [ , paramString ] = this.url.split( '?' );
                const urlParams = new URLSearchParams(paramString);
                var cls = urlParams.get("class");
                thisClass.propspecs[cls].data = response;
                thisClass.propspecs[cls].status = "success";
                thisClass.propspecs[cls].ajaxObject = this;
            };
            var onError = function(response) {
                thisClass.propspecs[propspec].status = "error";
                thisClass.propspecs[propspec].ajaxObject = this;
            };

            this.propspecs[propspec] = {
                    "status": "retrieving",
                    "data": null,
                    "ajaxObject": null
            }
            $.ajax({
                url: `../api/gateway`,
                type: "GET",
                data: {
                    "opendcs_api_call": opendcsApiCall,
                    "class": propspec
                },
                success: onSuccess,
                error: onError
            });
        }
    }

    //onSuccess will always be run after setting the class data to the response.
    //It has parameters for thisClass and response.
    //onError has parameters for thisClass and response.
    getData(opendcsApiCalls, onSuccess, onError)
    {
        var thisClass = this;

        for (var x = 0; x < opendcsApiCalls.length; x++)
        {
            var opendcsApiCall = opendcsApiCalls[x];
            var k = opendcsApiCall;


            var fullOnSuccess = function(response) {
                // Get everything after the `?`
                const [ , paramString ] = this.url.split( '?' );
                const urlParams = new URLSearchParams(paramString);
                var apiCall = urlParams.get("opendcs_api_call");
                thisClass.data[apiCall].data = response;
                thisClass.data[apiCall].status = "success";
                thisClass.data[apiCall].ajaxObject = this;
                for (var y = 0; y < thisClass.data[apiCall].grouped_api_calls.length; y++)
                {
                    var curApiCall = thisClass.data[apiCall].grouped_api_calls[y];
                    if (thisClass.data[curApiCall].status != "success")
                    {
                        console.log(`Still Waiting for API calls to complete.`);
                        return;
                    }
                }
                if (onSuccess == null)
                {
                    //onSuccess was not passed.
                }
                else
                {
                    onSuccess(thisClass, response);
                }
            };
            var fullOnError = function(response) {
                thisClass.data[k].status = "error";
                thisClass.data[k].ajaxObject = this;
                if (onError == null)
                {
                    //onError was not passed.
                }
                else
                {
                    onError(thisClass, response);
                }
            };

            this.data[k] = {
                    "status": "retrieving",
                    "data": null,
                    "grouped_api_calls": opendcsApiCalls,
                    "onsuccess": fullOnSuccess,
                    "onError": fullOnError,
                    "ajaxObject": null
            }
            $.ajax({
                url: `../api/gateway`,
                type: "GET",
                data: {
                    "opendcs_api_call": opendcsApiCall
                },
                success: fullOnSuccess,
                error: fullOnError
            });
        }
    }
}

function handleErrorResponse(response, showAlert, showAlertDetails)
{
    try {
        var responseText = response.responseText;
        var responseJson = response.responseJSON;
        if (showAlert)
        {
            if (showAlertDetails == null)
            {
                showAlertDetails = {};
            }
            if (showAlertDetails.title == null)
            {
                showAlertDetails.title = "Error";
            }
            if (showAlertDetails.header == null)
            {
                showAlertDetails.header = "An error occurred.";
            }
            showAlertDetails.message = response.message;
            if (showAlertDetails.buttonText == null)
            {
                showAlertDetails.buttonText = "OK";
            }
            if (showAlertDetails.buttonCss == null)
            {
                showAlertDetails.buttonCss = "bg-danger";
            }
            if (showAlertDetails.headerCss == null)
            {
                showAlertDetails.headerCss = "bg-danger";
            }
            show_notification_modal(showAlertDetails.title, 
                    showAlertDetails.header, 
                    responseJson.message, 
                    showAlertDetails.buttonText, 
                    showAlertDetails.buttonCss, 
                    showAlertDetails.headerCss,
                    showAlertDetails.buttonOnClick);

        }
    }
    catch (error) {
        show_notification_modal("Error", "An error occurred.", "There was an error with the operation.", "OK", "bg-danger", "bg-danger");
    }
}
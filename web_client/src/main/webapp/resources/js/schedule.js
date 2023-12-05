//schedule.js

//---------------------
/**
 * Reference to the schedule entry datatable
 */
var scheduleEntryTable;

/**
 * OpenDCS reflists and propspecs are stored here.  This is an instance of
 * the OpenDcsData class, which is an object that helps get recurring 
 * API data from the OpenDCS API.
 */
var openDcsData;

document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded schedule.js.");
    openDcsData = new OpenDcsData();
    show_waiting_modal();
    openDcsData.getData(["schedulerefs", "apprefs", "routingrefs"], 
            function(classInstance, response) {
        for (var x = 0; x < classInstance.data["apprefs"].data.length; x++)
        {
            var curLoadingApp = classInstance.data["apprefs"].data[x];
            var newOption = $('<option>', {
                value: curLoadingApp.appName,
                text: curLoadingApp.appName,
                appid: curLoadingApp.appId
            })
            $("#scheduleEntryLoadingApp").append(newOption);
        }
        for (var x = 0; x < classInstance.data["routingrefs"].data.length; x++)
        {
            var curRoutingRef = classInstance.data["routingrefs"].data[x];
            var newOption = $('<option>', {
                value: curRoutingRef.name,
                text: curRoutingRef.name,
                routingid: curRoutingRef.routingId
            })
            $("#scheduleEntryRoutingSpec").append(newOption);
        }
        updateScheduleEntryTable(classInstance.data.schedulerefs.data);
        hide_waiting_modal(500);
    },
    function(response) {
        console.log("app ref error.");
    }
    );


    allTimezones.forEach(tz => {
        var newOption = $('<option>', {
            value: tz,
            text: tz
        });

        $("#onceTimezoneSelect").append(newOption);
        newOption = $('<option>', {
            value: tz,
            text: tz
        });

        $("#timezoneSelect").append(newOption);
    });

    initializeEvents();
    initializeDataTables();
    initializeExecutionSchedule();
});

/**
 * Performs the process for opening the main dialog.  This happens when either
 * the user clicks new, copy, or selects a row to be edited
 * 
 * @param rowClicked If a row is clicked, this will be populated with that row.
 *                   null if the new button is clicked. 
 * @param copy       true if the user selects copy on a new row.
 * @returns
 */
function openScheduleDialog(rowClicked, rowCopy)
{
    var scheduleData = null;
    clearDialog();
    if (rowClicked != null)
    {
        var scheduleData;

        if (!rowCopy)
        {
            scheduleData = scheduleEntryTable.row(this).data();
            $("#modal_schedule #modalTitle #modalSubTitle").html(` (Edit ${scheduleData[1]})`);
            $("#scheduleEntryName").val(scheduleData[1]);
        }
        else
        {
            $("#selectedSheduleRowIndex").data("index", -1);
            scheduleData = scheduleEntryTable.row(rowClicked).data();
            $("#modal_schedule #modalTitle #modalSubTitle").html(` (Copy ${scheduleData[1]})`);
        }
        var clickedRowIndex = scheduleEntryTable.row(this).index();
        $("#selectedSheduleRowIndex").data("index", clickedRowIndex);
        $("#lastModified").text(scheduleData[5]);
        var isEnabled = (scheduleData[4].indexOf("checkmark") != -1);
        updateSwitchValue("scheduleEntryEnabled", isEnabled);
        $("#scheduleEntryLoadingApp").val(scheduleData[2]);
        $("#scheduleEntryRoutingSpec").val(scheduleData[3]);

        var params = {
                "scheduleid": scheduleData[0],
                "opendcs_api_call": "schedule"
        };
        show_waiting_modal();
        $.ajax({
            url: "../api/gateway",
            type: "GET",
            data: params,
            success: function(response) {

                //Once
                if (response.startTime != null && response.runInterval == null)
                {
                    var startTime = response.startTime.substr(0,16);
                    selectRadioButton("once");
                    $("#onceStartDateTime").val(startTime);
                    $("#onceTimezoneSelect").val(response.timeZone);
                }
                //Continuously
                else if (response.startTime == null && response.startTime == null)
                {
                    selectRadioButton("continuous");
                }
                //Interval
                else if (response.runInterval != null)
                {
                    selectRadioButton("runevery");
                    var split = response.runInterval.split(" ");
                    $("#runEveryDigit").val(split[0]);
                    $("#runEveryUnit").val(split[1]);
                    var startTime = response.startTime.substr(0,16);
                    $("#runEveryStartDateTime").val(startTime);
                    $("#timezoneSelect").val(response.timeZone);
                }
                setTimeout(function(schedData) {
                    $("#modal_schedule").modal("show");
                }, 500, response);
                hide_waiting_modal(500);
            },
            error: function(response) {
                hide_waiting_modal(500);
            }
        });
    }
    else
    {
        populateScheduleDialog(null);
        $("#modal_schedule").modal("show");
    }
}


/**
 * Opens the save modal.  It is a confirmation modal that the user really
 * wants to save the data.
 * 
 * @returns
 */
function openSaveModal()
{
    set_yesno_modal("Save Schedule", 
            `Save Schedule?`, 
            `Are you sure you want to save this schedule?`, 
            "bg-info", 
            function() {
        var isValid = true;
        var errMessage = "";
        if ($("#scheduleEntryName").val() == null || $("#scheduleEntryName").val() == "")
        {
            isValid = false;
            errMessage = "You must enter a schedule entry name.";
        }
        var scheduleType = getSelectedScheduleRadioButton().val();
        if (scheduleType == "once")
        {
            var start = $("#onceStartDateTime").val();
            isValid =  start != '' ? true : false;
            errMessage = isValid ? "" : "Invalid start time for 'Run Once'.";                            
        }
        else if (scheduleType == "runevery")
        {
            var start = $("#runEveryStartDateTime").val();
            isValid =  start != '' ? true : false;
            errMessage = isValid ? "" : "Invalid start time for 'Run Every'.";                            
        }

        if (!isValid)
        {
            show_notification_modal("Save Schedule", 
                    "Cannot Save Schedule", 
                    errMessage, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    function() {
                hide_notification_modal();
            }
            );
            return;
        }

        var clickedIndex = parseInt($("#selectedSheduleRowIndex").data("index"), 10);
        var clickedData = null;
        if (clickedIndex > -1)
        {
            clickedData = scheduleEntryTable.row(clickedIndex).data();
        }

        var params = {
                "appId": $("#scheduleEntryLoadingApp").find(":selected").attr("appid"),
                "appName": $("#scheduleEntryLoadingApp").val(),
                "enabled": isSwitchChecked("scheduleEntryEnabled"),
                "name": $("#scheduleEntryName").val(),
                "routingSpecId": $("#scheduleEntryRoutingSpec").find(":selected").attr("routingid"),
                "routingSpecName": $("#scheduleEntryRoutingSpec").val()
        };

        if (clickedData != null)
        {
            params["schedEntryId"] = clickedData[0];
        }

        var scheduleType = getSelectedScheduleRadioButton().val();
        if (scheduleType == "continuous")
        {
            console.log("Continuous, don't save any other params.");
        }
        else if (scheduleType == "once")
        {
            params["startTime"] = $("#onceStartDateTime").val() + ":00.000Z[UTC]";
            params["timeZone"] =  $("#onceTimezoneSelect").val();
        }
        else if (scheduleType == "runevery")
        {
            params["startTime"] = $("#runEveryStartDateTime").val() + ":00.000Z[UTC]";
            params["timeZone"] =  $("#timezoneSelect").val();    
            params["runInterval"] = $("#runEveryDigit").val() + " " + $("#runEveryUnit").val();
        }


        var token = sessionStorage.getItem("token");
        var url = `../api/gateway?opendcs_api_call=schedule&token=${token}`;
        $.ajax({
            url: url,
            type: "POST",
            headers: {     
                "Content-Type": "application/json"   
            },

            data: JSON.stringify(params),
            success: function(response) {
                hide_waiting_modal(500);
                show_notification_modal("Save Schedule", 
                        `Schedule Saved Successfully`, 
                        `The '${response.name}' schedule has been saved successfully.`, 
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
                show_notification_modal("Save Schedule", 
                        "There was an error saving Schedule.", 
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
    openScheduleDialog(clickedLink.closest("tr"), true);
}


/**
 * Clears the main dialog, making it ready to be populated with data.
 * 
 * @returns
 */
function clearDialog()
{
    $("#modal_schedule #modalTitle #modalSubTitle").html(" (New)");
    $("#scheduleEntryName").val("");
    $("#lastModified").text("");
    $("#selectedSheduleRowIndex").data("index", -1);
    selectRadioButton("continuous");
}


/**
 * Updates the main schedule entry table.
 * 
 * @param responseJson {object} This is the reflist of schedules, retrieved
 *                              from the API.
 * @returns
 */
function updateScheduleEntryTable(responseJson)
{
    scheduleEntryTable.init();
    scheduleEntryTable.clear();
    scheduleEntryTable.draw(false);
    for (var x = 0; x < responseJson.length; x++)
    {
        var curEntry = responseJson[x];
        var params = {
                "objectType": "schedule",
                "objectTypeDisplayName": "Schedule",
                "objectIdIndex": 0,
                "objectNameIndex": 1,
                "urlIdName": "scheduleid"
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
        var isEnabled = curEntry.enabled ? '<i class="icon-checkmark4 mr-3 icon-1x"></i>' : "";
        scheduleEntryTable.row.add([curEntry.schedEntryId, curEntry.name, curEntry.appName, curEntry.routingSpecName, isEnabled, curEntry.lastModified, createActionDropdown(actions)]);
    }
    scheduleEntryTable.draw(false);
}

/**
 * Populates the main dialog for editing/updating/creating schedules.
 * 
 * @param responseJson - {object} The schedule data returned from the API.
 *  
 * @returns
 */
function populateScheduleDialog(data)
{
    clearDialog();
    $("#sourceName").val("");
    if (data != null && data != {})
    {
        $("#sourceName").val(data.name);
        $("#sourceType").val(data.type);
        $("#sourceId").text(data.dataSourceId.toString());
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
    }
    makeTableInline("propertiesTable");
}


/**
 * Adds a property to the property table.
 * 
 * @param name {string} The property name
 * @param value {string} The property value
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
 * Enables or disables the elements that allow the schedule to be repeatedly run
 * 
 * @param enabled {boolean} true for enabled, false for disabled.
 * 
 * @returns
 */
function enableRunEveryElements(enabled)
{
    $("#onceStartDateTime").prop("disabled", enabled);
    $("#onceTimezoneSelect").prop("disabled", enabled);
}

/**
 * Enables the elements for running only once.
 * 
 * @param enabled {boolean} true for enabled, false for disabled.
 * @returns
 */
function enableOnceElements(enabled)
{
    $("#runEveryDigit").prop("disabled", enabled);
    $("#runEveryUnit").prop("disabled", enabled);
    $("#runEveryStartDateTime").prop("disabled", enabled);
    $("#timezoneSelect").prop("disabled", enabled);
}

/**
 * Selects the run frequency radio button based on the 'value' parameter.
 * 
 * @param value {string} The radio button to select. (once, continuous, etc.).
 * @returns
 */
function selectRadioButton(value)
{
    var radioButtons = $('input[type=radio][name=runFreq]');
    for (var x = 0; x < radioButtons.length; x++)
    {
        var curRadioButton = $(radioButtons[x]);
        if (curRadioButton.attr("value") == value)
        {
            curRadioButton.click();
            return;
        }
    }
}

/**
 * Gets the radio button that is currently selected.
 * 
 * @returns {jquery object} The jquery element of the radio button that is 
 *                          currently selected.
 */
function getSelectedScheduleRadioButton()
{
    var radioButtons = $('input[type=radio][name=runFreq]');
    for (var x = 0; x < radioButtons.length; x++)
    {
        var curRadioButton = $(radioButtons[x]);
        var checked = curRadioButton.closest("span").hasClass("checked");
        if (checked)
        {
            return curRadioButton;
        }
    }
    return null;
}

/**
 * Initialize the schedule functionality
 * 
 * @returns
 */
function initializeExecutionSchedule()
{
    var elems = Array.prototype.slice.call(document.querySelectorAll('.form-check-input-switchery'));
    //initialize the switches in the execution schedule.
    elems.forEach(function(html) {
        var switchery = new Switchery(html);
    });

    $('.form-check-input-styled-primary').uniform({
        wrapperClass: 'border-primary text-primary'
    });

    // Danger
    $('.form-check-input-styled-danger').uniform({
        wrapperClass: 'border-danger text-danger'
    });

    // Success
    $('.form-check-input-styled-success').uniform({
        wrapperClass: 'border-success text-success'
    });

    enableRunEveryElements(false);

    $('input[type=radio][name=runFreq]').change(function() {
        if (this.value == "runevery")
        {
            enableRunEveryElements(true);
            enableOnceElements(false);
        }
        else if (this.value == "once")
        {
            enableOnceElements(true);
            enableRunEveryElements(false);
        }
        else
        {
            enableRunEveryElements(true);
            enableOnceElements(true);
        }
    });
    //Doing this twice will initialize the greyed out sections for "once" and "runevery" schedule types.
    selectRadioButton("once");
    selectRadioButton("continuous");
}

/**
 * Initializes all of the events on the page.
 * @returns
 */
function initializeEvents()
{
    $("#saveModalButton").on("click", function() {
        openSaveModal();
    });
}


/**
 * Initializes the datatables on the page.
 * 
 * @returns
 */
function initializeDataTables()
{

    scheduleEntryTable = $("#scheduleEntryTable").DataTable(
            {
                "lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
                "pageLength": 10,
                "dom": 'Bflrtip',
                "buttons": [
                    {
                        text: '+',
                        action: function ( e, dt, node, config ) {
                            openScheduleDialog(null);
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
    $('#scheduleEntryTable').on('click', 'tbody tr', openScheduleDialog);
}

//sites.js
/**
 * DataTable reference to the main table on the page.
 */
var sitesTable;

/**
 * DataTable reference to the properties table.
 */
var propertiesTable;

/**
 * DataTable reference to the siteNames table.
 */
var siteNamesTable;

/**
 * This is the default site name type.  It should be set as a property in 
 * tsdb_properties.
 */
var defaultSiteNameType = "cwms";

/**
 * Inline options for the site names table.
 */
var inlineOptionsSiteNames;

/**
 * Inline options for the properties table.
 */
var inlineOptionsProperties = {
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
 * The OpenDcsData object.  Retrieves all the data you need at the beginning
 * of the page load so it can be used throughout the use of the page without
 * the need to retrieve common data multiple times.
 */
var openDcsData;

document.addEventListener('DOMContentLoaded', function() {
    console.log("Loaded sites.js.");

    initializeDataTables();
    initializeEvents();
    initializeElements();

    openDcsData = new OpenDcsData();
    show_waiting_modal();
    openDcsData.getData(["reflists", "siterefs"], 
            function(classInstance, response) {
        var allSiteNameTypes = Object.keys(classInstance.data.reflists.data.SiteNameType.items);
        allSiteNameTypes.forEach(snt => {
            snt = snt.toLowerCase();
        });
        var options = [];
        allSiteNameTypes.forEach(snt => {
            options.push({
                "value": snt,
                "text": snt,
                "selected": snt == defaultSiteNameType ? true : false
            });
        });
        var selectBox = createSelectBox("displayedTypeSelect", 
                options, 
                ["float-right"],
                changeSiteNameType,
                null,
                null
        );
        var fullDiv = createElement("div", null, ["float-left", "row", "w-50"], null);
        var labelDiv = createElement("div", null, ["col-lg-6", "text-right", "justify-content-center", "align-self-center"], "Displayed Type");
        var selectDiv = createElement("div", null, ["col-lg-6"], selectBox);
        fullDiv.append(labelDiv);
        fullDiv.append(selectDiv);
        addElementToDataTableHeader("sitesTable", fullDiv);
        
        siteNamesTable.setInlineOptions({
                "columnDefs": [
                    {
                        "targets": [0],
                        "type": "select",
                        "data": allSiteNameTypes,
                        "bgcolor": {
                            "change": "#c4eeff"
                        }
                    },
                    {
                        "targets": [1],
                        "type": "input",
                        "data": null,
                        "bgcolor": {
                            "change": "#c4eeff"
                        }
                    }
                    ],
        });
        siteNamesTable.initDataTable();
        updateSitesTable(classInstance.data.siterefs.data);                
        hide_waiting_modal(500);
    },
    function(response) {
        hide_waiting_modal(500);
    }
    );
    allTimezones.forEach(tz => {
        $("#tzSelectbox").append($("<option>", {
            "value": tz,
            "text": tz
        }));
    });
});

/**
 * Updates the sites table (the main table on the page) utilizing the sites
 * reflist from the api.
 * 
 * @param responseJson {object} The sites reflist from the API containing a list
 *                              of all the sites in OpenDCS.
 * @returns
 */
function updateSitesTable(responseJson)
{
    sitesTable.init();
    sitesTable.clear();
    sitesTable.draw(false);

    var selectedSiteNameType = $("#displayedTypeSelect").val();
    $("#siteNameColumnHeader").text("Site Name (" + selectedSiteNameType + ")");
    for (var x = 0; x < responseJson.length; x++)
    {
        var curSite = responseJson[x];
        var siteNameTypes = Object.keys(curSite.sitenames);

        var displayedSiteName = "";
        var displayedSiteNames = "";
        for (var key in curSite.sitenames)
        {
            var curSiteName = curSite.sitenames[key];
            displayedSiteNames += key + " - " + curSiteName + "<br>";
            if (key.toLowerCase() == selectedSiteNameType)
            {
                displayedSiteName = curSiteName;
            }
        }

        var params = {
                "objectType": "site",
                "objectTypeDisplayName": "Site",
                "objectIdIndex": 0,
                "objectNameIndex": 1,
                "urlIdName": "siteid"
        };

        var actions = [{
            "type": "delete",
            "onclick": `deleteOpendcsObject_default(event, this, ${JSON.stringify(params)})`

        }];
        var newRow = [curSite.siteId,
            displayedSiteName,
            displayedSiteNames,
            curSite.description != null ? curSite.description : '',
                    createActionDropdown(actions)];
        sitesTable.row.add(newRow);
    }
    sitesTable.draw(false);
}


/**
 * Opens the save modal to confirm if the user wants to save the data or not.
 * 
 * @returns
 */
function openSaveModal()
{
    set_yesno_modal("Save Site", 
            `Save Site?`, 
            `Are you sure you want to save the site?`, 
            "bg-info", 
            function() {

        var isValid = true;
        var errMessage = "";
        if (siteNamesTable.dataTable.data().length <= 0)
        {
            isValid = false;
            errMessage = "You must have at least one site name to save.";
        }
        else if ($("#elevationTextbox").val() == null || $("#elevationTextbox").val() == "")
        {
            isValid = false;
            errMessage = "You must enter a valid elevation to save this site";
        }

        if (!isValid)
        {
            show_notification_modal("Save Site", 
                    "Cannot Save Site", 
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
        var siteId = $("#displayedSiteId").val();
        var params = {
                "description": $("#descriptionTextbox").val(),
                "elevUnits": $("#elevUnitsSelectbox").val() != null ? $("#elevUnitsSelectbox").val() : "",
                "elevation": $("#elevationTextbox").val() != null ? $("#elevationTextbox").val() : "",
                "latitude": $("#latitudeTextbox").val() != null ? $("#latitudeTextbox").val() : "",
                "longitude": $("#longitudeTextbox").val()  != null ? $("#longitudeTextbox").val() : "",
                "nearestCity": $("#nearestCityTextbox").val() != null ? $("#nearestCityTextbox").val() : "",
                "publicName": $("#publicNameTextbox").val() != null ? $("#publicNameTextbox").val() : "",
                "country": $("#countryTextbox").val() != null ? $("#countryTextbox").val() : "",
                "state": $("#stateTextbox").val() != null ? $("#stateTextbox").val() : "",
                "timezone": $("#tzSelectbox").val() != null ? $("#tzSelectbox").val() : "",
                "region": $("#regionTextbox").val() != null ? $("#regionTextbox").val() : "",
                "sitenames": {},
                "properties": {}
        };
        if (siteId != null && siteId != "")
        {
            params["siteId"] = siteId;
        }
        else
        {
        	//new site
        }

        var propRows = propertiesTable.getNonDeletedRowData();
        for (var x = 0; x < propRows.length; x++)
        {
            var prop = propRows[x];
            params["properties"][prop[0]] = prop[1];
        }

        var siteNamesData = siteNamesTable.getNonDeletedRowData();
        for (var x = 0; x < siteNamesData.length; x++)
        {
            var sn = siteNamesData[x];
            params["sitenames"][sn[0]] = sn[1];
        }
        var token = sessionStorage.getItem("token");
        var url = `../api/gateway?token=${token}&opendcs_api_call=site`
            show_waiting_modal();
        $.ajax({
            url: url,
            type: "POST",
            headers: {     
                "Content-Type": "application/json"   
            },

            data: JSON.stringify(params),
            success: function(response) {
                hide_waiting_modal(500);
                show_notification_modal("Save Site", 
                        "Site Saved Successfully", 
                        "The site has been saved successfully.", 
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
                hide_waiting_modal();
                show_notification_modal("Save Site", 
                        "There was an error saving Site.", 
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
 * Clears the site dialog to be ready to be populated with a new site.
 * 
 * @returns
 */
function clearSiteDialog()
{
    siteNamesTable.clearDataTable(false);
    
    propertiesTable.clearDataTable();

    $("#displayedSiteId").attr("value", "");
    $("#sitesTitle").val("NEW SITE");

    $("#latitudeTextbox").val("");
    $("#longitudeTextbox").val("");
    $("#elevationTextbox").val("");
    $("#elevUnitsSelectbox").val("ft");
    $("#nearestCityTextbox").val("");
    $("#tzSelectbox").val("GMT");
    $("#stateTextbox").val("");
    $("#countryTextbox").val("");
    $("#regionTextbox").val("");
    $("#publicNameTextbox").val("");
    $("#descriptionTextbox").val("");
}

/**
 * Populates the site dialog with the site data that was returned from the API.
 * 
 * @param data {object} The site data, which came back from the API.
 * @returns
 */
function populateSiteDialog(data)
{
    clearSiteDialog();
    if (data != null)
    {
        $("#displayedSiteId").val(data.siteId);
        var displayedSiteName = "NO PUBLIC SITE NAME FOR THIS SITE";
        if (data.publicName != null && data.publicName != "")
        {
            displayedSiteName = data.publicName;
        }
        $("#sitesTitle").val(displayedSiteName);
        $("#latitudeTextbox").val(data.latitude);
        $("#longitudeTextbox").val(data.longitude);
        $("#elevationTextbox").val(data.elevation);
        $("#elevUnitsSelectbox").val(data.elevUnits);
        $("#nearestCityTextbox").val(data.nearestCity);
        $("#tzSelectbox").val(data.timezone);
        $("#stateTextbox").val(data.state);
        $("#countryTextbox").val(data.country);
        $("#regionTextbox").val(data.region);
        $("#publicNameTextbox").val(data.publicName);
        $("#descriptionTextbox").val(data.description);
        for (var key in data.sitenames)
        {
            var actions = [{
                "type": "delete",
                "onclick": null
            }];
            var newRow = [key, data.sitenames[key], createActionDropdown(actions)];
            siteNamesTable.addRow(newRow);
        }
        siteNamesTable.dataTable.draw(false);
        siteNamesTable.makeTableInline();

        var actions = [{
            "type": "delete",
            "onclick": null
        }];
        
        propertiesTable.updateProps(data.properties) ;
    }
    else
    {
        console.log("Not adding to the site dialog, as it's a new site being added.");
    }
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
function openSiteDialog(rowClicked)
{
    var params = {}
    if (rowClicked != null)
    {
        var siteData = sitesTable.row(this).data();
        var token = sessionStorage.getItem("token");
        params = {
                "siteid": siteData[0],
                "token": token,
                "opendcs_api_call": "site"
        };
        show_waiting_modal();
        $.ajax({
            url: `../api/gateway`,
            type: "GET",
            data: params,
            success: function(response) {
                setTimeout(function(data) {
                    populateSiteDialog(data);
                    hide_waiting_modal(0);
                    $("#modal_site").modal("show");
                }, 500, response);
            },
            error: function(response) {
                hide_waiting_modal(500);
            }
        });
    }
    else
    {
        populateSiteDialog(null);
        $("#modal_site").modal("show");
    }
}

/**
 * Allows the user to change the site name type that is used in the site name
 * column.  This allows the user to sort on the corresponding site name.
 * 
 * @param event {object} The change event from the displayed site name type
 *                       select box. 
 * @returns
 */
function changeSiteNameType(event)
{
    updateSitesTable(openDcsData.data.siterefs.data);
}

/**
 * Initializes all of the events on the page.
 * 
 * @returns
 */
function initializeEvents()
{
    $("#saveSiteModalButton").click(function() {
        openSaveModal();
    });

    $("#newSiteButton").on("click", function() {
        openSiteDialog(null);
    });
    $("#addSiteNameButton").on("click", function() {
        var targetId = "siteNamesTable";
        var action = [{
            "type": "delete",
            "onclick": null
        }];
        
        siteNamesTable.addBlankRowToDataTable(true);
    });

    $("#modal_site").on('shown.bs.modal', function(){
        siteNamesTable.updateDataTableScroll();
        propertiesTable.updateDataTableScroll();
    });
}

/**
 * Initializes all of the datatables on the page.
 * 
 * @returns
 */
function initializeDataTables()
{
	
	
    sitesTable = $("#sitesTable").DataTable(
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
                            openSiteDialog(null);
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

    propertiesTable = new PropertiesTable(
    		"propertiesTable", 
    		true);
    
    siteNamesTable = new OpenDcsDataTable("siteNamesTable", 
    		{
		        "searching": false,
		        "ordering": false,
		        "paging": false,
		        "info": false,
		        "scrollCollapse": true,
		        "scrollY": 150,
		        "autoWidth": true,
		        "dom": 'Bflrtip',
		        "buttons": []
		    }, 
    		null, 
    		[{
                "type": "delete",
                "onclick": null
            }], 
    		false) ;
    $('#sitesTable').on('click', 'tbody tr', openSiteDialog);
    updateDataTableScroll("#sitesTable", 0);
}

/**
 * Initializes the elements on the web page.  In this case, it is the validation
 * class.
 * 
 * @returns
 */
function initializeElements()
{
    var validator = $('.form-validate-jquery').validate({
        ignore: 'input[type=hidden], .select2-search__field, .no_validate', // ignore hidden fields
        errorClass: 'validation-invalid-label',
        successClass: 'validation-valid-label',
        validClass: 'validation-valid-label',
        highlight: function(element, errorClass) {
            $(element).removeClass(errorClass);
        },
        unhighlight: function(element, errorClass) {
            $(element).removeClass(errorClass);
        },
        success: function(label) {
            label.addClass('validation-valid-label').text('Acceptable.'); // remove to hide Success message
        },

        // Different components require proper error label placement
        errorPlacement: function(error, element) {

            // Unstyled checkboxes, radios
            if (element.parents().hasClass('form-check')) {
                error.appendTo( element.parents('.form-check').parent() );
            }

            // Input with icons and Select2
            else if (element.parents().hasClass('form-group-feedback') || element.hasClass('select2-hidden-accessible')) {
                error.appendTo( element.parent() );
            }

            // Input group, styled file input
            else if (element.parent().is('.uniform-uploader, .uniform-select') || element.parents().hasClass('input-group')) {
                error.appendTo( element.parent().parent() );
            }

            // Other elements
            else {
                error.insertAfter(element);
            }
        },
        rules: {
            password: {
                minlength: 5
            },
            repeat_password: {
                equalTo: '#password'
            },
            email: {
                email: true
            },
            repeat_email: {
                equalTo: '#email'
            },
            minimum_characters: {
                minlength: 10
            },
            maximum_characters: {
                maxlength: 10
            },
            minimum_number: {
                min: 10
            },
            maximum_number: {
                max: 10
            },
            number_range: {
                range: [10, 20]
            },
            url: {
                url: true
            },
            date: {
                date: true
            },
            date_iso: {
                dateISO: true
            },
            numbers: {
                number: true
            },
            digits: {
                digits: true
            },
            creditcard: {
                creditcard: true
            },
            basic_checkbox: {
                minlength: 2
            },
            styled_checkbox: {
                minlength: 2
            },
            switchery_group: {
                minlength: 2
            },
            switch_group: {
                minlength: 2
            }
        },
        messages: {
            custom: {
                required: 'This is a custom error message'
            },
            basic_checkbox: {
                minlength: 'Please select at least {0} checkboxes'
            },
            styled_checkbox: {
                minlength: 'Please select at least {0} checkboxes'
            },
            switchery_group: {
                minlength: 'Please select at least {0} switches'
            },
            switch_group: {
                minlength: 'Please select at least {0} switches'
            },
            agree: 'Please accept our policy'
        }
    });
}

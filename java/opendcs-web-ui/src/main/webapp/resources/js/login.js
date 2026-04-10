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
 *  # Login
 *  # Web Login functionality to the OpenDCS database using the OpenDCS API. 
 *    # Author Olav & Co., Inc.
 *
 * ---------------------------------------------------------------------------- */

/**
 * Runs on page load.  
 * Initializes the button clicks
 * Initializes keypress/keyup events.
 * Focuses on the username textbox.
 * Initializes the datatables
 * Gets the information from the api that is needed to display the netlist list, as well as information that will be reused as the user navigates. 
 */
$( document ).ready(function() {
    console.log("Loaded login.js.");
    $(".dropdown-user").addClass("invisible");

    $("#loginButton").on("click", function(e) {
        login();
    });
    $("#id_password").keyup(inputBoxLogin);
    $("#id_organization").keyup(inputBoxLogin);
    let usernameField = $("#id_username");
    usernameField.keyup(inputBoxLogin);
    usernameField.focus();
    const $orgSelect = $('#id_organization');

    $.ajax({
        url: `${globalThis.API_URL}/organizations`,
        type: "GET",
        dataType: "json",
        success: function (data) {
            data.forEach(function (org) {
                $('<option>')
                    .val(org.name)
                    .text(org.name)
                    .appendTo($orgSelect);
            });
            $orgSelect.select2({
                placeholder: 'Select an organization',
                allowClear: true,
                minimumResultsForSearch: 0,
                width: '100%'
            });
            const orgId = localStorage.getItem("organizationId");
            if (orgId && $orgSelect.find('option[value="' + orgId + '"]').length) {
                $orgSelect.val(orgId).trigger('change');
            }
        },
        failure: function () {
            console.error("Failed to load organizations");
            $orgSelect.next('.select2-container').hide();
        }
    });
});

function inputBoxLogin(event)
{
    // Number 13 is the "Enter" key on the keyboard
    if (event.keyCode === 13) {
        login();
    }
}

/**
 * Attempts to log the user into OpenDCS using the credentials api call in OHydroJson
 * On success, it will set the username and token into the session storage for future use.  It will bring the user to a page in decodes.  
 * On failure, a notification modal will appear, stating that there was an issue with the login attempt.
 */
function login()
{
    const username = $("#id_username").val();
    const password = $("#id_password").val();
    const organization = $("#id_organization").val();
    const params = {
            "username": username,
            "password": password
    };
    $.ajax({
        url: `${window.API_URL}/credentials`,
        type: "POST",
        data: JSON.stringify(params),
        headers: {
            "Content-Type": "application/json",
            "X-ORGANIZATION-ID": organization
        },
        success: function(response) {
            sessionStorage.setItem("username", response.username);
            localStorage.setItem("organizationId", organization);
            window.location = "platforms";
        },
        error: function(response) {
            let errorJson;
            let errorMessage;
            try {
                errorJson = JSON.parse(response.responseText);
                errorMessage = errorJson.errMessage;
            }
            catch (e) {
                if (response.responseText.length > 0)
                {
                    errorMessage = response.responseText;
                }
                else
                {
                    errorMessage = "Could not login due to an unknown error.";
                }
            }
            show_notification_modal("Login", 
                    "There was an error logging in.", 
                    errorMessage, 
                    "OK", 
                    "bg-danger", 
                    "bg-secondary",
                    null);
        }
    });
}
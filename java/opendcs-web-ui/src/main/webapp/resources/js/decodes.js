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

/**
 * @file Controls general functions for the OpenDCS Web Tool.
 * @author Olav & Co., Inc.
 */

//Does not run the verifyCredentials functions on these pages.
//The user should not be logged in on the login page.
var ignoreVerifyCredentials = ["/login"]; 

document.addEventListener('DOMContentLoaded', function() {
    console.log("decodes.js has been loaded.");
    initializeUserDropdown();
    if (!ignoreVerifyCredentials.some(pageSuffix => (window.location.href.endsWith(pageSuffix) || window.location.href.endsWith(pageSuffix + "#")))) 
    {
        console.log("Running Verify Credentials.");
        verifyCredentials();
    }
    else
    {
        console.log("Skipping Verify Credentials.");
    }
});

/**
 * Initializes the menu dropdown based on logged in status.
 */
function initializeUserDropdown()
{
    var username = sessionStorage.getItem("username");
    if (username != null)
    {
        $("#usernameDropdownText").text(username);
    }
}

/**
 * Verifies that the user is logged by checking the token in and that his/her token has not expired.
 * A call is made to the API and if the token is valid, the page stays open.
 * If the token is invalid, the user is redirected to the login page.
 */
function verifyCredentials()
{
    console.log("Verifying token is valid.")
    var params = {};

    $.ajax({
        url: `${window.API_URL}/check`,
        type: "GET",
        data: params,
        dataType_json: "json",
        dataType: "text",
        success: function(response) {
            console.log("Token Verified Successfully.");
        },
        error: function(response) {
            var errorJson = response.responseText;
            show_notification_modal(
                    "Login", 
                    "There was an error logging in.", 
                    (errorJson != null && errorJson.errMessage != null) ? errorJson.errMessage + ".  You will be redirected to the login page." : "There was an error logging in.  You will be redirected to the login page.", 
                            "OK", 
                            "bg-danger", 
                            "bg-secondary",
                            function() {
                        window.location = "login";
                    });
        }
    });
}
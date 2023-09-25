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
    var token = sessionStorage.getItem("token");
    var params = {
            "token": token
    }

    $.ajax({
        url: `../api/gateway?opendcs_api_call=check`,
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
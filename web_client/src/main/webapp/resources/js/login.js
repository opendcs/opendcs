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
    $("#id_username").keyup(function(event) {
        inputBoxLogin(event);
    });
    $("#id_password").keyup(function(event) {
        inputBoxLogin(event);
    });
    $("#id_username").focus();
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
    var username = $("#id_username").val();
    var password = $("#id_password").val();
    var params = {
            "username": username,
            "password": password
    };
    $.ajax({
        url: `../api/gateway?opendcs_api_call=credentials`,
        type: "POST",
        data: JSON.stringify(params),
        headers: {     
            "Content-Type": "application/json"
        },
        success: function(response) {
            sessionStorage.setItem("username", response.username);
            sessionStorage.setItem("token", response.token);
            window.location = "platforms";
        },
        error: function(response) {
            var errorJson;
            var errorMessage;
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
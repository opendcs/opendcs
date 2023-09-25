document.addEventListener("DOMContentLoaded", function(event) {
    console.log("yesNo Modal js loaded.");
    /*
    $("#modal_yesno_no_button").on("click", function(e){
        on_modal_yesno_no_click();
    });
    $("#modal_yesno_yes_button").on("click", function(e){
        on_modal_yesno_yes_click();
    });
*/
});

function hide_yesno_modal()
{
    //set_waiting_modal(undefined);
    $("#yesNoModal").modal("hide");
}

function show_yesno_modal()
{
    //set_waiting_modal(valuesDict);
    $("#yesNoModal").modal("show");
}

function set_yesno_modal(title, messageTitle, message, titleClass, onYesClick, yesButtonClass, onNoClick, noButtonClass)
{
    if (title == undefined)
    {
        title = "";
    }
    if (messageTitle == undefined)
    {
        messageTitle = "";
    }
    if (message == undefined)
    {
        message = "";
    }
    if (titleClass == undefined)
    {
        $("#yesNoModalTitleDiv").removeClass(function (index, css) {
            return (css.match(/(^|\s)bg-\S+/g) || []).join(' ');
        });
        $("#yesNoModalTitleDiv").addClass("bg-success");
    }
    else
    {
        $("#yesNoModalTitleDiv").removeClass(function (index, css) {
            return (css.match(/(^|\s)bg-\S+/g) || []).join(' ');
        });
        $("#yesNoModalTitleDiv").addClass(titleClass);
    }
    if (noButtonClass == undefined)
    {
        $("#modal_yesno_no_button").removeClass(function (index, css) {
            return (css.match(/(^|\s)bg-\S+/g) || []).join(' ');
        });
        $("#modal_yesno_no_button").addClass("bg-secondary");
    }
    else
    {
        $("#modal_yesno_no_button").removeClass(function (index, css) {
            return (css.match(/(^|\s)bg-\S+/g) || []).join(' ');
        });
        $("#modal_yesno_no_button").addClass(noButtonClass);
    }

    if (yesButtonClass == undefined)
    {
        $("#modal_yesno_yes_button").removeClass(function (index, css) {
            return (css.match(/(^|\s)bg-\S+/g) || []).join(' ');
        });
        $("#modal_yesno_yes_button").addClass("bg-success");
    }
    else
    {
        $("#modal_yesno_yes_button").removeClass(function (index, css) {
            return (css.match(/(^|\s)bg-\S+/g) || []).join(' ');
        });
        $("#modal_yesno_yes_button").addClass(yesButtonClass);
    }
    $("#yesNoModalTitle").text(title);
    $("#yesNoModalTextTitle").text(messageTitle);
    $("#yesNoModalText").text(message);

    if (onYesClick == undefined)
    {
        $("#modal_yesno_yes_button").off("click").on("click", function(e){
            on_modal_yesno_yes_click();
        });
    }
    else
    {
        $("#modal_yesno_yes_button").off("click").on("click", onYesClick);
    }

    if (onNoClick == undefined)
    {
        $("#modal_yesno_no_button").off("click").on("click", function(e){
            on_modal_yesno_no_click();
        });
    }
    else
    {
        $("#modal_yesno_no_button").off("click").on("click", onNoClick);
    }
    show_yesno_modal();
}

function on_modal_yesno_no_click(e)
{
    hide_yesno_modal();
}

function on_modal_yesno_yes_click(e)
{
    alert("Action Performed Successfully");
    hide_yesno_modal();
}
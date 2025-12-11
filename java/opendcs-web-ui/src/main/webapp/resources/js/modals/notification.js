document.addEventListener("DOMContentLoaded", function(event) {
    console.log("Notification Modal js loaded.");
});

var notification_modal_defaults = {
    "header_text": {
        "text": "Notification",
        "target": "#notificationModalTitle"
    },
    "body_header_text": {
        "text": "Notification",
        "target": "#notificationModalTextTitle"
    },
    "body_text": {
        "text": "This is a notification",
        "target": "#notificationModalText"
    },
    "button_text": {
        "text": "OK",
        "target": "#notificationModalTitleDiv button"
    }
};

function hide_notification_modal()
{
    set_notification_modal(undefined);
    $("#notificationModal").modal("hide");
}

function show_notification_modal(title, body_header_text, body_text, button_text, header_css, button_css, btnClick)
{
    set_notification_modal(title, body_header_text, body_text, button_text, header_css, button_css, btnClick);
    $("#notificationModal").modal("show");
}

function set_notification_modal(title, body_header_text, body_text, button_text, header_css, button_css, btnClick)
{
    $(notification_modal_defaults.header_text.target).text(title);
    $(notification_modal_defaults.body_header_text.target).text(body_header_text);
    $(notification_modal_defaults.body_text.target).text(body_text);
    $(notification_modal_defaults.button_text.target).text(button_text);

    $("#notificationModalTitleDiv").removeClass()
    $("#notificationModalTitleDiv").addClass("modal-header");
    $("#notificationModalTitleDiv").addClass(header_css);


    $("#notificationModal button").removeClass()
    $("#notificationModal button").addClass("btn");
    $("#notificationModal button").addClass(button_css);
    
    if (btnClick == null)
    {
        okClick = function(e){
            hide_notification_modal();
        };
        $("#notificationModal button").on("click", okClick);
    }
    else
    {
        $("#notificationModal button").on("click", btnClick);
    }
}
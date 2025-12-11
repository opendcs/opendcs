document.addEventListener("DOMContentLoaded", function(event) {
    console.log("Waiting Modal js loaded.");
});

var waiting_modal_defaults = {
    "header_text": {
        "text": "Waiting",
        "target": "#modal_waiting .modal-title"
    },
    "body_header_text": {
        "text": "",
        "target": "#modal_waiting .modal-body h6"
    },
    "body_text": {
        "text": "",
        "target": "#modal_waiting p"
    }
};

function hide_waiting_modal(delay)
{
    if (delay == undefined)
    {
        delay = 0;
    }
    set_waiting_modal(undefined);
    setTimeout(function(){$("#modal_waiting").modal("hide");}, delay);
}

function show_waiting_modal(valuesDict)
{
    set_waiting_modal(valuesDict);
    $("#modal_waiting").modal("show");
}

function set_waiting_modal(valuesDict)
{
    if ((valuesDict == undefined) || (typeof(valuesDict) != "object"))
    {
        valuesDict = {};
    }
    for (var key in waiting_modal_defaults)
    {
        if (!(key in valuesDict))
        {
            valuesDict[key] = waiting_modal_defaults[key]["text"];
        }
        $(waiting_modal_defaults[key]["target"]).text(valuesDict[key]);
    }
}
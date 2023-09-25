document.addEventListener('DOMContentLoaded', function() {
    console.log("dom_utilities.js");
    var elems = Array.prototype.slice.call(document.querySelectorAll('.form-check-input-switchery'));
    elems.forEach(function(html) {
        var switchery = new Switchery(html);
    });

    if ($().select2) {
        console.warn('Warning - select2.min.js is loaded.');
        //Select with search
        $('.select-search').select2();
    }
});
function createElement(elementType, attributes, classes, innerHtml)
{
    var element = $("<" + elementType + ">");
    if (attributes != null)
    {
        element.attr(attributes);
    }
    if (classes != null)
    {
        classes.forEach(cls => {
            element.addClass(cls);
        });
    }
    if (innerHtml != null)
    {
        element.append(innerHtml);
    }
    return element;
}

function createSelectBox(id, options, additionalClasses, onChangeFunction, appendTo, prependTo)
{
    //Don't want the pound sign in the id, though this allows the user to pass it.
    if (typeof id == "string")
    {
        if (id.startsWith("#"))
        {
            id = id.replace("#", "");
        }
    }
    var select = $(`#${id}`);
    if (select.length > 0)
    {
        console.log("Select already exists.  Not creating, just adding options.");
    }
    else
    {
        select = $("<select>");
        if (id != null)
        {
            select.attr({
                "id": id,
                "class": "form-control"
            });
        }
    }
    if (options != null)
    {
        options.forEach(option => {
            var optionAttributes = {
                    "value": option.value
            };
            if (option.selected)
            {
                optionAttributes.selected = true;
            }
            if (option.title != null)
            {
                optionAttributes.title = option.title;
            }
            var newOption = $("<option>").attr(optionAttributes).html(option.text);
            select.append(newOption);
        });
    }
    if (additionalClasses != null)
    {
        additionalClasses.forEach(cls => {
            select.addClass(cls);
        });
    }
    if (onChangeFunction != null)
    {
        select.on("change", onChangeFunction);
    }
    if (appendTo != null)
    {
        select.appendTo(appendTo);
    }
    else if (appendTo != null)
    {
        select.prependTo(appendTo);
    }
    return select;
}

function updateSwitchValue(switchId, newValue)
{
    switchId = switchId.startsWith("#") ? switchId : "#" + switchId; 
    var swtch = $(switchId);
    if (swtch.prop("checked") != newValue)
    {
        swtch.click();
    }
}

function isSwitchChecked(switchId)
{
    switchId = switchId.startsWith("#") ? switchId : "#" + switchId; 
    var swtch = $(switchId);
    return swtch.prop("checked");
}
function deleteObjectPropertiesWithValue(obj, value)
{
    for (var key in obj)
    {
        if (obj[key] == value)
        {
            delete obj[key];
        }
    }
    return obj;
}

function changeObjectPropertyValueToNewValue(obj, oldValue, newValue)
{
    for (var key in obj)
    {
        if (obj[key] === oldValue)
        {
            obj[key] = newValue;
        }
    }
    return obj;
}

function changeArrayValueToNewValue(arr, oldValue, newValue)
{
    for (var x = 0; x < arr.length; x++)
    {
        if (arr[x] == oldValue)
        {
            arr[x] = newValue;
        }
    }
    return arr;
}

/**
 * @param {Object} object
 * @param {string} key
 * @return {any} value
 */
function getParameterCaseInsensitive(object, key) {
    const asLowercase = key.toLowerCase();
    return object[Object.keys(object)
        .find(k => k.toLowerCase() === asLowercase)
        ];
}
function keyValuePairToString(obj, keyValueSeparator, endSeparator)
{
    var returnString = "";
    for (var key in obj)
    {
        returnString += `${key}${keyValueSeparator}${obj[key]}${endSeparator}`;
    }
    return returnString;
}
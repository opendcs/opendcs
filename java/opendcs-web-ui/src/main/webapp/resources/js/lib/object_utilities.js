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
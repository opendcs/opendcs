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

function sortListByProperty(theList, prop, ascending)
{
    //1 sorts ascending, -1 sorts in reverse (descending)
    var ascNum = ascending ? 1 : -1;
    theList.sort((a, b) => (a[prop] > b[prop]) ? ascNum : -ascNum)
    return theList;
}

//Searches a list of objects and returns the first instance of an object (along with the index in the array) with a property equal to the search value.
function findObjectInListByPropValue(theList, prop, value)
{
    var returnObj = {
            "index": -1,
            "object": null
    };
    for (var x = 0; x < theList.length; x++)
    {
        var curObj = theList[x];
        if (curObj != null && curObj[prop] == value)
        {
            returnObj = {
                    "index": x,
                    "object": curObj
            }
            break;
        }
    }
    return returnObj;
}

function listOfObjectsToListByProperty(listOfObjects, property)
{
    var listByProp = listOfObjects.map(a => a[property]);
    return listByProp;
}
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
/**
 * Gets the parameters from a passed relative url.
 * 
 * @param relativeUrl {string}  URL relative to the base path.
 * @param parameter   {string}  The parameter that is being searched for.
 * @returns
 */
function getParamValueFromRelativeUrl(relativeUrl, parameter){
    // Get everything after the `?`
    const [ , paramString ] = relativeUrl.split( '?' );
    const urlParams = new URLSearchParams(paramString);
    var val = urlParams.get(parameter);
    console.log(`URL Search params found ${parameter} to be ${val} in relative url ${relativeUrl}`);
    return val;
}
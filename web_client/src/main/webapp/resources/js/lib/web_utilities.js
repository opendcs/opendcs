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
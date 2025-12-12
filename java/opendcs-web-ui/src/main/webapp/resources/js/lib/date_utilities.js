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

function dateFromDay(year, day){
    var date = new Date(year, 0); // initialize a date in `year-01-01`
    return new Date(date.setDate(day)); // add the number of days
}

function getDashDatestringFromDate(dt, utc)
{

    var dateString = `${dt.getFullYear()}-${(dt.getMonth()+1).toString().padStart(2, '0')}-${(dt.getDate()).toString().padStart(2, '0')}T${dt.getHours().toString().padStart(2, '0')}:${dt.getMinutes().toString().padStart(2, '0')}`;
    if (utc)
    {
        dateString = `${dt.getUTCFullYear()}-${(dt.getUTCMonth()+1).toString().padStart(2, '0')}-${(dt.getUTCDate()).toString().padStart(2, '0')}T${dt.getUTCHours().toString().padStart(2, '0')}:${dt.getUTCMinutes().toString().padStart(2, '0')}`;
    }
    return dateString;
}

function getSecondsFromHHMMSS(hhmmss)
{
    var split = hhmmss.split(":");
    var seconds = split[0]*3600+split[1]*60+(+split[2]);
    return seconds
}

function getHhmmssFromSeconds(seconds)
{
    var hhmmss = new Date(seconds * 1000).toISOString().slice(11, 19);
    return hhmmss;
}

function getDayOfYear(date)
{
    //var now = new Date();
    var start = new Date(date.getFullYear(), 0, 0);
    var diff = date - start;
    var oneDay = 1000 * 60 * 60 * 24;
    var day = Math.floor(diff / oneDay);
    return day;
}
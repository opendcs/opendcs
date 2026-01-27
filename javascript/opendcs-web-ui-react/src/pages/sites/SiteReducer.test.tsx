

import { assert, expect, test } from 'vitest'
import type { UiSite } from './Site';
import { SiteReducer } from './SiteReducer';

test('Reducer Add SiteName', () => {

    const testSite: UiSite = {};

    const result = SiteReducer(testSite, {type: "add_name", playload: {type: "CWMS", name: "Test Site 1"}});
    console.log(result);
    expect(result).toBeDefined();
    expect(result.sitenames).toBeDefined();
    
    expect(result.sitenames?.CWMS).toEqual("Test Site 1");

});


test('Change a SiteName Type', () => {

    const testSite: UiSite = {
        sitenames: {
            CWMS: "Test 1",
            NWSHDB5: "Test1"
        }
    }

    expect(testSite.sitenames).toBeDefined();    
    expect(testSite.sitenames?.CWMS).toEqual("Test 1");
    const result = SiteReducer(testSite, {type:"change_type", playload: {"old_type": "CWMS", "new_type": "local"}});
    expect(result).toBeDefined();
    expect(result.sitenames?.CWMS).not.toBeDefined();
    expect(result.sitenames?.local).toEqual("Test 1");
});
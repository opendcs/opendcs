import type { UiSite } from "./Site";



export type Action = {type: "add_name"; playload: {type: string, name: string}} |
              {type: "change_type"; playload: {old_type: string, new_type: string}};


export function SiteReducer(currentSite: UiSite, action: Action): UiSite {
    switch (action.type) {
        case 'add_name': {
            return {
                ...currentSite,
                sitenames: {
                    ...currentSite.sitenames, 
                    [action.playload.type]: action.playload.name        
                }
            }
        }
        case 'change_type': {
            type sitenames = {[k: string]: string};
            const curKey: keyof sitenames = action.playload.old_type;
            const curValue = currentSite.sitenames![action.playload.old_type];
            const {[curKey]: _, ...currentSiteNames} = currentSite.sitenames;

            return {
                ...currentSite,
                sitenames: {
                    ...currentSiteNames,
                    [action.playload.new_type]: curValue
                }
                
            }
        }
    }
}
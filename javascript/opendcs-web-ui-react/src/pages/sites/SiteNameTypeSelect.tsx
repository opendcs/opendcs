import { FormSelect } from "react-bootstrap";
import { REFLIST_SITE_NAME_TYPE, useRefList } from "../../contexts/data/RefListContext";


export interface SiteNameTypeSelectProperties {
    current: string | undefined;
}

export const SiteNameTypeSelect: React.FC<SiteNameTypeSelectProperties> = ({current}) => {
    const {refList} = useRefList();
    const siteNameTypes = refList(REFLIST_SITE_NAME_TYPE);
    
    console.log(refList);
    console.log(siteNameTypes);

    return (
        <FormSelect name="siteNameType">
            {siteNameTypes.items ?
                Object.values(siteNameTypes.items!)
                      .map((item) => {
                            return <option key={item.value} value={item.value} selected={current === item.value}>{item.value}</option>
                      })
            :
            null
            }
        </FormSelect>
    );
};

export default SiteNameTypeSelect;
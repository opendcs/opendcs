import { FormSelect } from "react-bootstrap";
import { REFLIST_SITE_NAME_TYPE, useRefList } from "../../contexts/data/RefListContext";


export interface SiteNameTypeSelectProperties {
    current: string | undefined;
}

export const SiteNameTypeSelect: React.FC<SiteNameTypeSelectProperties> = ({current}) => {
    const {refList} = useRefList();
    return (
        <FormSelect name="siteNameType">
            {refList("nametype").items ?
                Object.values(refList(REFLIST_SITE_NAME_TYPE).items!)
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
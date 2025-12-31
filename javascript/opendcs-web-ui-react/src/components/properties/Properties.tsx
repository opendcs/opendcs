import DataTable, { type DataTableProps, type DataTableRef, type DataTableSlots } from "datatables.net-react";
import DT, { type ConfigColumns } from "datatables.net-bs5";
import dtButtons from "datatables.net-buttons-bs5"
import { Button, Container, Form } from "react-bootstrap";
import 'datatables.net-responsive';
import { Pencil, Save, Trash } from "react-bootstrap-icons";
import type { ApiPropSpec } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { useRef } from "react";
import { renderToString } from "react-dom/server";
  

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(dtButtons);


export interface Property {
    name: string;
    value: unknown;
    spec?: ApiPropSpec;
}

export interface PropertiesTableProps {
    theProps: Property[];
    saveProp: (prop: Property) => void;
    removeProp: (prop: string) => void,
    width?: React.CSSProperties['width'];
    height?: React.CSSProperties['height'];
    classes?: string;
};
export interface ActionProps {
    data: Property,
    editMode: boolean,
    removeProp: (prop: string) => void,
    editProp: (prop: string) => void,
    saveProp: (prop: Property) => void,
}

export const PropertyActions: React.FC<ActionProps> = ({data, editMode, removeProp, editProp, saveProp}) => {
    console.log(editMode);
    return (
        <>
            {editMode === true ?
                <Button onClick={() => {saveProp(data)}} variant="primary"
                        aria-label={`save property named ${data.name}`} size='sm'><Save/></Button>
                :
                <Button onClick={() => editProp(data.name)} variant="warning"
                        aria-label={`edit property named ${data.name}`} size='sm'><Pencil /></Button>
            }
            <Button onClick={() => removeProp(data.name)} variant='danger' size='sm'
                    aria-label={`delete property named ${data.name}`}><Trash /></Button>
        </>
    )
};

export const PropertiesTable: React.FC<PropertiesTableProps> = ({theProps, saveProp, removeProp, width = '20em', height = '100vh', classes = ''}) => {
    const table = useRef<DataTableRef>(null);

    const renderEditable = (data: string | number | readonly string[] | undefined, type: string, row: any, meta: any) =>{
        if (type !== "display") {
            return data;
        }
        const api = table.current!.dt();
        const rowNode = api?.row(meta.row).node();
        if (rowNode?.dataset.edit === "true" || data === "") {
            return renderToString(<Form.Control type="text" name={meta.settings.aoColumns[meta.col].mData} defaultValue={data} />);
        } else {
            return data;
        }
    };

    const columns: ConfigColumns[]  = [
        {data: "name", render: renderEditable},
        {data: "value", render: renderEditable,},
        {data: null, name:"actions"}
    ];
    const options: DataTableProps["options"] = {
        paging: false,
        responsive: true,
        layout: {
            top1Start: {
                buttons: [
                    {
                        text:'+',
                        action: () => {
                            const api = table.current!.dt();
                            if (api) {
                                api.row.add({name: "", value:""}).draw();
                            }
                        },
                    }
                ]
            }
        },
        createdRow: (row: HTMLElement, data: Property) => {
            row.dataset.propName = data.name;
            if (data.name === "") {
                row.dataset.edit = "true";
            } else {
                row.dataset.edit = "false";
            }
        },
    };

    const getAndSaveProp = (data: Property): void => {
        const api = table.current!.dt();
        console.log(api);
        if (api) {
            const row = api.row(`tr[data-prop-name="${data.name}"]`)?.node();
            
            if (row) {
                const tds = row.children;
                console.log(tds);
                const name = tds[0].hasChildNodes() ? (tds[0].children[0] as HTMLInputElement).value : tds[0].innerHTML;
                const value = (tds[1].children[0] as HTMLInputElement).value;
                const prop: Property = {name: name, value: value}
                console.log(prop);
                saveProp(prop);
                
            }
        }
    };

    const slots: DataTableSlots = {
        actions: (data: Property, type: unknown, row: Property) => {
            if (type === 'display') {
                const api = table.current!.dt();
                let inEdit = false;
                if (api) {
                    const rowNode = api?.row(`tr[data-prop-name="${data.name}"]`).node();
                    // either the row doesn't exist yet or the value is set.
                    inEdit = row.name === "" ? true : rowNode?.dataset.edit === "true";
                }

                return (<PropertyActions data={data} editMode={inEdit} removeProp={removeProp} saveProp={getAndSaveProp} editProp={(name: string) => {
                    const api = table.current!.dt();
                    const rowNode = api?.row(`tr[data-prop-name="${name}"]`);
                    if (rowNode) {
                        rowNode.node().dataset.edit = "true";
                        rowNode.draw();
                        api?.draw();
                        api?.rows().invalidate();
                    }
                }}/>)
            } else {
                return data;
            }
        } 
    };

    return (
        <Container fluid style={{width: width, height: height}} className={`${classes}`}>
            <DataTable columns={columns} data={theProps} options={options} slots={slots} ref={table}
                           className="table table-hover table-striped tablerow-cursor w-100 border">
                <caption className="captionTitleCenter">
					Properties
                </caption>
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Value</th>
                        <th>Actions</th>
                    </tr>
                </thead>
            </DataTable>
        </Container>
    );
};

export default PropertiesTable;
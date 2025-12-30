import DataTable, { type DataTableProps, type DataTableRef, type DataTableSlots } from "datatables.net-react";
import DT, { type ConfigColumns, type RowSelector } from "datatables.net-bs5";
import dtButtons from "datatables.net-buttons-bs5"
import { Button, Container, Form } from "react-bootstrap";
import 'datatables.net-responsive';
import { Pencil, Trash } from "react-bootstrap-icons";
import type { ApiPropSpec } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { useRef } from "react";
import { renderToString } from "react-dom/server";
  

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);
DataTable.use(dtButtons);


export interface Property {
    name: string;
    value: unknown;
    spec?: ApiPropSpec;
}

export interface PropertiesTableProps {
    theProps: Property[];
    addProp: () => void;
    removeProp: (prop: string) => void,
    width?: React.CSSProperties['width'];
    height?: React.CSSProperties['height'];
    classes?: string;
};
export interface ActionProps {
    data: Property,
    removeProp: (prop: string) => void,
    editProp: (prop: string) => void
}

export const PropertyActions: React.FC<ActionProps> = ({data, removeProp, editProp}) => {
    return (
        <>
            <Button onClick={() => editProp(data.name)} variant="warning" aria-label="edit property" size='sm'><Pencil /></Button>
            <Button onClick={() => removeProp(data.name)} variant='danger' size='sm'
                    aria-label="delete property"><Trash /></Button>
        </>
    )
};

export const PropertiesTable: React.FC<PropertiesTableProps> = ({theProps, addProp, removeProp, width = '20em', height = '100vh', classes = ''}) => {
    const table = useRef<DataTableRef>(null);


    const renderEditable = (data: string | number | readonly string[] | undefined, type: string, row: any, meta: any) =>{
        if (type !== "display") {
            return data;
        }
        const api = table.current!.dt();
        const rowNode = api?.row(meta.row).node();
        console.log(row);
        console.log(data);
        if (rowNode?.getAttribute('data-edit') === "true") {
            return renderToString(<Form.Control type="text" name={meta.settings.aoColumns[meta.col].mData} defaultValue={data} />);
        } else {
            return data;
        }
    };

    const columns: ConfigColumns[]  = [
        {
            data: "name",
            render: renderEditable,
        },
        {data: "value", render: renderEditable},
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
                        action: () => {addProp()},
                    }
                ]
            }
        },
        
        createdRow: (row: HTMLElement, data: Property) => {
            console.log(row);
            console.log(data);
            console.log("end");
            row.setAttribute("data-prop-name", data.name);
            row.setAttribute("data-edit", "false");
        },
    };

    const slots: DataTableSlots = {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        actions: (data: Property, type: unknown, row: unknown) => {
            if (type === 'display') {
                return (<PropertyActions data={data} removeProp={removeProp} editProp={(name: string) => {
                    let api = table.current!.dt();
                    const row = api?.row(`tr[data-prop-name="${name}"]`)!;
                    console.log(row);
                    row.node().setAttribute("data-edit","true");
                    
                    row.draw();
                    api?.draw();
                    api?.rows().invalidate();
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
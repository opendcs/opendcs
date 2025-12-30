import DataTable, { type DataTableProps, type DataTableSlots } from "datatables.net-react";
import DT, { type ConfigColumns } from "datatables.net-bs5";
import dtButtons from "datatables.net-buttons-bs5"
import { Button, Container } from "react-bootstrap";
import 'datatables.net-responsive';
import { Pencil, Trash } from "react-bootstrap-icons";
import type { ApiPropSpec } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
  

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
        <div>
            <Button onClick={() => editProp(data.name)} variant="warning" aria-label="edit property" size='sm'><Pencil /></Button>
            <Button onClick={() => removeProp(data.name)} variant='danger' size='sm'
                    aria-label="delete property"><Trash /></Button>
        </div>
    )
};

export const PropertiesTable: React.FC<PropertiesTableProps> = ({theProps, addProp, removeProp, width = '20em', height = '100vh', classes = ''}) => {
    const columns: ConfigColumns[]  = [
        {data: "name", render: (data, type, row, meta) => {
            console.log("Data: " + JSON.stringify(data));
            console.log("Type: " + type);
            console.log("Row: " + JSON.stringify(row));
            console.log(meta);
            return data;
        }},
        {data: "value",},
        {data: null, name:"actions"}
    ];
    const options: DataTableProps["options"] = {
        paging: false,
        stripeClasses: ["bg-primary-subtle", "bg-subtle"],
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
        }
    };

    const slots: DataTableSlots = {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        actions: (data: Property, type: unknown, row: unknown) => {
            if (type === 'display') {
                return (<PropertyActions data={data} removeProp={removeProp} editProp={(name: string) => {
                    
                }}/>)
            } else {
                return data;
            }
        } 
    };

    return (
        <Container fluid style={{width: width, height: height}} className={`${classes}`}>
            <DataTable columns={columns} data={theProps} options={options} slots={slots}
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
import DataTable, { type DataTableProps } from "datatables.net-react";
import DT from "datatables.net-bs5";
import { Button } from "react-bootstrap";
  

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

interface Property {
    name: string;
    value: unknown;
    tooltip?: string;
}

export default function Properties(theProps: Property[]) {
    const columns = [
        {data: "name"},
        {data: "value"}
    ];
    const options: DataTableProps["options"] = {

    };

    // eslint-disable-next-line @typescript-eslint/no-unused-vars 
    const addProp = (_e: unknown) => {theProps.push({name: "test", value: "test value"})}

    return (
        <div>
            <Button onClick={addProp}>+</Button>
            <DataTable columns={columns} data={theProps} options={options}
                           className="table-hover table-striped datatable-responsive tablerow-cursor w-100">
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Value</th>
                    </tr>
                </thead>
            </DataTable>
        </div>
    );
}
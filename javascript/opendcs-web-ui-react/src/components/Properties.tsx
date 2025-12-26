import DataTable, { type DataTableProps } from "datatables.net-react";
import DT from "datatables.net-bs5";
import { Button, Container } from "react-bootstrap";
import 'datatables.net-responsive-dt';
  

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export interface Property {
    name: string;
    value: unknown;
    tooltip?: string;
}

interface PropertiesTableProps {
    theProps: Property[];
    addProp: () => void;
    width?: React.CSSProperties['width'];
    height?: React.CSSProperties['height'];
    classes?: string;
};

export const PropertiesTable: React.FC<PropertiesTableProps> = ({theProps, addProp, width = '20em', height = '100vh', classes = ''}) => {
    const columns = [
        {data: "name"},
        {data: "value"},
        {data: null}
    ];
    const options: DataTableProps["options"] = {
        paging: false,
        stripeClasses: ["bg-primary-subtle", "bg--subtle"],
        responsive: true
    };


    return (
        <Container fluid style={{width: width, height: height}} className={`${classes}`}>
            <DataTable columns={columns} data={theProps} options={options}
                           className="table-hover table-striped tablerow-cursor w-100 border">
                <caption className="captionTitleCenter">
					Properties
                    <Button variant="secondary" className="float-right captionButton mt-1"
                        onClick={
                        // eslint-disable-next-line @typescript-eslint/no-unused-vars
                        (_e: unknown) => addProp()
                    }>+</Button>
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
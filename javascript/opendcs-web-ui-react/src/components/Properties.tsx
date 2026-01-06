import DataTable, { type DataTableProps } from "datatables.net-react";
import DT from "datatables.net-bs5";
import { Button, Container } from "react-bootstrap";
import "datatables.net-responsive";
import "react-bootstrap-icons";
import { Trash } from "react-bootstrap-icons";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export interface Property {
  name: string;
  value: unknown;
  tooltip?: string;
}

export interface PropertiesTableProps {
  theProps: Property[];
  addProp: () => void;
  removeProp: (prop: string) => void;
  width?: React.CSSProperties["width"];
  height?: React.CSSProperties["height"];
  classes?: string;
}

export const PropertiesTable: React.FC<PropertiesTableProps> = ({
  theProps,
  addProp,
  removeProp,
  width = "20em",
  height = "100vh",
  classes = "",
}) => {
  const columns = [{ data: "name" }, { data: "value" }, { data: null }];
  const options: DataTableProps["options"] = {
    paging: false,
    stripeClasses: ["bg-primary-subtle", "bg--subtle"],
    responsive: true,
  };

  const slots = {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    2: (data: Property, row: unknown) => {
      console.log(row);
      return (
        <Button
          onClick={() => removeProp(data.name)}
          variant="warning"
          size="sm"
          aria-label={`delete property named ${data.name}`}
        >
          <Trash />
        </Button>
      );
    },
  };

  return (
    <Container fluid style={{ width: width, height: height }} className={`${classes}`}>
      <DataTable
        columns={columns}
        data={theProps}
        options={options}
        slots={slots}
        className="table table-hover table-striped tablerow-cursor w-100 border"
      >
        <caption className="captionTitleCenter">
          Properties
          <Button
            variant="secondary"
            className="float-right captionButton mt-1"
            onClick={
              // eslint-disable-next-line @typescript-eslint/no-unused-vars
              (_e: unknown) => addProp()
            }
            aria-label="add property"
          >
            +
          </Button>
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

import DataTable, {
  type DataTableProps,
  type DataTableRef,
  type DataTableSlots,
} from "datatables.net-react";
import DT, { type ConfigColumns } from "datatables.net-bs5";
import dtButtons from "datatables.net-buttons-bs5";
import { Button, Container, Form } from "react-bootstrap";
import "datatables.net-responsive";
import { Pencil, Save, Trash } from "react-bootstrap-icons";
import type { ApiPropSpec } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { useMemo, useRef } from "react";
import { renderToString } from "react-dom/server";
import { useTranslation } from "react-i18next";

// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(dtButtons);

/**
 * NOTE: this should primarily be extracted from the API spec with the state value
 * added as a type extension.
 */
export interface Property {
  name: string;
  value: unknown;
  spec?: ApiPropSpec;
  state?: "new" | "edit" | number; // Table component will add this if new or in edit mode
  // if value is 'new' name will be an index value.
  // number will be the initial ID number if this was new
}

export interface PropertiesTableProps {
  theProps: Property[];
  saveProp: (prop: Property) => void;
  removeProp: (prop: string) => void;
  addProp: () => void;
  editProp: (prop: string) => void;
  width?: React.CSSProperties["width"];
  height?: React.CSSProperties["height"];
  classes?: string;
}

export interface ActionProps {
  data: Property;
  editMode: boolean;
  removeProp: (prop: string) => void;
  editProp: (prop: string) => void;
  saveProp: (prop: Property) => void;
}

export const PropertyActions: React.FC<ActionProps> = ({
  data,
  editMode,
  removeProp,
  editProp,
  saveProp,
}) => {
  const [t] = useTranslation(["properties"]);
  return (
    <>
      {editMode === true ? (
        <Button
          onClick={() => {
            saveProp(data);
          }}
          variant="primary"
          aria-label={t("save prop", {name: data.name})}
          size="sm"
        >
          <Save />
        </Button>
      ) : (
        <Button
          onClick={() => editProp(data.name)}
          variant="warning"
          aria-label={t("edit prop", {name: data.name})}
          size="sm"
        >
          <Pencil />
        </Button>
      )}
      <Button
        onClick={() => removeProp(data.name)}
        variant="danger"
        size="sm"
        aria-label={t("delete prop", {name: data.name})}
      >
        <Trash />
      </Button>
    </>
  );
};

/**
 * Properties table component for use by all pages. Control of the properties by what uses the component to the save and remove
 * method provided should behave as such.
 *
 * Developer Note: You may notice we are likely doing direct dom manipulation, this is due to the use od DataTables.net which assumes it has control
 * of the dom. There is little to be done in the regard so we'll accept it, but try to keep it minimal.
 *
 */
export const PropertiesTable: React.FC<PropertiesTableProps> = ({
  theProps,
  saveProp,
  removeProp,
  addProp,
  editProp,
  width = "20em",
  height = "100vh",
  classes = "",
}) => {
  const table = useRef<DataTableRef>(null);
  const [t] = useTranslation();

  const renderEditable = (
    data: string | number | readonly string[] | undefined,
    type: string,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    row: any,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    meta: any,
  ) => {
    if (type !== "display") {
      return data;
    }
    const inputName: string = meta.col == 0 ? "name" : "value";
    if (row?.state === "new" || (row?.state == "edit" && meta.col == 1)) {
      return renderToString(
        <Form.Control
          type="text"
          name={meta.settings.aoColumns[meta.col].mData}
          defaultValue={row.state == "edit" ? data : ""}
          aria-label={t(`${inputName} input`, {name: row.name})}
        />,
      );
    } else {
      return data;
    }
  };

  const columns : ConfigColumns[] = [
      { data: "name", render: renderEditable },
      { data: "value", render: renderEditable },
      { data: null, name: "actions" },
    ];

  const options: DataTableProps["options"] = {
    paging: false,
    responsive: true,
    layout: {
      top1Start: {
        buttons: [
          {
            text: "+",
            action: () => {
              addProp();
            },
            attr: {
              "aria-label": t("add prop"),
            },
          },
        ],
      },
    },
    createdRow: (
      row: HTMLElement,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      data: any,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      _index: number,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      _cells: HTMLTableCellElement[],
    ) => (row.dataset.propName = data.name),
  };

  const getAndSaveProp = (data: Property): void => {
    const api = table.current?.dt();
    if (api) {
      const row = api.row(`tr[data-prop-name="${data.name}"]`)?.node();

      if (row) {
        const tds = row.children;
        const name =
          tds[0].children[0] instanceof HTMLInputElement
            ? (tds[0].children[0] as HTMLInputElement).value
            : tds[0].innerHTML;
        const value = (tds[1].children[0] as HTMLInputElement).value;
        const prop: Property = {
          name: name,
          value: value,
          state: data.state == "new" ? parseInt(data.name) : data.state,
        };
        saveProp(prop);
      } else {
        console.log(`can't find entries for ${JSON.stringify(data)}`);
      }
    } else {
      console.log("no dt api?");
    }
  };

  const slots: DataTableSlots = {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    actions: (data: Property, type: unknown, _row: Property) => {
      if (type === "display") {
        const inEdit = data.state !== undefined;
        return (
          <PropertyActions
            data={data}
            editMode={inEdit}
            removeProp={removeProp}
            saveProp={getAndSaveProp}
            editProp={editProp}
          />
        );
      } else {
        return data;
      }
    },
  };

  return (
    <Container fluid style={{ width: width, height: height }} className={`${classes}`}>
      <DataTable
        columns={columns}
        data={theProps}
        options={options}
        slots={slots}
        ref={table}
        className="table table-hover table-striped tablerow-cursor w-100 border"
      >
        <caption className="captionTitleCenter">t("PropertiesTitle")</caption>
        <thead>
          <tr>
            <th>{t("name")}</th>
            <th>{t("value")}</th>
            <th>{t("actions")}</th>
          </tr>
        </thead>
      </DataTable>
    </Container>
  );
};

export default PropertiesTable;

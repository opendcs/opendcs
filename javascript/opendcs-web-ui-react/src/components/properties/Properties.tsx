import DataTable, {
  type DataTableProps,
  type DataTableRef,
  type DataTableSlots,
} from "datatables.net-react";
import DT, { type ConfigButtons, type ConfigColumns } from "datatables.net-bs5";
import dtButtons from "datatables.net-buttons-bs5";
import { Button, Container, Form } from "react-bootstrap";
import "datatables.net-responsive";
import { Pencil, Save, Trash } from "react-bootstrap-icons";
import type { ApiPropSpec } from "../../../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { renderToString } from "react-dom/server";
import { useTranslation } from "react-i18next";
import { dtLangs } from "../../lang";
import type { CollectionActions } from "../../util/Actions";
import type { RowState } from "../../util/DataTables";

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
  value: string;
  spec?: ApiPropSpec;
  state?: "new" | "edit" | number; // Table component will add this if new or in edit mode
  // if value is 'new' name will be an index value.
  // number will be the initial ID number if this was new
}

export interface PropertiesTableProps {
  theProps: Property[];
  edit?: boolean; // can we edit existing props?
  canAdd?: boolean; // can we add additional props?
  actions: CollectionActions<Property, string>;
  width?: React.CSSProperties["width"];
  height?: React.CSSProperties["height"];
  classes?: string;
}

export interface ActionProps {
  data: Property;
  editMode: boolean;
  removeProp?: (prop: string) => void;
  editProp?: (prop: string) => void;
  saveProp?: (prop: Property) => void;
}

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
  actions,
  edit = false,
  canAdd = true,
  width = "20em",
  height = "100vh",
  classes = "",
}) => {
  const table = useRef<DataTableRef>(null);
  const [t, i18n] = useTranslation(["properties"]);
  const [localProps, setLocalProps] = useState<Property[]>([]);
  const [rowState, setRowState] = useState<RowState<string>>({});

  const rowStateRef = useRef(rowState);
  const localPropsRef = useRef(localProps);

  useEffect(() => {
    rowStateRef.current = rowState;
  }, [rowState]);
  useEffect(() => {
    localPropsRef.current = localProps;
  }, [localProps]);

  const allProps = useMemo(() => [...theProps, ...localProps], [theProps, localProps]);

  const renderName = useCallback(
    (
      data: string | number | readonly string[] | undefined,
      type: string,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      row: any,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      _meta: any,
    ) => {
      if (type !== "display") {
        return data;
      }
      const state = rowStateRef.current[row.name];
      if (data === undefined || state === "new") {
        return renderToString(
          <Form.Control
            type="text"
            name="name"
            defaultValue={state == "edit" ? data : ""}
            aria-label={t(`properties:name_input`, { name: row.name })}
          />,
        );
      } else {
        return data;
      }
    },
    [rowStateRef],
  );

  const renderValue = useCallback(
    (
      data: string | number | readonly string[] | undefined,
      type: string,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      row: any,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      _meta: any,
    ) => {
      if (type !== "display") {
        return data;
      }
      const state = rowStateRef.current[row.name];
      if (data === undefined || state === "new") {
        return renderToString(
          <Form.Control
            type="text"
            name="value"
            defaultValue={state == "edit" ? data : ""}
            aria-label={t(`properties:value_input`, { name: row.name })}
          />,
        );
      } else {
        return data;
      }
    },
    [rowStateRef],
  );

  const columns: ConfigColumns[] = useMemo(
    () => [
      { data: "name", render: renderName },
      { data: "value", render: renderValue },
      { data: null, name: "actions" },
    ],
    [renderName, renderValue],
  );

  const buttons: ConfigButtons = useMemo(() => {
    if (edit && canAdd) {
      return {
        buttons: [
          {
            text: "+",
            action: () => {
              setLocalProps((prev) => {
                const existingNew = prev
                  .filter((p: Property) => p.state === "new")
                  .sort((a: Property, b: Property) => {
                    return Number.parseInt(a.name) - Number.parseInt(b.name);
                  });
                const idx =
                  existingNew.length > 0 ? Number.parseInt(existingNew[0].name) + 1 : 1;
                setRowState((prev) => {
                  return { ...prev, [`${idx}`]: "new" };
                });
                return [...prev, { name: `${idx}`, value: "" }];
              });
            },
            attr: {
              "aria-label": t("properties:add_prop"),
            },
          },
        ],
      };
    } else {
      return { buttons: [] };
    }
  }, [actions, edit, canAdd]);

  const options: DataTableProps["options"] = useMemo(() => {
    return {
      paging: false,
      responsive: true,
      language: dtLangs.get(i18n.language),
      layout: {
        top1Start: {
          buttons: buttons,
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
  }, [buttons]);

  const getAndSaveProp: (data: Property) => void = useCallback(
    (data: Property): void => {
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
          };
          if (!(name === undefined || name.trim() === "")) {
            actions.save?.(prop);
            if (rowStateRef.current[name] === "new") {
              setRowState((prev) => {
                const { [name]: _, ...others } = prev;
                return {
                  ...others,
                };
              });
            }
          } else {
            (tds[0].children[0] as HTMLElement).classList.add(
              "border",
              "border-warning",
            );
          }
        } else {
          console.log(`can't find entries for ${JSON.stringify(data)}`);
        }
      } else {
        console.log("no dt api?");
      }
    },
    [actions, rowStateRef],
  );

  const renderActions = useCallback(
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    (data: Property, _row: Property) => {
      const state = rowStateRef.current[data.name];
      const inEdit = state !== undefined;
      return (
        <>
          {edit && inEdit && (
            <Button
              onClick={() => {
                getAndSaveProp(data);
              }}
              variant="primary"
              aria-label={t("properties:save_prop", { name: data.name })}
              size="sm"
            >
              <Save />
            </Button>
          )}

          {edit && !inEdit && (
            <Button
              onClick={() => {
                setRowState((prev) => {
                  return {
                    ...prev,
                    [data.name]: "edit",
                  };
                });
              }}
              variant="warning"
              aria-label={t("properties:edit_prop", { name: data.name })}
              size="sm"
            >
              <Pencil />
            </Button>
          )}
          {((edit && actions.remove !== undefined) || state === "new") && (
            <Button
              onClick={() => {
                setLocalProps((prev) => {
                  return [...prev.filter((p) => p.name !== data.name)];
                });
                actions.remove!(data.name);
              }}
              variant="danger"
              size="sm"
              aria-label={t("properties:delete_prop", { name: data.name })}
            >
              <Trash />
            </Button>
          )}
        </>
      );
    },
    [t, actions.remove, edit, rowStateRef, getAndSaveProp],
  );

  const slots = useMemo<DataTableSlots>(() => {
    return {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      actions: renderActions,
    };
  }, [actions, rowStateRef]);

  return (
    <Container fluid style={{ width: width, height: height }} className={`${classes}`}>
      <DataTable
        key={i18n.language} // convient way to force a render of the components and aria labels on a language change.
        columns={columns}
        data={allProps}
        options={options}
        slots={slots}
        ref={table}
        className="table table-hover table-striped tablerow-cursor w-100 border"
      >
        <caption className="captionTitleCenter">
          {t("properties:PropertiesTitle")}
        </caption>
        <thead>
          <tr>
            <th>{t("translation:name")}</th>
            <th>{t("translation:value")}</th>
            {edit && <th>{t("translation:actions")}</th>}
          </tr>
        </thead>
      </DataTable>
    </Container>
  );
};

export default PropertiesTable;

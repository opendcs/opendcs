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
  // if value is 'new' name will be an index value.
  // number will be the initial ID number if this was new
}

export type LocalProperty = Partial<Property> & {
  idx: number;
};

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
  const [localProps, setLocalProps] = useState<LocalProperty[]>([]);
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

  console.log(`The props ${JSON.stringify(theProps)}`);
  console.log(`Local props ${JSON.stringify(localProps)}`);
  console.log(`All props ${JSON.stringify(allProps)}`);
  const renderName = useCallback(
    (
      data: LocalProperty,
      type: string,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      _row: any,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      _meta: any,
    ) => {
      if (type !== "display") {
        return data;
      }
      const state = rowStateRef.current[data.idx];
      if (data.name === undefined || state === "new") {
        return renderToString(
          <Form.Control
            type="text"
            name="name"
            defaultValue=""
            aria-label={t(`properties:name_input`, { name: data.name || data.idx })}
          />,
        );
      } else {
        return data.name;
      }
    },
    [rowStateRef],
  );

  const renderValue = useCallback(
    (
      data: LocalProperty,
      type: string,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      _row: any,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      _meta: any,
    ) => {
      if (type !== "display") {
        return data;
      }
      const state = data.name ? rowStateRef.current[data.name] : "new";
      console.log(`Current data ${JSON.stringify(data)} state ${state}`);
      if (state === undefined) {
        return data.value || "";
      } else {
        return renderToString(
          <Form.Control
            type="text"
            name="value"
            defaultValue={state == "edit" ? data.value : ""}
            aria-label={t(`properties:value_input`, { name: data.name || data.idx })}
          />,
        );
      }
    },
    [rowStateRef],
  );

  const dataColumns = useMemo(
    () => [
      { data: null, render: renderName },
      { data: null, render: renderValue },
    ],
    [renderName, renderValue],
  );

  const columns: ConfigColumns[] = useMemo(
    () => (edit ? [...dataColumns, { data: null, name: "actions" }] : dataColumns),
    [edit, dataColumns],
  );

  const buttons: ConfigButtons = useMemo(() => {
    if (edit && canAdd) {
      return {
        buttons: [
          {
            text: "+",
            action: () => {
              setLocalProps((prev) => {
                const sortAsc = (a: LocalProperty, b: LocalProperty) => {
                  return a.idx - b.idx;
                };
                const existingNew = [...prev].sort(sortAsc);
                const idx = existingNew.length > 0 ? existingNew[0].idx + 1 : 1;
                return [...prev, { idx: idx }];
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

  const getAndSaveProp: (data: LocalProperty) => void = useCallback(
    (data: LocalProperty): void => {
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
          if (name === undefined || name.trim() === "") {
            (tds[0].children[0] as HTMLElement).classList.add(
              "border",
              "border-warning",
            );
          } else {
            console.log(`Saving! ${JSON.stringify(prop)}`);
            actions.save?.(prop);

            setLocalProps((prevProps) => prevProps.filter((p) => p.idx !== data.idx));

            setRowState((prev) => {
              const { [name]: _, ...others } = prev;
              return {
                ...others,
              };
            });
          }
        } else {
          console.log(`can't find entries for ${JSON.stringify(data)}`);
        }
      } else {
        console.log("no dt api?");
      }
    },
    [actions, setRowState, setLocalProps],
  );

  const renderActions = useCallback(
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    (data: LocalProperty, _row: Property) => {
      console.log(`Row state ${JSON.stringify(rowStateRef.current)}`);
      const rowState = rowStateRef.current;
      const state = data.name ? rowState[data.name] : rowState[data.idx];
      const inEdit = state !== undefined || data.idx !== undefined;
      return (
        <>
          {edit && inEdit && (
            <Button
              onClick={() => {
                getAndSaveProp(data);
              }}
              variant="primary"
              aria-label={t("properties:save_prop", { name: data.name || data.idx })}
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
                    [data.name!]: "edit",
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
                setLocalProps((prev) => prev.filter((p) => p.idx !== data.idx));
                if (data.name) {
                  actions.remove!(data.name);
                }
              }}
              variant="danger"
              size="sm"
              aria-label={t("properties:delete_prop", { name: data.name || data.idx })}
            >
              <Trash />
            </Button>
          )}
        </>
      );
    },
    [t, actions.remove, edit, rowStateRef, setRowState, setLocalProps, getAndSaveProp],
  );

  const slots = useMemo<DataTableSlots>(() => {
    return {
      actions: renderActions,
    };
  }, [actions, rowStateRef]);

  useEffect(() => {
    table.current?.dt()?.rows().invalidate().draw(false);
  }, [rowState, allProps]);

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

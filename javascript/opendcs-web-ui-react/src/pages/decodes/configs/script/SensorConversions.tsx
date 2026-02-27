import type React from "react";
import DataTable, {
  type DataTableProps,
  type DataTableRef,
} from "datatables.net-react";
import DT from "datatables.net-bs5";
import { ApiConfigSensor, type ApiConfigScriptSensor } from "opendcs-api";
import { useCallback, useEffect, useLayoutEffect, useMemo, useRef } from "react";
import UnitSelect from "../../../../components/controls/UnitSelector";
import { useContextWrapper } from "../../../../util/ContextWrapper";
import UnitConversionAlgorithmSelect from "../../../../components/controls/UnitConversionAlgorithmSelector";
import { FormControl } from "react-bootstrap";
// this isn't a hook, it just has "use" as the name.
// eslint-disable-next-line react-hooks/rules-of-hooks
DataTable.use(DT);

export interface SensorConversionProperties {
  scriptSensors: ApiConfigScriptSensor[];
  configSensors: { [k: number]: ApiConfigSensor };
  sensorChanged?: (sensor: ApiConfigScriptSensor) => void;
  edit?: boolean;
}

const colToCoeff = (col: number): string => {
  switch (col) {
    case 4:
      return "a";
    case 5:
      return "b";
    case 6:
      return "c";
    case 7:
      return "d";
    case 8:
      return "e";
    case 9:
      return "f";
    default:
      return "na";
  }
};

const SensorConversion: React.FC<SensorConversionProperties> = ({
  scriptSensors,
  configSensors,
  sensorChanged,
  edit = false,
}) => {
  const table = useRef<DataTableRef>(null);
  const inputRef = useRef<{ name: string; position: number }>(null);
  const { toDom } = useContextWrapper();

  const updateSensor = useCallback(
    (sensor: ApiConfigScriptSensor) => {
      sensorChanged?.(sensor);
    },
    [sensorChanged],
  );

  const renderCoefficient = useCallback(
    (
      data: ApiConfigScriptSensor,
      type: string,
      _row: unknown,
      meta: { col: number; row: number },
    ) => {
      if (type === "display") {
        const coeffIdx = colToCoeff(meta.col) as keyof typeof data.unitConverter;
        const coeff = data.unitConverter?.[coeffIdx];
        const inputName = `input_coeff_${data.sensorNumber}_${coeffIdx}`;
        return toDom(
          <FormControl
            value={coeff}
            disabled={!edit}
            id={inputName}
            name={inputName}
            onChange={(evt) => {
              updateSensor({
                ...data,
                unitConverter: {
                  ...data.unitConverter,
                  [coeffIdx]: evt.currentTarget.value,
                },
              });
              inputRef.current = { name: inputName, position: 0 };
            }}
          />,
        );
      } else {
        return data;
      }
    },
    [table, updateSensor],
  );

  const columns = [
    { data: "sensorNumber", defaultContent: "" },
    {
      data: null, // sensorName
      render: (data: ApiConfigScriptSensor, type: string, _row: unknown) => {
        if (type === "display") {
          return configSensors[data.sensorNumber!].sensorName;
        } else {
          return data;
        }
      },
    },
    {
      data: null, // units
      render: (
        data: ApiConfigScriptSensor,
        type: string,
        _row: unknown,
        _meta: unknown,
      ) => {
        if (type === "display") {
          const unitAbbr: string | undefined = data.unitConverter?.toAbbr;
          return toDom(
            <UnitSelect
              current={unitAbbr}
              disabled={!edit}
              onChange={(evt) => {
                inputRef.current = null;
                updateSensor({
                  ...data,
                  unitConverter: {
                    ...data.unitConverter,
                    toAbbr: evt.currentTarget.value,
                  },
                });
              }}
            />,
          );
        } else {
          return data;
        }
      },
    },
    {
      data: null, // algorithm
      render: (
        data: ApiConfigScriptSensor,
        type: string,
        _row: unknown,
        _meta: unknown,
      ) => {
        if (type === "display") {
          return toDom(
            <UnitConversionAlgorithmSelect
              current={data.unitConverter?.algorithm}
              disabled={!edit}
              onChange={(evt) => {
                inputRef.current = null;
                updateSensor({
                  ...data,
                  unitConverter: {
                    ...data.unitConverter,
                    algorithm: evt.currentTarget.value,
                  },
                });
              }}
            />,
          );
        } else {
          return data;
        }
      },
    },
    // a future improvement here could be for this to be one column
    // and use lazy loading to pull the correct method of rendering
    // the required columns as say none doesn't need any, linear needs 2, etc.
    { data: null, render: renderCoefficient },
    { data: null, render: renderCoefficient },
    { data: null, render: renderCoefficient },
    { data: null, render: renderCoefficient },
    { data: null, render: renderCoefficient },
    { data: null, render: renderCoefficient },
  ];

  useLayoutEffect(() => {
    const dt = table.current?.dt();
    console.log("updating data");
    if (dt) {
      dt.clear();
      dt.rows.add(scriptSensors);
      dt.draw();
    }
  }, [scriptSensors]);

  useEffect(() => {
    // for some reason we need this for the initial load
    // it is likely datatables.net is just not the correct choice
    // for these elements.
    const dt = table.current?.dt();
    if (dt) {
      dt.clear();
      dt.rows.add(scriptSensors);
      dt.draw();
    }
  }, []);

  const options: DataTableProps["options"] = useMemo(() => {
    return {
      search: false,
      searching: false,
      paging: false,
      info: false,
      ordering: {
        handler: false,
        indicators: false,
      },
      async drawCallback(_settings) {
        const api = this.api();
        console.log(api.table().node().id);
        console.log(`setting focus to ${inputRef.current?.name}`);
        if (inputRef.current) {
          // don't even ask, this all clearly needs to happen a different way

          await new Promise((resolve) => setTimeout(resolve, 30));
          const input = api
            .table()
            .node()
            .querySelector(
              `input[name="${inputRef.current.name}"]`,
            ) as HTMLInputElement;
          if (input) {
            input.focus();
          } else {
            console.log("element not found");
          }
        }
      },
    };
  }, []);

  return (
    <DataTable
      ref={table}
      className="table table-hover table-striped tablerow-cursor w-100 border table-sm"
      columns={columns}
      options={options}
    >
      <thead>
        <tr>
          <td>#</td>
          <td>Name</td>
          <td>Units</td>
          <td>Algorithm</td>
          <td>A</td>
          <td>B</td>
          <td>C</td>
          <td>D</td>
          <td>E</td>
          <td>F</td>
        </tr>
      </thead>
    </DataTable>
  );
};

export default SensorConversion;

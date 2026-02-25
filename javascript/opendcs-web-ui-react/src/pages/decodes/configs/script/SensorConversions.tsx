import type React from "react";
import DataTable, { type DataTableRef } from "datatables.net-react";
import DT from "datatables.net-bs5";
import { ApiConfigSensor, type ApiConfigScriptSensor } from "opendcs-api";
import { useCallback, useRef } from "react";
import { useUnits } from "../../../../contexts/data/UnitsContext";
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
  const units = useUnits();
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
        console.log(meta);
        console.log(`data is ${JSON.stringify(data)}`);
        const coeffIdx = colToCoeff(meta.col) as keyof typeof data.unitConverter;
        const coeff = data.unitConverter?.[coeffIdx];
        return toDom(
          <FormControl
            value={coeff}
            disabled={!edit}
            onChange={(evt) => {
              console.log(`changed Coeff ${evt.currentTarget.value}`);
              updateSensor({
                ...data,
                [coeffIdx]: evt.currentTarget.value,
              });
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
      data: null,
      render: (data: ApiConfigScriptSensor, type: string, _row: unknown) => {
        if (type === "display") {
          return configSensors[data.sensorNumber!].sensorName;
        } else {
          return data;
        }
      },
    },
    {
      data: null,
      render: (
        data: ApiConfigScriptSensor,
        type: string,
        _row: unknown,
        _meta: unknown,
      ) => {
        if (type === "display") {
          const unitAbbr: string | undefined = data.unitConverter?.ucId
            ? units.units[data.unitConverter.ucId]?.abbr
            : undefined;
          return toDom(
            <UnitSelect
              current={unitAbbr}
              disabled={!edit}
              onChange={(_evt) => console.log("changed units")}
            />,
          );
        } else {
          return data;
        }
      },
    },
    {
      data: null,
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
              onChange={(_evt) => console.log("changed units")}
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

  return (
    <DataTable
      ref={table}
      data={scriptSensors}
      className="table table-hover table-striped tablerow-cursor w-100 border table-sm"
      columns={columns}
      options={{
        search: false,
        searching: false,
        paging: false,
        info: false,
        ordering: {
          handler: false,
          indicators: false,
        },
      }}
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

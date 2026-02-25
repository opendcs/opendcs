import type {
  ApiConfigScript,
  ApiConfigScriptDataOrderEnum,
  ApiConfigScriptSensor,
  ApiScriptFormatStatement,
} from "opendcs-api";

export type DecodesScriptAction =
  // Header Section
  | { type: "set_data_order"; payload: { order: ApiConfigScriptDataOrderEnum } }
  | { type: "set_header"; payload: { header: string } }
  | { type: "set_name"; payload: { name: string } }
  // Statements
  | { type: "set_statements"; payload: { statements: ApiScriptFormatStatement[] } }
  // Sensors
  | { type: "set_sensor"; payload: { sensor: ApiConfigScriptSensor } };

export default function decodesScriptReducer(
  current: ApiConfigScript,
  action: DecodesScriptAction,
): ApiConfigScript {
  switch (action.type) {
    // Header data
    case "set_data_order": {
      return {
        ...current,
        dataOrder: action.payload.order,
      };
    }
    case "set_header": {
      return {
        ...current,
        headerType: action.payload.header,
      };
    }
    case "set_name": {
      return {
        ...current,
        name: action.payload.name,
      };
    }
    // Format Statements
    case "set_statements": {
      return {
        ...current,
        formatStatements: action.payload.statements,
      };
    }
    // Sensors
    case "set_sensor": {
      const sensors = current.scriptSensors!.filter(
        (ss) => ss.sensorNumber !== action.payload.sensor.sensorNumber,
      );

      return {
        ...current,
        scriptSensors: [...sensors, action.payload.sensor],
      };
    }
  }
}

import type { ApiConfigScript, ApiConfigSensor, ApiPlatformConfig } from "opendcs-api";

export type UiConfig = Partial<ApiPlatformConfig>;

export type ConfigAction =
  | { type: "save"; payload: UiConfig }
  | {
      type: "save_sensor";
      payload: { sensor: ApiConfigSensor; originalSensorNumber?: number };
    }
  | { type: "delete_sensor"; payload: { sensorNumber: number } }
  | {
      type: "save_script";
      payload: { script: ApiConfigScript; originalName?: string };
    }
  | { type: "delete_script"; payload: { name: string } };

export function ConfigReducer(current: UiConfig, action: ConfigAction): UiConfig {
  switch (action.type) {
    case "save":
      return { ...current, ...action.payload };
    case "save_sensor": {
      const { sensor, originalSensorNumber } = action.payload;
      const existing = current.configSensors ?? [];
      const matchNum = originalSensorNumber ?? sensor.sensorNumber;
      const idx = existing.findIndex((s) => s.sensorNumber === matchNum);
      const next = [...existing];
      if (idx >= 0) next[idx] = sensor;
      else next.push(sensor);
      return { ...current, configSensors: next };
    }
    case "delete_sensor": {
      const existing = current.configSensors ?? [];
      return {
        ...current,
        configSensors: existing.filter(
          (s) => s.sensorNumber !== action.payload.sensorNumber,
        ),
      };
    }
    case "save_script": {
      const { script, originalName } = action.payload;
      const existing = current.scripts ?? [];
      const matchName = originalName ?? script.name;
      const idx = existing.findIndex((s) => s.name === matchName);
      const next = [...existing];
      if (idx >= 0) next[idx] = script;
      else next.push(script);
      return { ...current, scripts: next };
    }
    case "delete_script": {
      const existing = current.scripts ?? [];
      return {
        ...current,
        scripts: existing.filter((s) => s.name !== action.payload.name),
      };
    }
  }
}

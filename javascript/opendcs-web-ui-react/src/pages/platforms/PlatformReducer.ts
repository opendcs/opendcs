import type { ApiPlatformSensor, ApiTransportMedium } from "opendcs-api";
import type { UiPlatform } from "./Platform";
import { transportKey } from "./transportKey";

export type PlatformAction =
  | { type: "save_prop"; payload: { name: string; value: string } }
  | { type: "delete_prop"; payload: { name: string } }
  | { type: "save"; payload: UiPlatform }
  | {
      type: "save_sensor";
      payload: { sensor: ApiPlatformSensor; originalSensorNum?: number };
    }
  | { type: "delete_sensor"; payload: { sensorNum: number } }
  | {
      type: "save_transport";
      payload: { medium: ApiTransportMedium; originalKey?: string };
    }
  | { type: "delete_transport"; payload: { key: string } };

export function PlatformReducer(
  currentPlatform: UiPlatform,
  action: PlatformAction,
): UiPlatform {
  switch (action.type) {
    case "save_prop": {
      return {
        ...currentPlatform,
        properties: {
          ...currentPlatform.properties,
          [action.payload.name]: action.payload.value,
        },
      };
    }
    case "delete_prop": {
      const props = { ...currentPlatform.properties };
      delete props[action.payload.name];
      return {
        ...currentPlatform,
        properties: {
          ...props,
        },
      };
    }
    case "save": {
      return {
        ...currentPlatform,
        ...action.payload,
      };
    }
    case "save_sensor": {
      const { sensor, originalSensorNum } = action.payload;
      const existing = currentPlatform.platformSensors ?? [];
      const matchNum = originalSensorNum ?? sensor.sensorNum;
      const idx = existing.findIndex((s) => s.sensorNum === matchNum);
      const next = [...existing];
      if (idx >= 0) {
        next[idx] = sensor;
      } else {
        next.push(sensor);
      }
      return { ...currentPlatform, platformSensors: next };
    }
    case "delete_sensor": {
      const existing = currentPlatform.platformSensors ?? [];
      return {
        ...currentPlatform,
        platformSensors: existing.filter(
          (s) => s.sensorNum !== action.payload.sensorNum,
        ),
      };
    }
    case "save_transport": {
      const { medium, originalKey } = action.payload;
      const existing = currentPlatform.transportMedia ?? [];
      const matchKey = originalKey ?? transportKey(medium);
      const idx = existing.findIndex((m) => transportKey(m) === matchKey);
      const next = [...existing];
      if (idx >= 0) {
        next[idx] = medium;
      } else {
        next.push(medium);
      }
      return { ...currentPlatform, transportMedia: next };
    }
    case "delete_transport": {
      const existing = currentPlatform.transportMedia ?? [];
      return {
        ...currentPlatform,
        transportMedia: existing.filter((m) => transportKey(m) !== action.payload.key),
      };
    }
  }
}

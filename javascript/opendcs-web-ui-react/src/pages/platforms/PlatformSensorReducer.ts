import type { UiPlatformSensor } from "./PlatformSensor";

export type PlatformSensorAction =
  | { type: "save_prop"; payload: { name: string; value: string } }
  | { type: "delete_prop"; payload: { name: string } }
  | { type: "save"; payload: UiPlatformSensor };

export function PlatformSensorReducer(
  current: UiPlatformSensor,
  action: PlatformSensorAction,
): UiPlatformSensor {
  switch (action.type) {
    case "save_prop": {
      return {
        ...current,
        sensorProps: {
          ...current.sensorProps,
          [action.payload.name]: action.payload.value,
        },
      };
    }
    case "delete_prop": {
      const props = { ...current.sensorProps };
      delete props[action.payload.name];
      return { ...current, sensorProps: props };
    }
    case "save": {
      return { ...current, ...action.payload };
    }
  }
}

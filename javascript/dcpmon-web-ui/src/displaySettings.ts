export type DisplaySettings = {
  theme: "system" | "light" | "dark";
  timeZone: string;
  hourFormat: "12" | "24";
  showSeconds: boolean;
};

export const DEFAULT_DISPLAY_SETTINGS: DisplaySettings = {
  theme: "system",
  timeZone: "GMT",
  hourFormat: "12",
  showSeconds: true,
};

const STORAGE_KEY = "opendcs.dcpmon.display-settings";

export function isValidTimeZone(timeZone: string) {
  try {
    new Intl.DateTimeFormat(undefined, { timeZone }).format();
    return true;
  } catch {
    return false;
  }
}

export function loadDisplaySettings(): DisplaySettings {
  try {
    const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? "null") as
      | Partial<DisplaySettings>
      | null;
    return {
      theme:
        stored?.theme === "light" || stored?.theme === "dark"
          ? stored.theme
          : "system",
      timeZone:
        stored?.timeZone && isValidTimeZone(stored.timeZone)
          ? stored.timeZone
          : DEFAULT_DISPLAY_SETTINGS.timeZone,
      hourFormat: stored?.hourFormat === "24" ? "24" : "12",
      showSeconds:
        typeof stored?.showSeconds === "boolean"
          ? stored.showSeconds
          : DEFAULT_DISPLAY_SETTINGS.showSeconds,
    };
  } catch {
    return DEFAULT_DISPLAY_SETTINGS;
  }
}

export function storeDisplaySettings(settings: DisplaySettings) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
  } catch {
    // The in-memory setting still applies when storage is unavailable.
  }
}

export function supportedTimeZones() {
  const supportedValuesOf = (
    Intl as typeof Intl & {
      supportedValuesOf?: (key: "timeZone") => string[];
    }
  ).supportedValuesOf;
  const zones = supportedValuesOf?.("timeZone") ?? [];
  return ["GMT", ...zones.filter((zone) => zone !== "GMT" && zone !== "UTC")];
}

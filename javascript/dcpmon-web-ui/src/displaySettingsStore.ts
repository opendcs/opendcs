import { createContext, useContext } from "react";
import {
  DEFAULT_DISPLAY_SETTINGS,
  type DisplaySettings,
} from "./displaySettings";

export type EffectiveTheme = "light" | "dark";

export type DisplaySettingsContextValue = {
  settings: DisplaySettings;
  effectiveTheme: EffectiveTheme;
  saveSettings: (settings: DisplaySettings) => void;
  toggleTheme: () => void;
};

export const DisplaySettingsContext = createContext<DisplaySettingsContextValue>({
  settings: DEFAULT_DISPLAY_SETTINGS,
  effectiveTheme: "light",
  saveSettings: () => undefined,
  toggleTheme: () => undefined,
});

export function useDisplaySettings() {
  return useContext(DisplaySettingsContext);
}

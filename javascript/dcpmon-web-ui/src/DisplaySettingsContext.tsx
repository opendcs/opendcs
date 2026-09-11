import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";
import {
  loadDisplaySettings,
  storeDisplaySettings,
  type DisplaySettings,
} from "./displaySettings";
import {
  DisplaySettingsContext,
  type EffectiveTheme,
} from "./displaySettingsStore";

function systemTheme(): EffectiveTheme {
  return window.matchMedia?.("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light";
}

export function DisplaySettingsProvider({ children }: PropsWithChildren) {
  const [settings, setSettings] = useState(loadDisplaySettings);
  const [preferredTheme, setPreferredTheme] = useState(systemTheme);
  const effectiveTheme =
    settings.theme === "system" ? preferredTheme : settings.theme;

  useEffect(() => {
    const media = window.matchMedia?.("(prefers-color-scheme: dark)");
    if (!media) return;
    const updateTheme = () => setPreferredTheme(media.matches ? "dark" : "light");
    media.addEventListener("change", updateTheme);
    return () => media.removeEventListener("change", updateTheme);
  }, []);

  useEffect(() => {
    document.documentElement.dataset.bsTheme = effectiveTheme;
    document.documentElement.style.colorScheme = effectiveTheme;
  }, [effectiveTheme]);

  const saveSettings = useCallback((nextSettings: DisplaySettings) => {
    setSettings(nextSettings);
    storeDisplaySettings(nextSettings);
  }, []);

  const toggleTheme = useCallback(() => {
    setSettings((current) => {
      const currentEffective =
        current.theme === "system" ? systemTheme() : current.theme;
      const next = {
        ...current,
        theme: currentEffective === "dark" ? "light" : "dark",
      } satisfies DisplaySettings;
      storeDisplaySettings(next);
      return next;
    });
  }, []);

  const value = useMemo(
    () => ({ settings, effectiveTheme, saveSettings, toggleTheme }),
    [effectiveTheme, saveSettings, settings, toggleTheme],
  );

  return (
    <DisplaySettingsContext.Provider value={value}>
      {children}
    </DisplaySettingsContext.Provider>
  );
}

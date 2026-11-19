import { useCallback, useEffect, useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import availableLanguages from "../../lang";
import { useAuth } from "./AuthContext";
import { useTheme, type ColorMode } from "./ThemeContext";
import {
  SITE_NAME_TYPES,
  useSiteNameType,
  type SiteNameTypePreference,
} from "./SiteNameTypeContext";
import { useSavePreferences } from "../../hooks/useUserPreferences";
import {
  PreferenceKeys,
  planChanges,
  planSessionStart,
  type SettingValues,
  type SyncedSetting,
} from "../../util/preferences";

const COLOR_MODES: readonly unknown[] = ["light", "dark", "auto"] satisfies ColorMode[];
const SITE_NAME_TYPE_PREFERENCES: readonly unknown[] = [
  "",
  ...SITE_NAME_TYPES,
] satisfies SiteNameTypePreference[];

const SETTINGS: SyncedSetting[] = [
  {
    key: PreferenceKeys.colorMode,
    isValid: (value) => COLOR_MODES.includes(value),
    seeds: (value) => value !== "auto",
  },
  {
    key: PreferenceKeys.siteNameType,
    isValid: (value) => SITE_NAME_TYPE_PREFERENCES.includes(value),
    seeds: (value) => value !== "",
  },
  {
    key: PreferenceKeys.language,
    isValid: (value) => availableLanguages.includes(value as string),
    // The language starts out detected from the browser, so a deliberate
    // choice can't be told from the default. It's saved once picked.
    seeds: () => false,
  },
];

const NO_PREFERENCES = {};

/**
 * Keeps the color mode, preferred site name type and language in step with
 * the signed-in user's profile, so they follow the user to other browsers and
 * outlive browser storage being cleared. Renders nothing.
 *
 * When a session starts the profile's values are applied, except for settings
 * changed since the last sign out (e.g. on the login page), which are saved
 * instead. From then on every change is saved. See {@link planSessionStart}.
 */
export const PreferencesSync = () => {
  const { user } = useAuth();
  const { theme, setTheme } = useTheme();
  const { siteNameType, setSiteNameType } = useSiteNameType();
  const { i18n } = useTranslation();
  const savePreferences = useSavePreferences();

  const colorMode = theme.colorMode;
  const preferredType = siteNameType.preferredType;
  const language = i18n.language;
  const local = useMemo<SettingValues>(
    () => ({
      [PreferenceKeys.colorMode]: colorMode,
      [PreferenceKeys.siteNameType]: preferredType,
      [PreferenceKeys.language]: language,
    }),
    [colorMode, preferredType, language],
  );

  const applyLocally = useCallback(
    (values: SettingValues) => {
      const {
        [PreferenceKeys.colorMode]: newColorMode,
        [PreferenceKeys.siteNameType]: newPreferredType,
        [PreferenceKeys.language]: newLanguage,
      } = values;
      if (newColorMode !== undefined) {
        setTheme({ colorMode: newColorMode as ColorMode });
      }
      if (newPreferredType !== undefined) {
        setSiteNameType({ preferredType: newPreferredType as SiteNameTypePreference });
      }
      if (newLanguage !== undefined) {
        i18n.changeLanguage(newLanguage as string);
      }
    },
    [setTheme, setSiteNameType, i18n],
  );

  const userKey = user?.email;
  const profile: SettingValues = user?.preferences ?? NO_PREFERENCES;
  // Settings as they were at page load or the last sign out. Anything that
  // differs at sign in was chosen while signed out.
  const baseline = useRef(local);
  const previous = useRef(local);
  const sessionUser = useRef<string | undefined>(undefined);

  useEffect(() => {
    let save: SettingValues = {};
    if (!userKey) {
      if (sessionUser.current !== undefined) {
        baseline.current = local;
      }
      sessionUser.current = undefined;
    } else if (sessionUser.current === userKey) {
      save = planChanges(SETTINGS, local, previous.current, profile);
    } else {
      sessionUser.current = userKey;
      const plan = planSessionStart(SETTINGS, local, baseline.current, profile);
      applyLocally(plan.apply);
      save = plan.save;
    }
    previous.current = local;

    if (Object.keys(save).length > 0) {
      savePreferences(save).catch((error: unknown) =>
        console.warn("Unable to save preferences to the user's profile", error),
      );
    }
  }, [userKey, profile, local, applyLocally, savePreferences]);

  return null;
};

export default PreferencesSync;

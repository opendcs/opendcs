import type { User } from "opendcs-api";

/**
 * Names under which the web UI keeps its settings in the user's profile
 * (`User.preferences` on the server). The server and other clients may store
 * further keys there, e.g. `preferredUserName` from OIDC registration, which is
 * why the UI only ever sends the keys it changes.
 */
export const PreferenceKeys = {
  organization: "organization",
  colorMode: "colorMode",
  siteNameType: "siteNameType",
  language: "language",
} as const;

/** A partial update to the profile: each value replaces the stored one and null removes it. */
export type PreferenceChanges = { [key: string]: unknown };

export type SettingValues = Record<string, unknown>;

/** A browser-local setting that is kept in step with the user's profile. */
export interface SyncedSetting {
  key: string;
  /** Whether a value, local or from the profile, is one this setting can use. */
  isValid: (value: unknown) => boolean;
  /**
   * Whether a local value is worth copying into a profile that has no value
   * for the setting yet. False for defaults, so the profile only collects
   * choices the user actually made.
   */
  seeds: (value: unknown) => boolean;
}

export interface SessionStartPlan {
  /** Profile values to apply locally. */
  apply: SettingValues;
  /** Local values to save to the profile. */
  save: SettingValues;
}

/**
 * Reconcile local settings with the profile when a user's session starts.
 * For each setting, the first rule that matches wins:
 *
 * 1. Changed since `baseline` (the values at the last sign out, or at page
 *    load), e.g. on the login page: the user just chose it, so save it.
 * 2. Stored in the profile: apply the profile's value locally.
 * 3. Otherwise save the local value if it `seeds`, so settings made before
 *    the profile kept them follow the user from now on.
 */
export function planSessionStart(
  settings: SyncedSetting[],
  local: SettingValues,
  baseline: SettingValues,
  profile: SettingValues,
): SessionStartPlan {
  const plan: SessionStartPlan = { apply: {}, save: {} };
  for (const { key, isValid, seeds } of settings) {
    const value = local[key];
    const stored = profile[key];
    if (value !== baseline[key] && isValid(value)) {
      if (value !== stored) plan.save[key] = value;
    } else if (isValid(stored)) {
      if (stored !== value) plan.apply[key] = stored;
    } else if (isValid(value) && seeds(value)) {
      plan.save[key] = value;
    }
  }
  return plan;
}

/**
 * Local values to save during a session: those that changed since `previous`
 * and that the profile doesn't already hold. Values that just arrived from the
 * profile match it, so applying the profile never echoes back as a save.
 */
export function planChanges(
  settings: SyncedSetting[],
  local: SettingValues,
  previous: SettingValues,
  profile: SettingValues,
): SettingValues {
  const save: SettingValues = {};
  for (const { key, isValid } of settings) {
    const value = local[key];
    if (value !== previous[key] && value !== profile[key] && isValid(value)) {
      save[key] = value;
    }
  }
  return save;
}

/** Name of the organization saved in the user's profile, if there is one. */
export const savedOrganization = (user?: User): string | undefined => {
  const name: unknown = user?.preferences?.[PreferenceKeys.organization];
  return typeof name === "string" && name !== "" ? name : undefined;
};

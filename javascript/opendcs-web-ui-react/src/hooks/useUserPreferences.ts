import { useCallback } from "react";
import {
  RESTAuthenticationAndAuthorizationApi,
  UserOperationsApi,
  type User,
} from "opendcs-api";
import { useApi } from "../contexts/app/ApiContext";
import { useAuth } from "../contexts/app/AuthContext";
import { useOrganizations } from "../contexts/app/OrganizationsContext";
import {
  PreferenceKeys,
  savedOrganization,
  type PreferenceChanges,
} from "../util/preferences";

/**
 * sessionStorage key that carries the login page's organization choice across
 * the redirect to an OIDC provider and back.
 */
export const LOGIN_ORGANIZATION_KEY = "loginOrganization";

// The server merges each save into what it last stored, so two saves in flight
// at once (e.g. the organization and a seeded color mode right after login)
// could each drop the other's keys. They're sent one at a time instead.
let lastSave: Promise<unknown> = Promise.resolve();

/**
 * Returns a function that saves changes to the signed-in user's profile
 * preferences. It resolves with the complete stored preferences, which also
 * replace those on the current user.
 */
export const useSavePreferences = () => {
  const { conf } = useApi();
  const { setUser } = useAuth();
  return useCallback(
    (changes: PreferenceChanges) => {
      const save = lastSave
        .catch(() => undefined)
        .then(async () => {
          // Preferences belong to the user rather than an organization.
          const preferences = await new UserOperationsApi(conf).updatePreferences(
            "",
            changes,
          );
          setUser((current) => (current ? { ...current, preferences } : current));
          return preferences;
        });
      lastSave = save;
      return save;
    },
    [conf, setUser],
  );
};

/**
 * Returns a function that selects the organization for a session that just
 * signed in. `choice` is the organization picked on the login page. Without one
 * ("Previous session") the organization saved in the user's profile is used,
 * falling back to this browser's last organization for profiles that don't
 * have one yet. Whatever was used is saved to the profile for next time. If the
 * user holds no role in it, no organization is selected.
 */
export const useApplyLoginOrganization = () => {
  const { conf, org: browserOrg, setOrg } = useApi();
  const { organizations } = useOrganizations();
  const savePreferences = useSavePreferences();
  return useCallback(
    async (user: User, choice?: string | null) => {
      const saved = savedOrganization(user);
      const name = choice || saved || browserOrg;
      if (!name) {
        setOrg({});
        return;
      }
      try {
        // Rejected when the user holds no role in the organization.
        await new RESTAuthenticationAndAuthorizationApi(conf).getOrganizations(name);
      } catch {
        setOrg({});
        return;
      }
      setOrg(organizations.find((org) => org.name === name) ?? { name });
      if (name !== saved) {
        await savePreferences({ [PreferenceKeys.organization]: name }).catch(
          (error: unknown) =>
            console.warn(
              "Unable to save the organization to the user's profile",
              error,
            ),
        );
      }
    },
    [conf, browserOrg, setOrg, organizations, savePreferences],
  );
};

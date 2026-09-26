import { Decorator } from "@storybook/react-vite";
import {
  ApiContext,
  defaultValue as apiDefault,
} from "../../src/contexts/app/ApiContext";
import { AuthContext } from "../../src/contexts/app/AuthContext";
import { fn } from "storybook/test";
import { OrganizationsContext } from "../../src/contexts/app/OrganizationsContext";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { ApiOrganization } from "opendcs-api";

export const MOCK_ORGANIZATIONS: ApiOrganization[] = [
  { name: "SPK" },
  { name: "HQ" },
  { name: "LRL" },
  { name: "SWT" },
  { name: "MVP" },
];

// The same offices wired up the way CWMS reports them: HQ at the root,
// divisions beneath it, districts beneath those. Kept separate from
// MOCK_ORGANIZATIONS so stories that assert the plain alphabetical list stay
// unaffected. Deliberately out of order, like the API's own response.
export const MOCK_ORG_HIERARCHY: ApiOrganization[] = [
  { name: "SPK", parent: "SPD" },
  { name: "HQ" },
  { name: "SWD", parent: "HQ" },
  { name: "SPD", parent: "HQ" },
  { name: "SWT", parent: "SWD" },
  { name: "MVP", parent: "MVD" },
  { name: "MVD", parent: "HQ" },
];

export const WithOrganization: Decorator = (Story, { args }) => {
  // A simple stand-in for the page the user is redirected to after login
  function PlatformsPage() {
    return <div data-testid="platforms-page">Platforms Page</div>;
  }

  var orgs = args.organizations || MOCK_ORGANIZATIONS;

  return (
    <ApiContext value={apiDefault}>
      <AuthContext
        value={{
          user: undefined,
          isLoading: false,
          setUser: fn(),
          logout: fn(),
        }}
      >
        <OrganizationsContext value={{ organizations: orgs }}>
          <Story />
          {/* <Routes>

            <Route path="/login" element={<Story />} />
            <Route path="/platforms" element={<PlatformsPage />} />
            <Route path="*" element={<Story />} />
          </Routes> */}
        </OrganizationsContext>
      </AuthContext>
    </ApiContext>
  );
};

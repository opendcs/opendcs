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

export const WithOrganization: Decorator = (Story, { args }) => {
  const MOCK_ORGANIZATIONS = [
    { name: "SPK" } as ApiOrganization,
    { name: "LRL" } as ApiOrganization,
    { name: "SWT" } as ApiOrganization,
    { name: "MVP" } as ApiOrganization,
  ];

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
          <MemoryRouter initialEntries={["/login"]}>
            <Routes>
              <Route path="/login" element={<Story />} />
              <Route path="/platforms" element={<PlatformsPage />} />
            </Routes>
          </MemoryRouter>
        </OrganizationsContext>
      </AuthContext>
    </ApiContext>
  );
};

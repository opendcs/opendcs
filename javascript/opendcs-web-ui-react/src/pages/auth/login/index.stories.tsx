import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, userEvent, within, waitFor, fn } from "storybook/test";
import Login from "./index";
import { http, HttpResponse } from "msw";
import {
  MOCK_ORG_HIERARCHY,
  WithOrganization,
} from "../../../../.storybook/mock/WithOrganization";
import { ApiOrganization } from "opendcs-api";
import { AuthContext } from "../../../contexts/app/AuthContext";
import { ApiProvider } from "../../../contexts/app/ApiProvider";
import { useApi } from "../../../contexts/app/ApiContext";
import apiAuthSpec from "../../../../.storybook/mock/openapi-security-schemes.json";
import { fromOpenApiData } from "../../../util/login-providers";
import { Route, Routes } from "react-router-dom";

const ORG_HEADER = "x-organization-id";

const authSchemes = await fromOpenApiData(apiAuthSpec);

// Offices the mock user holds a role in; /organizations answers 403 for others.
const USER_ORGS = ["SPK", "MVP", "SWT"];

// Records each PATCH of the user's profile preferences.
const savedPreferences = fn();

// A simple stand-in for the page the user is redirected to after login,
// showing which organization the session ended up in.
function PlatformsPage() {
  const { org } = useApi();
  return (
    <div data-testid="platforms-page">
      Platforms Page <span data-testid="session-org">{org}</span>
    </div>
  );
}

const meta: Meta<typeof Login & { organizations: ApiOrganization[] }> = {
  component: Login,
  decorators: [WithOrganization],
  parameters: {
    layout: "fullscreen",
    msw: {
      handlers: {
        // Default: successful login handler
        credentials: http.post("/odcsapi/credentials", async ({ request }) => {
          const body = (await request.json()) as { username: string; password: string };
          const { username, password } = body;
          var orgHeader = request.headers.get(ORG_HEADER);
          if (
            (orgHeader !== "" && orgHeader !== "SPK") ||
            username !== "admin" ||
            password !== "secret"
          ) {
            return new HttpResponse(
              "Invalid credentials or insufficient permissions for office",
              { status: 403 },
            );
          }

          return HttpResponse.json({
            username: "admin",
            roles: ["ODCS_USER"],
          });
        }),
        // Used after login to check the user holds a role in the chosen office.
        organizations: http.get("/odcsapi/organizations", ({ request }) =>
          USER_ORGS.includes(request.headers.get(ORG_HEADER) ?? "")
            ? HttpResponse.json([])
            : new HttpResponse("User does not have the correct roles", { status: 403 }),
        ),
        preferences: http.patch("/odcsapi/user/preferences", async ({ request }) => {
          const changes = await request.json();
          savedPreferences(changes);
          return HttpResponse.json(changes);
        }),
      },
    },
  },
  beforeEach: () => {
    savedPreferences.mockClear();
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

const authDecorator = (Story: any) => (
  <ApiProvider>
    <AuthContext
      value={{
        user: undefined,
        isLoading: false,
        loginSchemes: authSchemes,
        setSchemes: fn(),
        setUser: fn(),
        logout: fn(),
      }}
    >
      <Routes>
        <Route path="/login" element={<Story />} />
        <Route path="/platforms" element={<PlatformsPage />} />
        <Route path="*" element={<Story />} />
      </Routes>
    </AuthContext>
  </ApiProvider>
);
// DefaultPage intentionally bypasses the decorator.
// Since we are changing the local storage
// we need to bypass some of the
// behavior so that what we set is actually used.
export const DefaultPageWithOrgs: Story = {
  parameters: {
    bypassGlobalDecorators: true,
  },
  args: {
    organization: undefined,
  },
  render: () => {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    const api = useApi();
    api.setOrg({ name: "HQ" });
    window.localStorage.clear();
    window.localStorage.setItem("organization", JSON.stringify({ name: "HQ" }));
    return authDecorator(Login);
  },
  play: async ({ mount }) => {
    const canvas = await mount();
    await expect(await canvas.findByText("kc_idp_hint")).toBeInTheDocument();
  },
};

// Browser storage can be cleared on exit and is shared by everyone using the
// browser, so the login page no longer preselects the last office stored there.
// It defaults to "Previous session": the office saved in the user's profile,
// resolved once they've signed in.
export const DefaultsToPreviousSession: Story = {
  decorators: [authDecorator],
  beforeEach: () => {
    window.localStorage.setItem("organization", JSON.stringify({ name: "MVP" }));
  },
  play: async ({ mount }) => {
    const canvas = await mount();
    await expect(await canvas.findByText("kc_idp_hint")).toBeInTheDocument();
    const orgs = canvas.getByRole("combobox", {
      name: "Organization",
    }) as HTMLSelectElement;
    await expect(orgs.value).toBe("");
    await expect(orgs.selectedOptions[0]).toHaveTextContent("Previous session");
  },
};

export const SuccessfulLogin: Story = {
  args: {
    organization: { name: "SPK" },
  },
  decorators: [authDecorator],
  play: async ({ args, mount }) => {
    const canvas = await mount();

    await userEvent.type(await canvas.getByPlaceholderText(/username/i), "admin");
    await userEvent.type(await canvas.getByPlaceholderText(/password/i), "secret");

    // Set the organization
    await userEvent.selectOptions(
      canvas.getByRole("combobox", {
        name: "Organization",
      }),
      args.organization.name,
    );

    // Submit the form
    await userEvent.click(canvas.getByRole("button", { name: /^login$/i }));

    // Verify the user was redirected to the platforms page
    await waitFor(async () => {
      await expect(canvas.getByTestId("platforms-page")).toBeInTheDocument();
    });
    await expect(canvas.getByTestId("session-org")).toHaveTextContent("SPK");
    // An office picked at login is saved to the profile for next time.
    await expect(savedPreferences).toHaveBeenCalledWith({ organization: "SPK" });
  },
};

export const PreviousSessionUsesTheProfileOrganization: Story = {
  decorators: [authDecorator],
  parameters: {
    msw: {
      handlers: {
        credentials: http.post("/odcsapi/credentials", () =>
          HttpResponse.json({ email: "admin", preferences: { organization: "MVP" } }),
        ),
      },
    },
  },
  beforeEach: () => {
    // What this browser last used is ignored in favor of the profile.
    window.localStorage.setItem("organization", JSON.stringify({ name: "SWT" }));
  },
  play: async ({ mount }) => {
    const canvas = await mount();

    await userEvent.type(canvas.getByPlaceholderText(/username/i), "admin");
    await userEvent.type(canvas.getByPlaceholderText(/password/i), "secret");
    await userEvent.click(canvas.getByRole("button", { name: /^login$/i }));

    await waitFor(async () => {
      await expect(canvas.getByTestId("platforms-page")).toBeInTheDocument();
    });
    await expect(canvas.getByTestId("session-org")).toHaveTextContent("MVP");
    // Already in the profile, so there is nothing to save.
    await expect(savedPreferences).not.toHaveBeenCalled();
  },
};

// Profiles saved before organizations were kept there have none, so the
// browser's last organization is used and copied into the profile.
export const PreviousSessionFallsBackToTheBrowserOrganization: Story = {
  decorators: [authDecorator],
  beforeEach: () => {
    window.localStorage.setItem("organization", JSON.stringify({ name: "SWT" }));
  },
  play: async ({ mount }) => {
    const canvas = await mount();

    await userEvent.type(canvas.getByPlaceholderText(/username/i), "admin");
    await userEvent.type(canvas.getByPlaceholderText(/password/i), "secret");
    await userEvent.click(canvas.getByRole("button", { name: /^login$/i }));

    await waitFor(async () => {
      await expect(canvas.getByTestId("platforms-page")).toBeInTheDocument();
    });
    await expect(canvas.getByTestId("session-org")).toHaveTextContent("SWT");
    await expect(savedPreferences).toHaveBeenCalledWith({ organization: "SWT" });
  },
};

export const OrganizationWithoutARoleIsNotSelected: Story = {
  decorators: [authDecorator],
  play: async ({ mount }) => {
    const canvas = await mount();

    await userEvent.type(canvas.getByPlaceholderText(/username/i), "admin");
    await userEvent.type(canvas.getByPlaceholderText(/password/i), "secret");
    await userEvent.selectOptions(
      canvas.getByRole("combobox", { name: "Organization" }),
      "LRL",
    );
    await userEvent.click(canvas.getByRole("button", { name: /^login$/i }));

    await waitFor(async () => {
      await expect(canvas.getByTestId("platforms-page")).toBeInTheDocument();
    });
    await expect(canvas.getByTestId("session-org")).toBeEmptyDOMElement();
    await expect(savedPreferences).not.toHaveBeenCalled();
  },
};

export const FailedLogin_BadCredentials: Story = {
  args: {
    organization: { name: "SPK" },
  },
  decorators: [authDecorator],
  parameters: {
    msw: {
      handlers: {
        credentials: http.post("/odcsapi/credentials", () => {
          return new HttpResponse("User not authorized for office", { status: 403 });
        }),
      },
    },
  },
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement);

    await userEvent.type(canvas.getByPlaceholderText(/username/i), "wronguser");
    await userEvent.type(canvas.getByPlaceholderText(/password/i), "wrongpass");

    await userEvent.selectOptions(
      canvas.getByRole("combobox", { name: "Organization" }),
      args.organization.name,
    );

    await userEvent.click(canvas.getByRole("button", { name: /^login$/i }));

    // The login page should still be visible (no redirect)
    await waitFor(async () => {
      await expect(
        canvas.getByRole("button", { name: /^login$/i }),
      ).toBeInTheDocument();
    });
  },
};

export const FailedLogin_BadOrg: Story = {
  args: {
    organization: { name: "LRL" },
  },
  decorators: [authDecorator],
  parameters: {
    msw: {
      handlers: {
        credentials: http.post("/odcsapi/credentials", () => {
          return new HttpResponse("User not authorized for office", { status: 403 });
        }),
      },
    },
  },
  play: async ({ args, mount }) => {
    const canvas = await mount();

    await userEvent.type(canvas.getByPlaceholderText(/username/i), "admin");
    await userEvent.type(canvas.getByPlaceholderText(/password/i), "secret");

    await userEvent.selectOptions(
      canvas.getByRole("combobox", { name: "Organization" }),
      args.organization.name,
    );

    await userEvent.click(canvas.getByRole("button", { name: /^login$/i }));

    // The login page should still be visible (no redirect)
    await waitFor(async () => {
      await expect(
        canvas.getByRole("button", { name: /^login$/i }),
      ).toBeInTheDocument();
    });
  },
};

export const SuccessfulLogin_HiddenOrganizations: Story = {
  args: {
    organizations: [],
  },
  decorators: [authDecorator],
  play: async ({ mount }) => {
    const canvas = await mount();

    await userEvent.type(canvas.getByPlaceholderText(/username/i), "admin");
    await userEvent.type(canvas.getByPlaceholderText(/password/i), "secret");

    await waitFor(async () => {
      await expect(canvas.queryByRole("combobox", { name: "Organization" })).toBeNull();
    });

    await userEvent.click(canvas.getByRole("button", { name: /^login$/i }));

    // Successful login
    await waitFor(async () => {
      await expect(canvas.getByTestId("platforms-page")).toBeInTheDocument();
    });
  },
};

// The mock org list is deliberately unsorted (SPK, HQ, LRL, SWT, MVP) and is
// supplied straight to OrganizationsContext, so this covers the sort applied
// on read by useOrganizations rather than anything the query does.
export const OrganizationsAreSortedAlphabetically: Story = {
  args: {
    organization: undefined,
  },
  decorators: [authDecorator],
  play: async ({ mount }) => {
    const canvas = await mount();

    const select = await canvas.findByRole("combobox", { name: "Organization" });
    const options = within(select)
      .getAllByRole("option")
      .map((o) => o.textContent);

    // "Previous session" always leads; the offices follow in order.
    await expect(options).toEqual([
      "Previous session",
      "HQ",
      "LRL",
      "MVP",
      "SPK",
      "SWT",
    ]);
  },
};

// MOCK_ORG_HIERARCHY wires the same offices up the way CWMS reports them, so
// this covers the nesting rather than the plain alphabetical run above.
export const OrganizationsAreNestedUnderTheirParents: Story = {
  args: {
    organization: undefined,
    organizations: MOCK_ORG_HIERARCHY,
  },
  decorators: [authDecorator],
  play: async ({ mount }) => {
    const canvas = await mount();

    const select = await canvas.findByRole("combobox", { name: "Organization" });
    const options = within(select)
      .getAllByRole("option")
      .map((o) => o.textContent ?? "")
      // Skip "Previous session", which is never nested.
      .slice(1);

    // Non-breaking space, built from its code point so the indentation
    // characters stay visible in source.
    const NBSP = String.fromCodePoint(160);

    await expect(options.map((o) => o.replaceAll(NBSP, ""))).toEqual([
      "HQ",
      "MVD",
      "MVP",
      "SPD",
      "SPK",
      "SWD",
      "SWT",
    ]);
    // Indentation tracks depth: divisions one level in, districts two.
    await expect(options.map((o) => (o.split(NBSP).length - 1) / 4)).toEqual([
      0, 1, 2, 1, 2, 1, 2,
    ]);
  },
};

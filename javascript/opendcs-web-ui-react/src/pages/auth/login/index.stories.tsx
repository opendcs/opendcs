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
import {
  ApiContext,
  defaultValue as apiDefault,
  useApi,
} from "../../../contexts/app/ApiContext";
import apiAuthSpec from "../../../../.storybook/mock/openapi-security-schemes.json";
import { fromOpenApiData } from "../../../util/login-providers";
import { Route, Routes } from "react-router-dom";

const ORG_HEADER = "x-organization-id";

const authSchemes = await fromOpenApiData(apiAuthSpec);

// A simple stand-in for the page the user is redirected to after login
function PlatformsPage() {
  return <div data-testid="platforms-page">Platforms Page</div>;
}

const meta: Meta<typeof Login & { organizations: ApiOrganization[] }> = {
  component: Login,
  decorators: [WithOrganization],
  parameters: {
    layout: "fullscreen",
    msw: {
      handlers: [
        // Default: successful login handler
        http.post("/odcsapi/credentials", async ({ request }) => {
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
      ],
    },
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
// DefaultPage and Login within Previously stored org
// intentionally bypass the the decorator.
// A since we are changing the local storage
// for one of the tests we need to bypass some of the
// behavior so that what we set is actually used.
export const DefaultPageWithOrgs: Story = {
  parameters: {
    bypassGlobalDecorators: true,
  },
  args: {
    organization: undefined,
  },
  render: (args) => {
    const api = useApi();
    api.setOrg({ name: "HQ" });
    window.localStorage.clear();
    window.localStorage.setItem("organization", JSON.stringify({ name: "HQ" }));
    return authDecorator(Login);
  },
  play: async ({ args, mount }) => {
    const canvas = await mount();
    await expect(await canvas.findByText("kc_idp_hint")).toBeInTheDocument();
  },
};

export const LoginWithPreviouslyStoredOrg: Story = {
  parameters: {
    bypassGlobalDecorators: true,
  },
  render: () => {
    const api = useApi();
    api.setOrg({ name: "MVP" });
    window.localStorage.clear();
    window.localStorage.setItem("organization", JSON.stringify({ name: "MVP" }));

    return authDecorator(Login);
  },
  play: async ({ args, mount }) => {
    const canvas = await mount();
    await expect(await canvas.findByText("kc_idp_hint")).toBeInTheDocument();
    const orgs = canvas.getByRole("combobox", {
      name: "Organization",
    }) as HTMLSelectElement;
    console.log(orgs);
    await expect(JSON.parse(orgs.value)).toStrictEqual({ name: "MVP" });
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
      JSON.stringify(args.organization),
    );

    // Submit the form
    await userEvent.click(canvas.getByRole("button", { name: /^login$/i }));

    // Verify the user was redirected to the platforms page
    await waitFor(async () => {
      expect(canvas.getByTestId("platforms-page")).toBeInTheDocument();
    });
  },
};

export const FailedLogin_BadCredentials: Story = {
  args: {
    organization: { name: "SPK" },
  },
  decorators: [authDecorator],
  parameters: {
    msw: {
      handlers: [
        http.post("/odcsapi/credentials", () => {
          return new HttpResponse("User not authorized for office", { status: 403 });
        }),
      ],
    },
  },
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement);

    await userEvent.type(canvas.getByPlaceholderText(/username/i), "wronguser");
    await userEvent.type(canvas.getByPlaceholderText(/password/i), "wrongpass");

    await userEvent.selectOptions(
      canvas.getByRole("combobox", { name: "Organization" }),
      JSON.stringify(args.organization),
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
      handlers: [
        http.post("/odcsapi/credentials", () => {
          return new HttpResponse("User not authorized for office", { status: 403 });
        }),
      ],
    },
  },
  play: async ({ args, mount }) => {
    const canvas = await mount();

    await userEvent.type(canvas.getByPlaceholderText(/username/i), "admin");
    await userEvent.type(canvas.getByPlaceholderText(/password/i), "secret");

    await userEvent.selectOptions(
      canvas.getByRole("combobox", { name: "Organization" }),
      JSON.stringify(args.organization),
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

    await expect(options).toEqual(["HQ", "LRL", "MVP", "SPK", "SWT"]);
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
      .map((o) => o.textContent ?? "");

    // Non-breaking space, built from its code point so the indentation
    // characters stay visible in source.
    const NBSP = String.fromCharCode(160);

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

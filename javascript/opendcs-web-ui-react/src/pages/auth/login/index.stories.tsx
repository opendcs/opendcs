import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, userEvent, within, waitFor, fn } from "storybook/test";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import Login from "./index";
import { http, HttpResponse } from "msw";
import { OrganizationsContext } from "../../../contexts/app/OrganizationsContext";
import { AuthContext } from "../../../contexts/app/AuthContext";
import {
  ApiContext,
  defaultValue as apiDefault,
} from "../../../contexts/app/ApiContext";

const MOCK_ORGANIZATIONS = ["SPK", "LRL", "SWT", "MVP"];

// A simple stand-in for the page the user is redirected to after login
function PlatformsPage() {
  return <div data-testid="platforms-page">Platforms Page</div>;
}

const meta: Meta<typeof Login> = {
  component: Login,
  decorators: [
    (Story) => (
      <ApiContext value={apiDefault}>
        <AuthContext
          value={{
            user: undefined,
            isLoading: false,
            setUser: fn(),
            logout: fn(),
          }}
        >
          <OrganizationsContext value={{ organizations: MOCK_ORGANIZATIONS }}>
            <MemoryRouter initialEntries={["/login"]}>
              <Routes>
                <Route path="/login" element={<Story />} />
                <Route path="/platforms" element={<PlatformsPage />} />
              </Routes>
            </MemoryRouter>
          </OrganizationsContext>
        </AuthContext>
      </ApiContext>
    ),
  ],
  parameters: {
    layout: "fullscreen",
    msw: {
      handlers: [
        // Default: successful login handler
        http.post("/odcsapi/credentials/:org", () => {
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

export const Default: Story = {
  args: {
    organization: "SPK",
  },
  play: async ({ mount }) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const _canvas = await mount();
  },
};

export const FilledIn: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);

    await userEvent.type(await canvas.getByPlaceholderText(/username/i), "admin");
    await userEvent.type(await canvas.getByPlaceholderText(/password/i), "secret");

    await expect(canvas.getByRole("button", { name: /login/i })).toBeInTheDocument();
  },
};

export const SuccessfulLogin: Story = {
  args: {
    organization: "SPK",
  },
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement);

    // Fill in the username
    await userEvent.type(await canvas.getByPlaceholderText(/username/i), "admin");

    // Fill in the password
    await userEvent.type(await canvas.getByPlaceholderText(/password/i), "secret");

    // Set the organization
    userEvent.selectOptions(
      await canvas.getByRole("combobox", {
        name: "",
      }),
      args.organization,
    );

    // Submit the form
    await userEvent.click(canvas.getByRole("button", { name: /login/i }));

    // Verify the user was redirected to the platforms page
    await waitFor(() => {
      expect(canvas.getByTestId("platforms-page")).toBeInTheDocument();
    });
  },
};

export const FailedLogin: Story = {
  args: {
    organization: "SPK",
  },
  parameters: {
    msw: {
      handlers: [
        http.post("/odcsapi/credentials/:org", () => {
          return new HttpResponse(null, { status: 401 });
        }),
      ],
    },
  },
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement);

    await userEvent.type(canvas.getByPlaceholderText(/username/i), "wronguser");
    await userEvent.type(canvas.getByPlaceholderText(/password/i), "wrongpass");

    await userEvent.selectOptions(
      canvas.getByRole("combobox", { name: "" }),
      args.organization,
    );

    await userEvent.click(canvas.getByRole("button", { name: /login/i }));

    // The login page should still be visible (no redirect)
    await waitFor(() => {
      expect(canvas.getByRole("button", { name: /login/i })).toBeInTheDocument();
    });
  },
};

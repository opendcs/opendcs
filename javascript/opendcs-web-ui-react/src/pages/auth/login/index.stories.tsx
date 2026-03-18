import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, userEvent, within, waitFor } from "storybook/test";
import Login from "./index";
import { http, HttpResponse } from "msw";
import { WithOrganization } from "../../../../.storybook/mock/WithOrganization";

const ORG_HEADER = "x-organization-id";

const meta: Meta<typeof Login & { organizations: string[] }> = {
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

export const SuccessfulLogin: Story = {
  args: {
    organization: { name: "SPK" },
  },
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement);

    await userEvent.type(await canvas.getByPlaceholderText(/username/i), "admin");
    await userEvent.type(await canvas.getByPlaceholderText(/password/i), "secret");

    // Set the organization
    userEvent.selectOptions(
      await canvas.getByRole("combobox", {
        name: "",
      }),
      JSON.stringify(args.organization),
    );

    // Submit the form
    await userEvent.click(canvas.getByRole("button", { name: /login/i }));

    // Verify the user was redirected to the platforms page
    await waitFor(() => {
      expect(canvas.getByTestId("platforms-page")).toBeInTheDocument();
    });
  },
};

export const FailedLogin_BadCredentials: Story = {
  args: {
    organization: { name: "SPK" },
  },
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
      canvas.getByRole("combobox", { name: "" }),
      JSON.stringify(args.organization),
    );

    await userEvent.click(canvas.getByRole("button", { name: /login/i }));

    // The login page should still be visible (no redirect)
    await waitFor(() => {
      expect(canvas.getByRole("button", { name: /login/i })).toBeInTheDocument();
    });
  },
};

export const FailedLogin_BadOrg: Story = {
  args: {
    organization: { name: "LRL" },
  },
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

    await userEvent.type(canvas.getByPlaceholderText(/username/i), "admin");
    await userEvent.type(canvas.getByPlaceholderText(/password/i), "secret");

    await userEvent.selectOptions(
      canvas.getByRole("combobox", { name: "" }),
      JSON.stringify(args.organization),
    );

    await userEvent.click(canvas.getByRole("button", { name: /login/i }));

    // The login page should still be visible (no redirect)
    await waitFor(() => {
      expect(canvas.getByRole("button", { name: /login/i })).toBeInTheDocument();
    });
  },
};

export const SuccessfulLogin_HiddenOrganizations: Story = {
  args: {
    organizations: [],
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);

    await userEvent.type(canvas.getByPlaceholderText(/username/i), "admin");
    await userEvent.type(canvas.getByPlaceholderText(/password/i), "secret");

    await waitFor(() => {
      expect(canvas.queryByRole("combobox", { name: "" })).toBeNull();
    });

    await userEvent.click(canvas.getByRole("button", { name: /login/i }));

    // Successful login
    await waitFor(() => {
      expect(canvas.getByTestId("platforms-page")).toBeInTheDocument();
    });
  },
};

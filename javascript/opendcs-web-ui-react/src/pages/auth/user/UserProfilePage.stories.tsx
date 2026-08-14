import type { Decorator, Meta, StoryObj } from "@storybook/react-vite";
import { UserProfilePage } from "./UserProfilePage";
import { expect, fn } from "storybook/test";
import {
  ApiContext,
  defaultValue as apiDefault,
} from "../../../contexts/app/ApiContext";
import { AuthContext } from "../../../contexts/app/AuthContext";
import { BasicUser, CwmsUser } from "../../../../.storybook/mock/TestUsers";
import { http, HttpResponse } from "msw";

const meta = {
  component: UserProfilePage,
  parameters: {
    msw: {
      handlers: [
        http.post("/odcsapi/user/updatePassword", async ({ request }) => {
          const json = await request.json();
          console.log(json);
          const { currentPassword } = json as {
            currentPassword: string;
            newPassword: string;
          };

          if (currentPassword === "current password") {
            return new HttpResponse(null, { status: 200 });
          } else {
            return new HttpResponse(null, { status: 403, statusText: "Forbidden" });
          }
        }),
      ],
    },
  },
} satisfies Meta<typeof UserProfilePage>;

export default meta;

type Story = StoryObj<typeof meta>;

const authDecorator: Decorator = (Story) => (
  <ApiContext value={apiDefault}>
    <AuthContext
      value={{
        user: BasicUser,
        isLoading: false,
        loginSchemes: {},
        setSchemes: fn(),
        setUser: fn(),
        logout: fn(),
      }}
    >
      <Story />
    </AuthContext>
  </ApiContext>
);

const cwmsAuthDecorator: Decorator = (Story) => (
  <ApiContext value={apiDefault}>
    <AuthContext
      value={{
        user: CwmsUser,
        isLoading: false,
        loginSchemes: {},
        setSchemes: fn(),
        setUser: fn(),
        logout: fn(),
      }}
    >
      <Story />
    </AuthContext>
  </ApiContext>
);

export const Default: Story = {
  args: {},
  decorators: [authDecorator],
  play: async ({ mount }) => {
    const canvas = await mount();
    expect(await canvas.findByText(/Default/)).toBeInTheDocument();
  },
};

export const CwmsUserWithAdditionalOfficePermissions: Story = {
  args: {},
  decorators: [cwmsAuthDecorator],
  play: async ({ mount }) => {
    const canvas = await mount();

    expect(await canvas.findByText(/SPK/)).toBeInTheDocument();
    expect(await canvas.findByText(/SWT/)).toBeInTheDocument();
  },
};

import type { Decorator, Meta, StoryObj } from "@storybook/react-vite";
import { UserProfilePage } from "./UserProfilePage";
import { fn } from "storybook/test";
import {
  ApiContext,
  defaultValue as apiDefault,
} from "../../../contexts/app/ApiContext";
import { AuthContext } from "../../../contexts/app/AuthContext";
import { BasicUser } from "../../../../.storybook/mock/TestUsers";
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

export const Default: Story = {
  args: {},
  decorators: [authDecorator],
};

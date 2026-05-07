import type { Meta, StoryObj } from "@storybook/react-vite";
import { UserProfile } from "./UserProfile";
import { fn } from "storybook/test";
import {
  ApiContext,
  defaultValue as apiDefault,
} from "../../../contexts/app/ApiContext";
import { AuthContext } from "../../../contexts/app/AuthContext";
import { BasicUser } from "../../../../.storybook/mock/TestUsers";

const meta = {
  component: UserProfile,
} satisfies Meta<typeof UserProfile>;

export default meta;

type Story = StoryObj<typeof meta>;

const authDecorator = (Story: any) => (
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
  args: {
    updatePassword: fn(),
  },
  decorators: [authDecorator],
};

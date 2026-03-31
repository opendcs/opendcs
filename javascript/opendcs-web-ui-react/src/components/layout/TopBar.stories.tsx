import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, fn, userEvent } from "storybook/test";
import { TopBar } from "./TopBar";
import { ModeIcons } from "../ModeIcon";
import { AuthContext } from "../../contexts/app/AuthContext";
import { OrganizationsContext } from "../../contexts/app/OrganizationsContext";
import { ApiContext, defaultValue as apiDefault } from "../../contexts/app/ApiContext";
import { MOCK_ORGANIZATIONS } from "../../../.storybook/mock/WithOrganization";

const meta = {
  component: TopBar,
  decorators: [
    (Story) => {
      return (
        <>
          <ModeIcons />
          <Story />
        </>
      );
    },
  ],
} satisfies Meta<typeof TopBar>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    onToggleSidebar: fn(),
    sidebarOpen: false,
  },
  play: async ({ canvas, mount }) => {
    await mount();
    const text = await canvas.findByText("OpenDCS");
    expect(text).toBeInTheDocument();
  },
};

export const WithUser: Story = {
  args: {
    onToggleSidebar: fn(),
    sidebarOpen: false,
  },
  decorators: [
    (Story) => {
      return (
        <AuthContext
          value={{
            logout: fn(),
            setUser: fn(),
            loginSchemes: {},
            setSchemes: fn(),
            user: { email: "testuser@example.com" },
            isLoading: false,
          }}
        >
          <Story />
        </AuthContext>
      );
    },
  ],
  play: async ({ mount }) => {
    const canvas = await mount();
    const text = await canvas.findByText("OpenDCS");
    expect(text).toBeInTheDocument();
  },
};

export const WithChangeOrg: Story = {
  args: {
    onToggleSidebar: fn(),
    sidebarOpen: false,
  },
  decorators: [
    (Story) => {
      return (
        <AuthContext
          value={{
            logout: fn(),
            setUser: fn(),
            loginSchemes: {},
            setSchemes: fn(),
            user: { email: "testuser@example.com" },
            isLoading: false,
          }}
        >
          <OrganizationsContext value={{ organizations: MOCK_ORGANIZATIONS }}>
            <Story />
          </OrganizationsContext>
        </AuthContext>
      );
    },
  ],
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const text = await canvas.findByRole("button", {
      name: i18n.t("Change Organization"),
    });
    expect(text).toBeInTheDocument();
    await userEvent.click(text);
    const office = await canvas.findByText("SPK");
    expect(office).toBeInTheDocument();
  },
};

export const NoChangeOrgWhenNoOrgs: Story = {
  args: {
    onToggleSidebar: fn(),
    sidebarOpen: false,
  },
  decorators: [
    (Story, { args }) => {
      return (
        <ApiContext value={apiDefault}>
          <AuthContext
            value={{
              logout: fn(),
              setUser: fn(),
              loginSchemes: {},
              setSchemes: fn(),
              user: { email: "testuser@example.com" },
              isLoading: false,
            }}
          >
            <OrganizationsContext value={{ organizations: [] }}>
              <Story {...args} />
            </OrganizationsContext>
          </AuthContext>
        </ApiContext>
      );
    },
  ],
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const text = await canvas.queryByText(i18n.t("Change Organization"));
    expect(text).not.toBeInTheDocument();
  },
};

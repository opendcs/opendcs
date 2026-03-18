import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, fn } from "storybook/test";
import { PublicOnlyRoute } from "./PublicOnlyRoute";
import { AuthContext } from "../../contexts/app/AuthContext";
import { ApiContext, defaultValue as apiDefault } from "../../contexts/app/ApiContext";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { http, HttpResponse } from "msw";
import Login from "../../pages/auth/login";
import { Platforms } from "../../pages/platforms";
import { ApiPlatformRef } from "opendcs-api";

const platformMswHandlers = {
  platforms: http.get("/odcsapi/platformrefs", () => {
    return HttpResponse.json<ApiPlatformRef[]>([
      {
        platformId: 1,
        name: "Test Platform",
        agency: "USGS",
        transportmedium: "goes-self-timed",
        config: "test-config",
        description: "A test platform",
      },
      {
        platformId: 2,
        name: "Another Platform",
        agency: "NOAA",
        transportmedium: "polled-tcp",
        config: "noaa-config",
        description: "Another test platform",
      },
    ]);
  }),
};

const meta = {
  component: PublicOnlyRoute,
} satisfies Meta<typeof PublicOnlyRoute>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Loading: Story = {
  decorators: [
    (Story) => (
      <AuthContext
        value={{
          user: undefined,
          isLoading: true,
          setUser: fn(),
          logout: fn(),
        }}
      >
        <MemoryRouter initialEntries={["/login"]}>
          <Routes>
            <Route element={<Story />}>
              <Route path="/login" element={<div>Login Page</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthContext>
    ),
  ],
  play: async ({ canvas, mount }) => {
    await mount();
    const spinner = await canvas.findByRole("status");
    expect(spinner).toBeInTheDocument();
  },
};

export const NotAuthenticated: Story = {
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
          <MemoryRouter initialEntries={["/login"]}>
            <Routes>
              <Route element={<Story />}>
                <Route path="/login" element={<Login />} />
              </Route>
            </Routes>
          </MemoryRouter>
        </AuthContext>
      </ApiContext>
    ),
  ],
  play: async ({ canvas, mount, parameters }) => {
    await mount();
    const { i18n } = parameters;
    const usernameLabel = await canvas.findByText(i18n.t("username"));
    expect(usernameLabel).toBeInTheDocument();
  },
};

export const AlreadyAuthenticated: Story = {
  parameters: {
    msw: {
      handlers: platformMswHandlers,
    },
  },
  decorators: [
    (Story) => (
      <ApiContext value={apiDefault}>
        <AuthContext
          value={{
            user: { email: "testuser@example.com" },
            isLoading: false,
            setUser: fn(),
            logout: fn(),
          }}
        >
          <MemoryRouter initialEntries={["/login"]}>
            <Routes>
              <Route element={<Story />}>
                <Route path="/login" element={<Login />} />
              </Route>
              <Route path="/platforms" element={<Platforms />} />
            </Routes>
          </MemoryRouter>
        </AuthContext>
      </ApiContext>
    ),
  ],
  play: async ({ canvas, mount }) => {
    await mount();
    const platformsPage = await canvas.findByText("Test Platform");
    expect(platformsPage).toBeInTheDocument();
  },
};

import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, fn } from "storybook/test";
import { ProtectedRoute } from "./ProtectedRoute";
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
  component: ProtectedRoute,
} satisfies Meta<typeof ProtectedRoute>;

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
        <MemoryRouter initialEntries={["/platforms"]}>
          <Routes>
            <Route element={<Story />}>
              <Route path="/platforms" element={<Platforms />} />
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

export const Authenticated: Story = {
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
          <MemoryRouter initialEntries={["/platforms"]}>
            <Routes>
              <Route element={<Story />}>
                <Route path="/platforms" element={<Platforms />} />
              </Route>
            </Routes>
          </MemoryRouter>
        </AuthContext>
      </ApiContext>
    ),
  ],
  play: async ({ canvas, mount }) => {
    await mount();
    const content = await canvas.findByText("Test Platform");
    expect(content).toBeInTheDocument();
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
          <MemoryRouter initialEntries={["/platforms"]}>
            <Routes>
              <Route element={<Story />}>
                <Route path="/platforms" element={<Platforms />} />
              </Route>
              <Route path="/login" element={<Login />} />
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

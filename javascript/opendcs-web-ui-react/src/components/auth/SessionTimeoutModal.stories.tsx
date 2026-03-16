import type { Meta, StoryObj } from "@storybook/react-vite";
import { expect, screen, waitFor } from "storybook/test";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { http, HttpResponse } from "msw";
import { AuthProvider } from "../../contexts/app/AuthProvider";

// Shared MSW handlers — mock the session-check and logout endpoints.
const handlers = {
  checkSession: http.get("/odcsapi/check", () =>
    HttpResponse.json({ email: "testuser@example.com" }),
  ),
  logout: http.delete("/odcsapi/logout", () => new HttpResponse(null, { status: 204 })),
};

/**
 * Wrapper that renders AuthProvider with fast timeouts inside a minimal
 * router. The /login route gives the auto-logout story something to land on.
 */
const AuthTimeoutDemo = ({
  warnAfterMs,
  logoutAfterMs,
}: {
  warnAfterMs: number;
  logoutAfterMs: number;
}) => (
  <MemoryRouter initialEntries={["/app"]}>
    <Routes>
      <Route
        path="/app"
        element={
          <AuthProvider warnAfterMs={warnAfterMs} logoutAfterMs={logoutAfterMs}>
            <p>You are logged in.</p>
          </AuthProvider>
        }
      />
      <Route path="/login" element={<p>You have been logged out.</p>} />
    </Routes>
  </MemoryRouter>
);

const meta = {
  component: AuthTimeoutDemo,
  parameters: {
    msw: {
      handlers,
    },
  },
} satisfies Meta<typeof AuthTimeoutDemo>;

export default meta;

type Story = StoryObj<typeof meta>;

/**
 * After half a second of inactivity (no API calls) the warning modal appears.
 * The logout timer is set far in the future so the modal stays visible.
 */
export const WarningModalAppears: Story = {
  args: {
    warnAfterMs: 500,
    logoutAfterMs: 600_000,
  },
  play: async ({ canvas, mount }) => {
    await mount();
    // Confirm the app content is visible while session is live.
    await canvas.findByText("You are logged in.");
    // After ~500 ms of no API activity the warning modal should appear.
    // The modal renders via a React Bootstrap Portal to document.body, so we
    // must use screen (document-wide) rather than canvas (story container only).
    await waitFor(
      () => expect(screen.getByText("Session Expiring")).toBeInTheDocument(),
      { timeout: 3000 },
    );
  },
};

/**
 * After half a second the warning modal appears, the countdown reaches 0,
 * and the user is automatically redirected to the login page.
 *
 * Timings:
 *  - warnAfterMs=300  → modal appears at ~300 ms
 *  - logoutAfterMs=1400 → warningDurationS = round((1400-300)/1000) = 1 s
 *  - countdown ticks from 1 → 0, then forceLogout fires at ~1300 ms total
 */
export const AutoLogoutOnTimeout: Story = {
  args: {
    warnAfterMs: 300,
    logoutAfterMs: 1400,
  },
  play: async ({ canvas, mount }) => {
    await mount();
    await canvas.findByText("You are logged in.");
    // Modal appears after ~300 ms (portal → use screen).
    await waitFor(
      () => expect(screen.getByText("Session Expiring")).toBeInTheDocument(),
      { timeout: 2000 },
    );
    // Countdown reaches 0 and the user is redirected to /login.
    // The modal disappears and the login route renders inside the canvas.
    await waitFor(
      () => expect(canvas.getByText("You have been logged out.")).toBeInTheDocument(),
      { timeout: 4000 },
    );
  },
};

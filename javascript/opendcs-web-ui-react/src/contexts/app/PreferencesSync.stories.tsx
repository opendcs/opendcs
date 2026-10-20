import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { expect, fn, userEvent, waitFor } from "storybook/test";
import { delay, http, HttpResponse } from "msw";
import type { User } from "opendcs-api";
import { PreferencesSync } from "./PreferencesSync";
import { AuthContext } from "./AuthContext";
import { ThemeProvider } from "./ThemeProvider";
import { useTheme } from "./ThemeContext";
import { SiteNameTypeProvider } from "./SiteNameTypeProvider";
import { useSiteNameType } from "./SiteNameTypeContext";
import { useSavePreferences } from "../../hooks/useUserPreferences";

// Records each PATCH of the user's profile preferences.
const savedPreferences = fn();

function Settings() {
  const { theme, setTheme } = useTheme();
  const { siteNameType } = useSiteNameType();
  return (
    <>
      <output data-testid="color-mode">{theme.colorMode}</output>
      <output data-testid="site-name-type">{siteNameType.preferredType}</output>
      <button onClick={() => setTheme({ colorMode: "dark" })}>Dark</button>
    </>
  );
}

function SaveTwice() {
  const savePreferences = useSavePreferences();
  return (
    <button
      onClick={() => {
        savePreferences({ language: "de-DE" });
        savePreferences({ siteNameType: "USGS" });
      }}
    >
      Save twice
    </button>
  );
}

/** Signs a user with the given profile preferences in and out around PreferencesSync. */
function Harness({ profile }: { profile: Record<string, unknown> }) {
  const [user, setUser] = useState<User | undefined>(undefined);
  return (
    <ThemeProvider>
      <SiteNameTypeProvider>
        <AuthContext
          value={{
            user,
            setUser,
            isLoading: false,
            loginSchemes: {},
            setSchemes: fn(),
            logout: fn(),
          }}
        >
          <PreferencesSync />
          <Settings />
          <button
            onClick={() => setUser({ email: "user@example.com", preferences: profile })}
          >
            Sign in
          </button>
          <SaveTwice />
        </AuthContext>
      </SiteNameTypeProvider>
    </ThemeProvider>
  );
}

const meta = {
  component: Harness,
  parameters: {
    msw: {
      handlers: [
        http.patch("/odcsapi/user/preferences", async ({ request }) => {
          const changes = await request.json();
          savedPreferences(changes);
          return HttpResponse.json(changes);
        }),
      ],
    },
  },
  beforeEach: () => {
    savedPreferences.mockClear();
  },
} satisfies Meta<typeof Harness>;

export default meta;

type Story = StoryObj<typeof meta>;

// A save, if one were coming, is sent from the same effect that applies the
// profile, so this is ample time for it to reach the mock server.
const settle = () => new Promise((resolve) => setTimeout(resolve, 200));

export const AppliesTheProfileAtSignIn: Story = {
  args: { profile: { colorMode: "dark", siteNameType: "USGS" } },
  play: async ({ canvas }) => {
    await expect(canvas.getByTestId("color-mode")).toHaveTextContent("auto");

    await userEvent.click(canvas.getByRole("button", { name: "Sign in" }));

    await waitFor(() =>
      expect(canvas.getByTestId("color-mode")).toHaveTextContent("dark"),
    );
    await expect(canvas.getByTestId("site-name-type")).toHaveTextContent("USGS");
    await settle();
    await expect(savedPreferences).not.toHaveBeenCalled();
  },
};

export const SavesChangesDuringTheSession: Story = {
  args: { profile: { colorMode: "light" } },
  play: async ({ canvas }) => {
    await userEvent.click(canvas.getByRole("button", { name: "Sign in" }));
    await waitFor(() =>
      expect(canvas.getByTestId("color-mode")).toHaveTextContent("light"),
    );

    await userEvent.click(canvas.getByRole("button", { name: "Dark" }));

    await waitFor(() =>
      expect(savedPreferences).toHaveBeenCalledWith({ colorMode: "dark" }),
    );
    await settle();
    await expect(savedPreferences).toHaveBeenCalledTimes(1);
  },
};

export const ChoiceMadeBeforeSignInWins: Story = {
  args: { profile: { colorMode: "light" } },
  play: async ({ canvas }) => {
    await userEvent.click(canvas.getByRole("button", { name: "Dark" }));
    await userEvent.click(canvas.getByRole("button", { name: "Sign in" }));

    await waitFor(() =>
      expect(savedPreferences).toHaveBeenCalledWith({ colorMode: "dark" }),
    );
    await expect(canvas.getByTestId("color-mode")).toHaveTextContent("dark");
  },
};

// Settings kept only in this browser before profiles held them are copied over.
export const SeedsAnEmptyProfile: Story = {
  args: { profile: {} },
  beforeEach: () => {
    localStorage.setItem("theme-mode", "dark");
  },
  play: async ({ canvas }) => {
    await userEvent.click(canvas.getByRole("button", { name: "Sign in" }));

    // The site name type is still the default, so only the color mode is copied.
    await waitFor(() =>
      expect(savedPreferences).toHaveBeenCalledWith({ colorMode: "dark" }),
    );
  },
};

// The server merges each save into what it last stored, so overlapping saves
// could drop each other's keys.
let inFlight = 0;
let mostInFlight = 0;

export const SavesAreSentOneAtATime: Story = {
  args: { profile: {} },
  parameters: {
    msw: {
      handlers: [
        http.patch("/odcsapi/user/preferences", async ({ request }) => {
          inFlight++;
          mostInFlight = Math.max(mostInFlight, inFlight);
          const changes = await request.json();
          await delay(50);
          savedPreferences(changes);
          inFlight--;
          return HttpResponse.json(changes);
        }),
      ],
    },
  },
  beforeEach: () => {
    inFlight = 0;
    mostInFlight = 0;
  },
  play: async ({ canvas }) => {
    await userEvent.click(canvas.getByRole("button", { name: "Save twice" }));

    await waitFor(() => expect(savedPreferences).toHaveBeenCalledTimes(2));
    await expect(mostInFlight).toBe(1);
    await expect(savedPreferences).toHaveBeenNthCalledWith(1, { language: "de-DE" });
    await expect(savedPreferences).toHaveBeenNthCalledWith(2, { siteNameType: "USGS" });
  },
};

import type { Decorator, Preview } from "@storybook/react-vite";
import "datatables.net-bs5";
import "datatables.net-responsive-bs5";

import "../src/styles/main.scss";
import "datatables.net-bs5/css/dataTables.bootstrap5.css";
import "datatables.net-buttons-bs5/css/buttons.bootstrap5.css";
import { WithRefLists } from "./mock/WithRefLists";
import { initialize, mswLoader } from "msw-storybook-addon";
import { WithI18next } from "./mock/WithI18Next";
import i18n from "../src/i18n";
import { WithTheme } from "./mock/WithTheme";

// MSW setup
initialize(
  {
    onUnhandledRequest(request, print) {
      const url = new URL(request.url);
      // Ignore warnings for specific URLs (e.g., /api/health)
      if (!url.pathname.startsWith("/odcsapi")) {
        return;
      }
      // For all other unhandled requests, print the warning
      print.warning();
    },
  },
  [],
);

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    reactOptions: {
      legacyRootApi: true,
      strictMode: false,
    },

    a11y: {
      // 'todo' - show a11y violations in the test UI only
      // 'error' - fail CI on a11y violations
      // 'off' - skip a11y checks entirely
      test: "todo",
    },
    i18n,
  },
  decorators: [WithI18next, WithTheme, WithRefLists],
  globalTypes: {
    locale: {
      name: "Locale",
      description: "Internationalization locale",
      toolbar: {
        icon: "globe",
        title: "Language",
        items: [
          { value: "en-US", title: "English" },
          { value: "de-DE", title: "Deutsch" },
          { value: "es-ES", title: "Spanish" },
        ],
        dynamicTitle: true,
      },
    },
    colorMode: {
      name: "ColorMode",
      description: "Whether to show as light/dark/or auto color scheme",
      toolbar: {
        title: "ColorMode",
        icon: "sun",
        items: ["light", "dark", "auto"],
        dynamicTitle: true,
      },
    },
  },
  initialGlobals: {
    locale: "en-US",
  },
  loaders: [mswLoader],
};

export default preview;

import type { Decorator, Preview } from "@storybook/react-vite";
import "datatables.net-bs5";
import "datatables.net-responsive-bs5";
import i18n from "../src/i18n";
import "bootstrap/dist/css/bootstrap.min.css";
import "datatables.net-bs5/css/dataTables.bootstrap5.css";
import "datatables.net-buttons-bs5/css/buttons.bootstrap5.css";

import { Dispatch, SetStateAction, Suspense, useEffect, useState } from "react";
import { I18nextProvider } from "react-i18next";
import { Theme, ThemeContext } from "../src/contexts/app/ThemeContext";
import { useGlobals } from "storybook/internal/preview-api";
import { WithRefLists } from "./mock/WithRefLists";
import { initialize, mswLoader } from "msw-storybook-addon";
import { http, HttpResponse } from "msw";
import { ApiSiteRef } from "../../../java/api-clients/api-client-typescript/build/generated/openApi/dist";

// Wrap your stories in the I18nextProvider component
// lifted direct from https://storybook.js.org/recipes/react-i18next
// eslint-disable-next-line react-refresh/only-export-components
const WithI18next: Decorator = (Story, context) => {
  const { locale } = context.globals;

  // When the locale global changes
  // Set the new locale in i18n
  useEffect(() => {
    console.log(`changing lang to ${locale}`);
    i18n.changeLanguage(locale);
  }, [locale]);

  return (
    // This catches the suspense from components not yet ready (still loading translations)
    // Alternative: set useSuspense to false on i18next.options.react when initializing i18next
    <Suspense fallback={<div>loading translations...</div>}>
      <I18nextProvider i18n={i18n}>
        <Story />
      </I18nextProvider>
    </Suspense>
  );
};

i18n.on("languageChanged", (locale) => {
  const direction = i18n.dir(locale);
  document.dir = direction;
});

// eslint-disable-next-line react-refresh/only-export-components
const WithTheme: Decorator = (Story) => {
  const [{ colorMode }, updateGlobals] = useGlobals();

  const [theme, setTheme] = useState<Theme>({ colorMode: colorMode });

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setTheme({ colorMode: colorMode });
  }, [colorMode]);

  const setGlobalTheme: Dispatch<SetStateAction<Theme>> = (action) => {
    if (action as Theme) {
      updateGlobals({ colorMode: (action as Theme).colorMode });
    }

    setTheme(action);
  };

  return (
    <ThemeContext value={{ theme: theme, setTheme: setGlobalTheme }}>
      <Story />
    </ThemeContext>
  );
};

// end lift

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

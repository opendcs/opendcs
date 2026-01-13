import type { Decorator, Preview } from "@storybook/react-vite";
import "bootstrap/dist/css/bootstrap.min.css";
import "datatables.net-bs5";
import "datatables.net-responsive-bs5";
import "../src/main.css";
import "../src/assets/opendcs-shim.css";
import i18n from "../src/i18n";

import { Suspense, useEffect } from "react";
import { I18nextProvider } from "react-i18next";


// Wrap your stories in the I18nextProvider component
// lifted direct from https://storybook.js.org/recipes/react-i18next
const WithI18next: Decorator = (Story, context) => {
  const {locale} = context.globals;

  // When the locale global changes
  // Set the new locale in i18n
  useEffect(() => {
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

i18n.on('languageChanged', (locale) => {
  console.log("Hello?");
  const direction = i18n.dir(locale);
  document.dir = direction;
});

// end lift

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
  decorators: [WithI18next],
  globalTypes: {
    locale: {
      name: 'Locale',
      description: 'Internationalization locale',
      toolbar: {
        icon: 'globe',
        title: 'Language',
        items: [
          { value: 'en-US', title: 'English' },
          { value: 'de', title: 'Deutsch' },
          { value: 'es', title: 'Spanish'}
        ],
        dynamicTitle: true
      },
    },
  },
  initialGlobals: {
    locale: 'en-US',
  },
 
};

export default preview;

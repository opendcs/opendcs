// Wrap your stories in the I18nextProvider component
// lifted direct from https://storybook.js.org/recipes/react-i18next

import { Decorator } from "@storybook/react-vite";
import { Suspense, useEffect } from "react";
import i18n from "../../src/i18n";
import { I18nextProvider } from "react-i18next";

export const WithI18next: Decorator = (Story, context) => {
  const { locale } = context.globals;

  // When the locale global changes
  // Set the new locale in i18n
  useEffect(() => {
    console.log(`changing lang to ${locale}`);
    i18n.changeLanguage(locale);
  }, [locale]);

  console.log(`Now using ${locale}`);
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
  // preload everything, otherwise the play functions can't always use the translation values
  // correctly.
  i18n.loadNamespaces(["sites", "algorithms", "colormode", "platforms", "properties"]);
});

// end lift

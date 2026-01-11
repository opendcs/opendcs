import type { Preview } from "@storybook/react-vite";
//import { useArgs } from '@storybook/preview-api';
import "bootstrap/dist/css/bootstrap.min.css";
import "datatables.net-bs5";
import "datatables.net-responsive-bs5";
import "../src/main.css";
import "../src/assets/opendcs-shim.css";
import i18n from "../src/i18n";

import { Suspense } from "react";
import { I18nextProvider } from "react-i18next";
import { JSX } from "react/jsx-runtime";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
globalThis.IS_REACT_ACT_ENVIRONMENT = true;

// Wrap your stories in the I18nextProvider component
// lifted direct from https://storybook.js.org/recipes/react-i18next
const withI18next = (Story: JSX.Element) => {
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
    decorators: [withI18next],
    a11y: {
      // 'todo' - show a11y violations in the test UI only
      // 'error' - fail CI on a11y violations
      // 'off' - skip a11y checks entirely
      test: "todo",
    },
  },
};

export default preview;

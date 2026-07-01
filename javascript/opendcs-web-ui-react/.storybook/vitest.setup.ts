import * as a11yAddonAnnotations from "@storybook/addon-a11y/preview";
import { setProjectAnnotations } from "@storybook/react-vite";
import * as projectAnnotations from "./preview";
import { configure } from "@testing-library/react";

// This is an important step to apply the right configuration when testing your stories.
// More info at: https://storybook.js.org/docs/api/portable-stories/portable-stories-vitest#setprojectannotations
setProjectAnnotations([a11yAddonAnnotations, projectAnnotations]);

// Increase the default waitFor/findBy timeout from 1s to 5s. On slower CI
// runners, the sequence of createRoot render → MSW fetch → React Suspense
// resolution → DetailFade animation (two rAF calls + 300ms setTimeout) can
// exceed the 1s default, causing flaky "unable to find element" failures.
configure({ asyncUtilTimeout: 5000 });

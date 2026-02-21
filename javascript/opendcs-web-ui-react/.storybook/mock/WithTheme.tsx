import { Decorator } from "@storybook/react-vite";
import { useGlobals } from "storybook/internal/preview-api";
import { Theme, ThemeContext } from "../../src/contexts/app/ThemeContext";
import { Dispatch, SetStateAction } from "react";

export const WithTheme: Decorator = (Story) => {
  const [{ colorMode }, updateGlobals] = useGlobals();

  const setGlobalTheme: Dispatch<SetStateAction<Theme>> = (action) => {
    if (action as Theme) {
      updateGlobals({ colorMode: (action as Theme).colorMode });
    }
  };

  return (
    <ThemeContext value={{ theme: colorMode, setTheme: setGlobalTheme }}>
      <Story />
    </ThemeContext>
  );
};

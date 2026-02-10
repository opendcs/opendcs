import { Decorator } from "@storybook/react-vite";
import { useEffect, useGlobals, useState } from "storybook/internal/preview-api";
import { Theme, ThemeContext } from "../../src/contexts/app/ThemeContext";
import { Dispatch, SetStateAction } from "react";

export const WithTheme: Decorator = (Story) => {
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

import { useEffect } from "react";
import { useTheme } from "../contexts/ThemeContext";
import ModeIcon from "./ModeIcon";
import { Button, NavDropdown } from "react-bootstrap";

export const ColorModes = () => {
  const { theme, setTheme } = useTheme();

  useEffect(() => {
    if (theme.colorMode === "auto") {
      document.documentElement.dataset.bsTheme = globalThis.matchMedia(
        "(prefers-color-scheme: dark)",
      ).matches
        ? "dark"
        : "light";
    } else {
      document.documentElement.dataset.bsTheme = theme.colorMode;
    }
    localStorage.setItem("theme-mode", theme.colorMode);
  }, [theme]);

  let ActiveIcon;
  switch (theme.colorMode) {
    case "light":
      ActiveIcon = (
        <ModeIcon
          name="sun-fill"
          className="bi me-2 opacity-50 my-1 mode-icon-active  theme-icon"
        />
      );
      break;
    case "dark":
      ActiveIcon = (
        <ModeIcon
          name="moon-stars-fill"
          className="bi me-2 opacity-50 my-1 mode-icon-active  theme-icon"
        />
      );
      break;
    case "auto":
      ActiveIcon = (
        <ModeIcon
          name="circle-half"
          className="bi me-2 opacity-50 theme-icon mode-icon-active"
        />
      );
      break;
  }

  return (
    <NavDropdown title={ActiveIcon} id="color-mode" drop="start">
      <NavDropdown.Item>
        <Button onClick={() => setTheme({ colorMode: "light" })}>
          <ModeIcon name="sun-fill" className="bi me-2 opacity-50 theme-icon" />
          Light
        </Button>
      </NavDropdown.Item>
      <NavDropdown.Item>
        <Button onClick={() => setTheme({ colorMode: "dark" })}>
          <ModeIcon name="moon-stars-fill" className="bi me-2 opacity-50 theme-icon" />
          Dark
        </Button>
      </NavDropdown.Item>
      <NavDropdown.Item>
        <Button onClick={() => setTheme({ colorMode: "auto" })}>
          <ModeIcon name="circle-half" className="bi me-2 opacity-50 theme-icon" />
          Auto
        </Button>
      </NavDropdown.Item>
    </NavDropdown>
  );
};

export default ColorModes;

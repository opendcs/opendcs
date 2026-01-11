import { useEffect } from "react";
import { useTheme } from "../contexts/ThemeContext";
import ModeIcon from "./ModeIcon";
import { Button, NavDropdown } from "react-bootstrap";
import { useTranslation } from "react-i18next";

export const ColorModes = () => {
  const { theme, setTheme } = useTheme();
  const [t] = useTranslation("colormode");
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
    <NavDropdown title={ActiveIcon} id="color-mode" drop="start"
                 aria-label={t("colormode:dropdownLabel")}>
      <NavDropdown.Item role="menuitem">
        <Button onClick={() => setTheme({ colorMode: "light" })}
                aria-label={t("colormode:activate", {mode: 'Light'})}>
          <ModeIcon name="sun-fill" className="bi me-2 opacity-50 theme-icon" />
          {t("translation:Light")}
        </Button>
      </NavDropdown.Item>
      <NavDropdown.Item role="menuitem">
        <Button onClick={() => setTheme({ colorMode: "dark" })}
                aria-label={t("colormode:activate", {mode: 'Dark'})}>
          <ModeIcon name="moon-stars-fill" className="bi me-2 opacity-50 theme-icon" />
          {t("translation:Dark")}
        </Button>
      </NavDropdown.Item>
      <NavDropdown.Item role="menuitem">
        <Button onClick={() => setTheme({ colorMode: "auto" })}
                aria-label={t("colormode:activate", {mode: 'Auto'})}>
          <ModeIcon name="circle-half" className="bi me-2 opacity-50 theme-icon" />
          {t("translation:Auto")}
        </Button>
      </NavDropdown.Item>
    </NavDropdown>
  );
};

export default ColorModes;

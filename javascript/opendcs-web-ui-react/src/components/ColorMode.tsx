import { useEffect } from "react";
import { useTheme } from "../contexts/app/ThemeContext";
import { ModeIcon } from "./ModeIcon";
import { Button, Dropdown } from "react-bootstrap";
import { useTranslation } from "react-i18next";

interface ToggleProperties {
  icon: React.ReactNode;
}

export const ColorToggle: React.FC<ToggleProperties> = ({ icon, ...args }) => {
  //const [t] = useTranslation();
  return <Button {...args}>{icon}</Button>;
};

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

  let iconName = "circle-half";
  switch (theme.colorMode) {
    case "light":
      iconName = "sun-fill";
      break;
    case "dark":
      iconName = "moon-stars-fill";
      break;
    case "auto":
      iconName = "circle-half";
      break;
  }

  const ActiveIcon = (
    <ModeIcon
      name={iconName}
      height="1em"
      width="1em"
      className="bi me-2 opacity-50 my-1 mode-icon-active"
    />
  );

  return (
    <Dropdown drop="start">
      <Dropdown.Toggle
        as={ColorToggle}
        id="color-mode"
        icon={ActiveIcon}
        size="lg"
        aria-label={t("colormode:dropdownLabel")}
      />
      <Dropdown.Menu>
        <Dropdown.Item
          onClick={() => setTheme({ colorMode: "light" })}
          color="text-primary"
          aria-label={t("colormode:activate", { mode: "Light" })}
          className="icon-link"
        >
          <ModeIcon
            name="sun-fill"
            className="bi me-2 opacity-50 theme-icon text-primary"
          />
          {t("translation:Light")}
        </Dropdown.Item>
        <Dropdown.Item
          onClick={() => setTheme({ colorMode: "dark" })}
          color="text-primary"
          aria-label={t("colormode:activate", { mode: "Dark" })}
          className="icon-link"
        >
          <ModeIcon name="moon-stars-fill" className="bi me-2 opacity-50 theme-icon" />
          {t("translation:Dark")}
        </Dropdown.Item>
        <Dropdown.Item
          onClick={() => setTheme({ colorMode: "auto" })}
          color="text-primary"
          aria-label={t("colormode:activate", { mode: "Auto" })}
          className="icon-link"
        >
          <ModeIcon name="circle-half" className="bi me-2 opacity-50 theme-icon" />
          {t("translation:Auto")}
        </Dropdown.Item>
      </Dropdown.Menu>
    </Dropdown>
  );
};

export default ColorModes;

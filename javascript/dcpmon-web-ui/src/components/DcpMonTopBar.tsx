import Button from "react-bootstrap/Button";
import Container from "react-bootstrap/Container";
import Nav from "react-bootstrap/Nav";
import Navbar from "react-bootstrap/Navbar";
import { MoonStarsFill, SunFill } from "react-bootstrap-icons";
import { useDisplaySettings } from "../displaySettingsStore";

/** Standalone form of the OpenDCS top bar used by the main web application. */
export function DcpMonTopBar() {
  const { effectiveTheme, toggleTheme } = useDisplaySettings();
  const nextTheme = effectiveTheme === "dark" ? "light" : "dark";

  return (
    <Navbar fixed="top" className="odcs-topbar">
      <Container fluid className="odcs-topbar__container">
        <div className="odcs-topbar__left">
          <Navbar.Brand href="/">OpenDCS</Navbar.Brand>
        </div>
        <Nav className="odcs-topbar__actions">
          <Button
            variant="link"
            className="odcs-theme-toggle"
            onClick={toggleTheme}
            aria-label={`Switch to ${nextTheme} mode`}
            title={`Switch to ${nextTheme} mode`}
          >
            {nextTheme === "dark" ? (
              <MoonStarsFill aria-hidden="true" />
            ) : (
              <SunFill aria-hidden="true" />
            )}
          </Button>
          <Navbar.Text>DCP Monitor</Navbar.Text>
        </Nav>
      </Container>
    </Navbar>
  );
}

import { useContext } from "react";
import Nav from "react-bootstrap/Nav";
import Navbar from "react-bootstrap/Navbar";
import { AuthContext } from "../../contexts/app/AuthContext";
import { ColorModes } from "../";
import LangPicker from "../menus/Language/LangPicker";
import UserMenu from "../menus/User/UserMenu";
import { Button, Container } from "react-bootstrap";
import { List, X } from "react-bootstrap-icons";

export interface TopBarProps {
  onToggleSidebar: () => void;
  sidebarOpen: boolean;
}

export function TopBar({ onToggleSidebar, sidebarOpen }: TopBarProps) {
  const { user, logout } = useContext(AuthContext);

  return (
    <Navbar sticky="top" className="odcs-topbar">
      <Container fluid className="odcs-topbar__container">
        <div className="odcs-topbar__left">
          <Button
            variant="link"
            className="odcs-topbar__hamburger d-lg-none"
            onClick={onToggleSidebar}
            aria-label="Toggle sidebar"
            aria-expanded={sidebarOpen}
          >
            {sidebarOpen ? <X size={20} /> : <List size={20} />}
          </Button>
          <Navbar.Brand href="#">OpenDCS</Navbar.Brand>
        </div>
        <Nav className="odcs-topbar__actions">
          <Nav.Item>
            <LangPicker />
          </Nav.Item>
          <Nav.Item>
            <ColorModes />
          </Nav.Item>
          {user && <UserMenu user={user} logout={logout} />}
        </Nav>
      </Container>
    </Navbar>
  );
}

import { Nav } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { Link, Outlet, useLocation } from "react-router-dom";

export interface SideBarProps {
  open: boolean;
  onClose: () => void;
}

export const SideBar = ({ open, onClose }: SideBarProps) => {
  const [t] = useTranslation(["platforms", "sites", "algorithms"]);
  const location = useLocation();

  return (
    <>
      {/* Overlay backdrop for mobile */}
      {open && (
        <div className="odcs-sidebar__overlay" onClick={onClose} aria-hidden="true" />
      )}

      <nav className={`odcs-sidebar ${open ? "show" : ""}`}>
        <div className="odcs-sidebar__section-title">Decodes</div>
        <Nav className="flex-column">
          <Nav.Link
            as={Link}
            to="/platforms"
            active={location.pathname === "/platforms"}
            onClick={onClose}
          >
            {t("platforms:platformsTitle")}
          </Nav.Link>
          <Nav.Link
            as={Link}
            to="/sites"
            active={location.pathname === "/sites"}
            onClick={onClose}
          >
            {t("sites:title")}
          </Nav.Link>
        </Nav>

        <div className="odcs-sidebar__section-title">Computation</div>
        <Nav className="flex-column">
          <Nav.Link
            as={Link}
            to="/algorithms"
            active={location.pathname === "/algorithms"}
            onClick={onClose}
          >
            {t("algorithms:algorithmsTitle")}
          </Nav.Link>
        </Nav>
      </nav>

      <main className="flex-grow-1 p-3 overflow-auto" style={{ minWidth: 0 }}>
        <Outlet />
      </main>
    </>
  );
};

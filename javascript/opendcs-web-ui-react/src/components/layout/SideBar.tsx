import { Container, ListGroup, Nav } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { Link, Outlet } from "react-router-dom";

export const SideBar = () => {
  const [t] = useTranslation(["platforms", "sites", "algorithms"]);

  return (
    <>
      <Nav
        defaultActiveKey="/platform"
        className="navbar-light bg-primary d-inline-flex d-inline-flex vh-100 page-sidebar"
      >
        <ListGroup title="DECODES" className="nav-item ps-2 flex-column">
          <Nav.Item as={ListGroup.Item} className="mb-2">
            <span className="text-primary-emphasis fw-bold">Decodes</span>
            <Nav.Link as={Link} to="/platforms" className="">
              {t("platforms:platformsTitle")}
            </Nav.Link>
            <Nav.Link as={Link} to="/sites">
              {t("sites:sitesTitle")}
            </Nav.Link>
          </Nav.Item>

          <Nav.Item as={ListGroup.Item} className="mb-2">
            <span className="text-primary-emphasis fw-bold">Computation</span>
            <Nav.Link as={Link} to="/algorithms">
              {t("algorithms:algorithmsTitle")}
            </Nav.Link>
          </Nav.Item>
        </ListGroup>
      </Nav>
      <Container className="page-content flex-grow-1">
        <Outlet />
      </Container>
    </>
  );
};

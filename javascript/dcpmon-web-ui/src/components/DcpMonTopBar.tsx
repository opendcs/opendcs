import Container from "react-bootstrap/Container";
import Nav from "react-bootstrap/Nav";
import Navbar from "react-bootstrap/Navbar";

/** Standalone form of the OpenDCS top bar used by the main web application. */
export function DcpMonTopBar() {
  return (
    <Navbar fixed="top" className="odcs-topbar">
      <Container fluid className="odcs-topbar__container">
        <div className="odcs-topbar__left">
          <Navbar.Brand href="/">OpenDCS</Navbar.Brand>
        </div>
        <Nav className="odcs-topbar__actions">
          <Navbar.Text>DCP Monitor</Navbar.Text>
        </Nav>
      </Container>
    </Navbar>
  );
}

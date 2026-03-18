import { Button, Modal } from "react-bootstrap";

interface Props {
  show: boolean;
  secondsRemaining: number;
  onStay: () => void;
  onLogout: () => void;
}

export const SessionTimeoutModal = ({
  show,
  secondsRemaining,
  onStay,
  onLogout,
}: Props) => (
  <Modal show={show} centered backdrop="static" keyboard={false}>
    <Modal.Header>
      <Modal.Title>Session Expiring</Modal.Title>
    </Modal.Header>
    <Modal.Body>
      Your session will expire in {secondsRemaining} second
      {secondsRemaining !== 1 ? "s" : ""} due to inactivity.
    </Modal.Body>
    <Modal.Footer>
      <Button variant="secondary" onClick={onLogout}>
        Log Out
      </Button>
      <Button variant="primary" onClick={onStay}>
        Stay Logged In
      </Button>
    </Modal.Footer>
  </Modal>
);

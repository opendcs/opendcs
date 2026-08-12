import { useSyncExternalStore } from "react";
import { Toast, ToastContainer } from "react-bootstrap";
import { subscribeToasts, getToasts } from "../../lib/toastBus";

export const ToastStack: React.FC = () => {
  const toasts = useSyncExternalStore(subscribeToasts, getToasts);

  return (
    <ToastContainer
      position="bottom-end"
      className="position-fixed p-3"
      style={{ zIndex: 1060 }}
    >
      {toasts.map((t) => (
        <Toast key={t.id} bg={t.variant} autohide delay={t.ttlMs} show>
          <Toast.Body className="text-white fw-semibold">{t.message}</Toast.Body>
        </Toast>
      ))}
    </ToastContainer>
  );
};

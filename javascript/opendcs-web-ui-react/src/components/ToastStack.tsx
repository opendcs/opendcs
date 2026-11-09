import { useSyncExternalStore } from "react";
import { Toast, ToastContainer } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { dismissToast, getToasts, subscribeToasts } from "../util/toastBus";

/** Renders the toasts pushed onto util/toastBus, newest at the bottom. */
export const ToastStack: React.FC = () => {
  const { t } = useTranslation();
  const toasts = useSyncExternalStore(subscribeToasts, getToasts);

  return (
    <ToastContainer position="bottom-end" className="position-fixed p-3">
      {toasts.map((toast) => (
        <Toast key={toast.id} bg={toast.variant}>
          <Toast.Body className="d-flex align-items-start text-white">
            <div className="me-auto">
              <div className="fw-semibold">{t(toast.title)}</div>
              {toast.detail && <div>{toast.detail}</div>}
            </div>
            <button
              type="button"
              className="btn-close btn-close-white ms-2"
              aria-label={t("translation:close")}
              onClick={() => dismissToast(toast.id)}
            />
          </Toast.Body>
        </Toast>
      ))}
    </ToastContainer>
  );
};

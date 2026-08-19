import { Alert } from "react-bootstrap";

interface SaveErrorAlertProps {
  /** Message to show; renders nothing when null. */
  error: string | null;
  /** Called when the user dismisses the alert. */
  onClose: () => void;
  className?: string;
}

// Dismissible banner for a failed save, normally placed just above the
// form's action row. Feed it from useSaveError().
export const SaveErrorAlert: React.FC<SaveErrorAlertProps> = ({
  error,
  onClose,
  className = "mt-3",
}) =>
  error ? (
    <Alert variant="danger" dismissible onClose={onClose} className={className}>
      {error}
    </Alert>
  ) : null;

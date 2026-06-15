import type { PropsWithChildren } from "react";
import { Button, type ButtonProps, Col, Row } from "react-bootstrap";
import { Save, X } from "react-bootstrap-icons";
import { useTranslation } from "react-i18next";

// Layout shell for the bottom-right action row on an edit card. Compose any
// buttons you want as children — typically <CancelButton/> + <SaveButton/>,
// optionally with a <DeleteButton/> or extra controls in between.
export const EditFormActions: React.FC<PropsWithChildren> = ({ children }) => (
  <Row className="mt-3">
    <Col className="d-flex justify-content-end gap-2">{children}</Col>
  </Row>
);

// Standard Cancel button: secondary variant, X icon, localized "Cancel" label.
// Override `children` for a different label/icon; spread the rest of ButtonProps
// for onClick, aria-label, disabled, type, etc.
export const CancelButton: React.FC<ButtonProps> = ({
  children,
  variant = "secondary",
  ...rest
}) => {
  const [t] = useTranslation("translation");
  return (
    <Button variant={variant} {...rest}>
      {children ?? (
        <>
          <X /> {t("cancel")}
        </>
      )}
    </Button>
  );
};

// Standard Save button: primary variant, Save icon, localized "Save" label.
// Same composition rules as CancelButton.
export const SaveButton: React.FC<ButtonProps> = ({
  children,
  variant = "primary",
  ...rest
}) => {
  const [t] = useTranslation("translation");
  return (
    <Button variant={variant} {...rest}>
      {children ?? (
        <>
          <Save /> {t("save")}
        </>
      )}
    </Button>
  );
};

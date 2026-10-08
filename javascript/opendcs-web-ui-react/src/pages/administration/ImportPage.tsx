import { useState } from "react";
import { Button, Card, Col, Row } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { DecodesImport } from "./DecodesImport";

export const ImportPage = () => {
  const [t] = useTranslation("administration");
  const [importType, setImportType] = useState<"decodes" | null>(null);

  if (importType === "decodes") {
    return <DecodesImport onBack={() => setImportType(null)} />;
  }

  return (
    <>
      <h1 className="h4 mb-3">{t("importTitle")}</h1>
      <Row xs={1} md={2} className="g-3">
        <Col>
          <Card className="h-100 position-relative">
            <Card.Body>
              <h2 className="h5">
                <Button
                  variant="link"
                  className="stretched-link p-0 text-start text-decoration-none fw-semibold"
                  onClick={() => setImportType("decodes")}
                >
                  {t("decodesImportTitle")}
                </Button>
              </h2>
              <p className="mb-0">{t("decodesImportDescription")}</p>
            </Card.Body>
          </Card>
        </Col>
        <Col>
          <Card className="h-100">
            <Card.Body>
              <h2 className="h5">{t("timeSeriesImportTitle")}</h2>
              <p>{t("timeSeriesImportDescription")}</p>
              <p className="text-body-secondary mb-0">
                {t("timeSeriesImportPlaceholder")}
              </p>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </>
  );
};

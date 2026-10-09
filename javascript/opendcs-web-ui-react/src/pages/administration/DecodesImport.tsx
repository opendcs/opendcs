import { useEffect, useRef, useState } from "react";
import { Alert, Button, Card, Form, Stack } from "react-bootstrap";
import { useTranslation } from "react-i18next";

export interface DecodesImportProps {
  onBack: () => void;
}

export const DecodesImport = ({ onBack }: DecodesImportProps) => {
  const [t] = useTranslation("administration");
  const [file, setFile] = useState<File | null>(null);
  const [previewed, setPreviewed] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [logs, setLogs] = useState<string[]>([]);
  const logRef = useRef<HTMLPreElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight;
    }
  }, [logs]);

  const selectFile = (files: File[], fromDrop = false) => {
    setPreviewed(false);
    setFile(null);
    setLogs([]);
    if (files.length !== 1 || !files[0].name.toLowerCase().endsWith(".xml")) {
      if (fileInputRef.current) fileInputRef.current.value = "";
      setError(t("decodesImport.invalidFile"));
      return;
    }
    if (fromDrop && fileInputRef.current) {
      const selection = new DataTransfer();
      selection.items.add(files[0]);
      fileInputRef.current.files = selection.files;
    }
    setError(null);
    setFile(files[0]);
    setLogs([t("decodesImport.selectedLog", { name: files[0].name })]);
  };

  // Demonstration feedback only: neither action reads the file or calls the API.
  const preview = () => {
    if (!file) return;
    setLogs((previous) => [
      ...previous,
      `[Sample preview] Starting preview of ${file.name}. File contents have not been read.`,
      "[Sample preview] Example contents: 3 platforms, 2 configurations, and 3 sites.",
      "[Sample preview] Example reference data: 1 reference list and 4 engineering units.",
      "[Sample preview] Example conflict: a platform with the same name already exists.",
      "[Sample preview] Preview complete. These are sample results, not findings from the selected file.",
    ]);
    setPreviewed(true);
  };

  const importFile = () => {
    if (!file || !previewed) return;
    setLogs((previous) => [
      ...previous,
      `[Sample import] Starting simulated import of ${file.name}.`,
      "[Sample import] Example progress: processing sites, configurations, platforms, and reference data.",
      "[Sample import] Simulation complete. No records were written to the database.",
    ]);
  };

  return (
    <>
      <Button variant="outline-secondary" className="mb-3" onClick={onBack}>
        {t("decodesImport.back")}
      </Button>
      <h1 className="h4 mb-3">{t("decodesImportTitle")}</h1>
      <Alert variant="info">
        Preview and Import currently show sample feedback. The selected file is not
        parsed or uploaded, and no database changes are made.
      </Alert>
      <Card className="mb-3">
        <Card.Body>
          <div
            className={`border border-2 rounded p-4 text-center mb-3 ${dragActive ? "border-primary bg-primary-subtle" : "border-secondary-subtle"}`}
            onDragOver={(event) => {
              event.preventDefault();
              event.dataTransfer.dropEffect = "copy";
              setDragActive(true);
            }}
            onDragLeave={() => setDragActive(false)}
            onDrop={(event) => {
              event.preventDefault();
              setDragActive(false);
              selectFile(Array.from(event.dataTransfer.files), true);
            }}
          >
            <p className="mb-1 fw-semibold">{t("decodesImport.dropFile")}</p>
            <p className="text-body-secondary mb-0">{t("decodesImport.fileHint")}</p>
          </div>
          <Form.Group controlId="decodes-import-file">
            <Form.Label>{t("decodesImport.chooseFile")}</Form.Label>
            <Form.Control
              ref={fileInputRef}
              type="file"
              accept=".xml"
              onChange={(event) => {
                const input = event.currentTarget as HTMLInputElement;
                if (input.files?.length) selectFile(Array.from(input.files));
              }}
            />
          </Form.Group>
          {error && (
            <Alert variant="danger" className="mt-3 mb-0">
              {error}
            </Alert>
          )}
          {file && (
            <p className="mt-3 mb-0 text-break">
              {t("decodesImport.selectedFile", {
                name: file.name,
                size: file.size.toLocaleString(),
              })}
            </p>
          )}
          <Stack direction="horizontal" gap={2} className="mt-3">
            <Button variant="outline-primary" disabled={!file} onClick={preview}>
              {t("decodesImport.preview")}
            </Button>
            <Button disabled={!file || !previewed} onClick={importFile}>
              {t("decodesImport.import")}
            </Button>
          </Stack>
          <p className="text-body-secondary mt-2 mb-0">
            {t("decodesImport.previewHint")}
          </p>
        </Card.Body>
      </Card>
      <Card>
        <Card.Body>
          <Stack direction="horizontal" className="justify-content-between mb-2">
            <h2 className="h5 mb-0">{t("decodesImport.logTitle")}</h2>
            <Button
              variant="outline-secondary"
              size="sm"
              disabled={!logs.length}
              onClick={() => setLogs([])}
            >
              {t("decodesImport.clearLog")}
            </Button>
          </Stack>
          <pre
            ref={logRef}
            role="log"
            aria-label={t("decodesImport.logTitle")}
            aria-live="polite"
            tabIndex={0}
            className="bg-body-tertiary border rounded p-3 mb-0 overflow-auto"
            style={{
              height: "40vh",
              minHeight: 240,
              maxHeight: 560,
              whiteSpace: "pre-wrap",
              overflowWrap: "anywhere",
            }}
          >
            {logs.length ? logs.join("\n") : t("decodesImport.emptyLog")}
          </pre>
        </Card.Body>
      </Card>
    </>
  );
};

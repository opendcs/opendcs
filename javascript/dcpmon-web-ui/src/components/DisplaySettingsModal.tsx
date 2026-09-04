import { useState } from "react";
import Button from "react-bootstrap/Button";
import Form from "react-bootstrap/Form";
import Modal from "react-bootstrap/Modal";
import {
  DEFAULT_DISPLAY_SETTINGS,
  isValidTimeZone,
  supportedTimeZones,
  type DisplaySettings,
} from "../displaySettings";

type DisplaySettingsModalProps = {
  settings: DisplaySettings;
  onHide: () => void;
  onSave: (settings: DisplaySettings) => void;
};

const timeZones = supportedTimeZones();

export function DisplaySettingsModal({
  settings,
  onHide,
  onSave,
}: DisplaySettingsModalProps) {
  const [draft, setDraft] = useState(settings);

  const timeZoneIsValid = isValidTimeZone(draft.timeZone);

  return (
    <Modal
      show
      onHide={onHide}
      centered
      dialogClassName="dcpmon-settings-dialog"
      aria-labelledby="dcpmon-display-settings-title"
    >
      <Modal.Header closeButton>
        <Modal.Title id="dcpmon-display-settings-title">
          Display settings
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Form>
          <Form.Group className="mb-4" controlId="dcpmon-theme">
            <Form.Label className="fw-semibold">Appearance</Form.Label>
            <Form.Select
              value={draft.theme}
              onChange={(event) =>
                setDraft((current) => ({
                  ...current,
                  theme:
                    event.target.value === "light" ||
                    event.target.value === "dark"
                      ? event.target.value
                      : "system",
                }))
              }
            >
              <option value="system">System default</option>
              <option value="light">Light</option>
              <option value="dark">Dark</option>
            </Form.Select>
            <Form.Text>
              System default follows your device’s light or dark appearance.
            </Form.Text>
          </Form.Group>

          <Form.Group className="mb-4" controlId="dcpmon-time-zone">
            <Form.Label className="fw-semibold">Display timezone</Form.Label>
            <Form.Control
              type="text"
              list="dcpmon-time-zones"
              value={draft.timeZone}
              onChange={(event) =>
                setDraft((current) => ({
                  ...current,
                  timeZone: event.target.value,
                }))
              }
              isInvalid={!timeZoneIsValid}
              autoComplete="off"
              placeholder="Start typing a timezone"
            />
            <datalist id="dcpmon-time-zones">
              {timeZones.map((timeZone) => (
                <option key={timeZone} value={timeZone} />
              ))}
            </datalist>
            <Form.Text>
              Start typing, then select GMT or an IANA timezone such as
              America/Chicago.
            </Form.Text>
            <Form.Control.Feedback type="invalid">
              Select a recognized timezone.
            </Form.Control.Feedback>
          </Form.Group>

          <Form.Group className="mb-4" controlId="dcpmon-hour-format">
            <Form.Label className="fw-semibold">Time format</Form.Label>
            <Form.Select
              value={draft.hourFormat}
              onChange={(event) =>
                setDraft((current) => ({
                  ...current,
                  hourFormat: event.target.value === "24" ? "24" : "12",
                }))
              }
            >
              <option value="12">12-hour (3:30 PM)</option>
              <option value="24">24-hour (15:30)</option>
            </Form.Select>
          </Form.Group>

          <Form.Check
            id="dcpmon-show-seconds"
            type="switch"
            label="Show seconds"
            checked={draft.showSeconds}
            onChange={(event) =>
              setDraft((current) => ({
                ...current,
                showSeconds: event.target.checked,
              }))
            }
          />
        </Form>
      </Modal.Body>
      <Modal.Footer className="dcpmon-settings-footer justify-content-between">
        <Button
          variant="link"
          className="px-0"
          onClick={() => setDraft(DEFAULT_DISPLAY_SETTINGS)}
        >
          Reset defaults
        </Button>
        <div className="dcpmon-settings-actions d-flex gap-2">
          <Button variant="outline-secondary" onClick={onHide}>
            Cancel
          </Button>
          <Button
            variant="primary"
            disabled={!timeZoneIsValid}
            onClick={() => onSave(draft)}
          >
            Save settings
          </Button>
        </div>
      </Modal.Footer>
    </Modal>
  );
}

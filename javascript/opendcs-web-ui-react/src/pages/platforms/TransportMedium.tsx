import { useCallback, useReducer } from "react";
import {
  Button,
  Card,
  Col,
  Form,
  FormCheck,
  FormGroup,
  Placeholder,
  Row,
} from "react-bootstrap";
import { Save, X } from "react-bootstrap-icons";
import { useTranslation } from "react-i18next";
import type { ApiTransportMedium } from "opendcs-api";
import { DetailFade } from "../../components/data-table";
import type { CancelAction } from "../../util/Actions";
import { TransportMediumReducer } from "./TransportMediumReducer";

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };

const MEDIUM_TYPES = [
  "GOES",
  "GOES-SELFTIMED",
  "GOES-RANDOM",
  "IRIDIUM",
  "POLLED-MODEM",
  "POLLED-TCP",
  "INCOMING-TCP",
  "DATA-LOGGER",
  "OTHER",
] as const;

const BAUD_RATES = [300, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200];
const PARITY_OPTIONS = ["none", "even", "odd"] as const;
const DATA_BITS_OPTIONS = [7, 8];
const STOP_BITS_OPTIONS = [1, 2];

const SKELETON_FIELDS = [
  "mediumType",
  "mediumId",
  "scriptName",
  "channelNum",
  "assignedTime",
  "transportWindow",
  "transportInterval",
  "timeAdjustment",
  "timezone",
  "loggerType",
] as const;

export type UiTransportMedium = Partial<ApiTransportMedium>;

export const TransportMediumSkeleton: React.FC<{
  edit?: boolean;
  className?: string;
}> = ({ edit = false, className }) => (
  <Card
    className={[
      "transport-medium-card",
      edit ? "transport-medium-card--edit" : null,
      className,
    ]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      <Row>
        <Col>
          {SKELETON_FIELDS.slice(0, 5).map((field) => (
            <Row key={field} className="mb-3 align-items-center">
              <Placeholder as={Col} sm={4} animation="glow">
                <Placeholder xs={10} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} sm={8} animation="glow">
                <Placeholder xs={12} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Row>
          ))}
        </Col>
        <Col>
          {SKELETON_FIELDS.slice(5).map((field) => (
            <Row key={field} className="mb-3 align-items-center">
              <Placeholder as={Col} sm={4} animation="glow">
                <Placeholder xs={10} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} sm={8} animation="glow">
                <Placeholder xs={12} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Row>
          ))}
        </Col>
      </Row>
      {edit && (
        <Row className="mt-3">
          <Col className="d-flex justify-content-end gap-2">
            <Placeholder animation="glow">
              <Placeholder
                className="rounded"
                style={{ ...INPUT_H, width: "5.5rem" }}
              />
            </Placeholder>
            <Placeholder animation="glow">
              <Placeholder
                className="rounded"
                style={{ ...INPUT_H, width: "4.5rem" }}
              />
            </Placeholder>
          </Col>
        </Row>
      )}
    </Card.Body>
  </Card>
);

export interface TransportMediumProperties {
  medium: UiTransportMedium;
  actions?: {
    save?: (medium: ApiTransportMedium, originalKey?: string) => void;
  } & CancelAction<string>;
  edit?: boolean;
  /** Identity of the row before edit — used to locate the original record in the parent platform array if mediumType/mediumId change. */
  originalKey?: string;
}

const toNumberOrUndefined = (value: string): number | undefined => {
  if (value === "") return undefined;
  const n = Number(value);
  return Number.isNaN(n) ? undefined : n;
};

export const TransportMedium: React.FC<TransportMediumProperties> = ({
  medium,
  actions = {},
  edit = false,
  originalKey,
}) => {
  const [t] = useTranslation(["platforms", "translation"]);
  const [local, dispatch] = useReducer(TransportMediumReducer, medium);

  const textChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    dispatch({ type: "save", payload: { [name]: value } });
  }, []);

  const selectChange = useCallback((event: React.ChangeEvent<HTMLSelectElement>) => {
    const { name, value } = event.target;
    dispatch({ type: "save", payload: { [name]: value } });
  }, []);

  const numberChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    dispatch({
      type: "save",
      payload: { [name]: toNumberOrUndefined(value) },
    });
  }, []);

  const numberSelectChange = useCallback(
    (event: React.ChangeEvent<HTMLSelectElement>) => {
      const { name, value } = event.target;
      dispatch({
        type: "save",
        payload: { [name]: toNumberOrUndefined(value) },
      });
    },
    [],
  );

  const checkboxChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, checked } = event.target;
    dispatch({ type: "save", payload: { [name]: checked } });
  }, []);

  const save = useCallback(() => {
    actions.save?.(local as ApiTransportMedium, originalKey);
  }, [actions, local, originalKey]);

  const cancel = useCallback(() => {
    if (originalKey !== undefined) actions.cancel?.(originalKey);
  }, [actions, originalKey]);

  return (
    <DetailFade skeleton={<TransportMediumSkeleton edit={edit} />}>
      <Card
        className={[
          "transport-medium-card",
          edit ? "transport-medium-card--edit" : null,
        ]
          .filter(Boolean)
          .join(" ")}
      >
        <Card.Body>
          <Form
            autoComplete="off"
            data-form-type="other"
            onSubmit={(e) => e.preventDefault()}
          >
            <Row>
              <Col>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="mediumType">
                    {t("platforms:medium_type")}
                  </Form.Label>
                  <Col sm={8}>
                    {edit ? (
                      <Form.Select
                        id="mediumType"
                        name="mediumType"
                        value={local.mediumType ?? "GOES"}
                        onChange={selectChange}
                      >
                        {MEDIUM_TYPES.map((opt) => (
                          <option key={opt} value={opt}>
                            {opt}
                          </option>
                        ))}
                      </Form.Select>
                    ) : (
                      <Form.Control
                        type="text"
                        id="mediumType"
                        name="mediumType"
                        readOnly={true}
                        value={local.mediumType ?? ""}
                      />
                    )}
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="mediumId">
                    {t("platforms:medium_id")}
                  </Form.Label>
                  <Col sm={8}>
                    <Form.Control
                      type="text"
                      id="mediumId"
                      name="mediumId"
                      readOnly={!edit}
                      defaultValue={local.mediumId ?? ""}
                      onChange={textChange}
                    />
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="scriptName">
                    {t("platforms:script_name")}
                  </Form.Label>
                  <Col sm={8}>
                    <Form.Control
                      type="text"
                      id="scriptName"
                      name="scriptName"
                      readOnly={!edit}
                      defaultValue={local.scriptName ?? ""}
                      onChange={textChange}
                    />
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="channelNum">
                    {t("platforms:channel_num")}
                  </Form.Label>
                  <Col sm={8}>
                    <Form.Control
                      type="number"
                      id="channelNum"
                      name="channelNum"
                      readOnly={!edit}
                      defaultValue={local.channelNum ?? ""}
                      onChange={numberChange}
                    />
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="assignedTime">
                    {t("platforms:assigned_time")}
                  </Form.Label>
                  <Col sm={8}>
                    <Form.Control
                      type="number"
                      id="assignedTime"
                      name="assignedTime"
                      readOnly={!edit}
                      defaultValue={local.assignedTime ?? ""}
                      onChange={numberChange}
                    />
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="transportWindow">
                    {t("platforms:transport_window")}
                  </Form.Label>
                  <Col sm={8}>
                    <Form.Control
                      type="number"
                      id="transportWindow"
                      name="transportWindow"
                      readOnly={!edit}
                      defaultValue={local.transportWindow ?? ""}
                      onChange={numberChange}
                    />
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="transportInterval">
                    {t("platforms:transport_interval")}
                  </Form.Label>
                  <Col sm={8}>
                    <Form.Control
                      type="number"
                      id="transportInterval"
                      name="transportInterval"
                      readOnly={!edit}
                      defaultValue={local.transportInterval ?? ""}
                      onChange={numberChange}
                    />
                  </Col>
                </FormGroup>
              </Col>
              <Col>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="timeAdjustment">
                    {t("platforms:time_adjustment")}
                  </Form.Label>
                  <Col sm={8}>
                    <Form.Control
                      type="number"
                      id="timeAdjustment"
                      name="timeAdjustment"
                      readOnly={!edit}
                      defaultValue={local.timeAdjustment ?? ""}
                      onChange={numberChange}
                    />
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="timezone">
                    {t("platforms:timezone")}
                  </Form.Label>
                  <Col sm={8}>
                    <Form.Control
                      type="text"
                      id="timezone"
                      name="timezone"
                      readOnly={!edit}
                      defaultValue={local.timezone ?? ""}
                      onChange={textChange}
                    />
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="loggerType">
                    {t("platforms:logger_type")}
                  </Form.Label>
                  <Col sm={8}>
                    <Form.Control
                      type="text"
                      id="loggerType"
                      name="loggerType"
                      readOnly={!edit}
                      defaultValue={local.loggerType ?? ""}
                      onChange={textChange}
                    />
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="baud">
                    {t("platforms:baud")}
                  </Form.Label>
                  <Col sm={8}>
                    {edit ? (
                      <Form.Select
                        id="baud"
                        name="baud"
                        value={local.baud ?? ""}
                        onChange={numberSelectChange}
                      >
                        <option value="">—</option>
                        {BAUD_RATES.map((rate) => (
                          <option key={rate} value={rate}>
                            {rate}
                          </option>
                        ))}
                      </Form.Select>
                    ) : (
                      <Form.Control type="text" readOnly value={local.baud ?? ""} />
                    )}
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="dataBits">
                    {t("platforms:data_bits")}
                  </Form.Label>
                  <Col sm={8}>
                    {edit ? (
                      <Form.Select
                        id="dataBits"
                        name="dataBits"
                        value={local.dataBits ?? ""}
                        onChange={numberSelectChange}
                      >
                        <option value="">—</option>
                        {DATA_BITS_OPTIONS.map((bits) => (
                          <option key={bits} value={bits}>
                            {bits}
                          </option>
                        ))}
                      </Form.Select>
                    ) : (
                      <Form.Control type="text" readOnly value={local.dataBits ?? ""} />
                    )}
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="stopBits">
                    {t("platforms:stop_bits")}
                  </Form.Label>
                  <Col sm={8}>
                    {edit ? (
                      <Form.Select
                        id="stopBits"
                        name="stopBits"
                        value={local.stopBits ?? ""}
                        onChange={numberSelectChange}
                      >
                        <option value="">—</option>
                        {STOP_BITS_OPTIONS.map((bits) => (
                          <option key={bits} value={bits}>
                            {bits}
                          </option>
                        ))}
                      </Form.Select>
                    ) : (
                      <Form.Control type="text" readOnly value={local.stopBits ?? ""} />
                    )}
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="parity">
                    {t("platforms:parity")}
                  </Form.Label>
                  <Col sm={8}>
                    {edit ? (
                      <Form.Select
                        id="parity"
                        name="parity"
                        value={local.parity ?? ""}
                        onChange={selectChange}
                      >
                        <option value="">—</option>
                        {PARITY_OPTIONS.map((opt) => (
                          <option key={opt} value={opt}>
                            {opt}
                          </option>
                        ))}
                      </Form.Select>
                    ) : (
                      <Form.Control type="text" readOnly value={local.parity ?? ""} />
                    )}
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="doLogin">
                    {t("platforms:do_login")}
                  </Form.Label>
                  <Col sm={8} className="d-flex align-items-center">
                    <FormCheck
                      type="switch"
                      id="doLogin"
                      name="doLogin"
                      disabled={!edit}
                      defaultChecked={local.doLogin ?? false}
                      onChange={checkboxChange}
                      style={{ fontSize: "1.5rem" }}
                    />
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="tm-loggerUser">
                    {t("platforms:tm_username")}
                  </Form.Label>
                  <Col sm={8}>
                    <Form.Control
                      type="text"
                      id="tm-loggerUser"
                      name="username"
                      readOnly={!edit}
                      autoComplete="off"
                      data-1p-ignore=""
                      data-lpignore="true"
                      data-form-type="other"
                      defaultValue={local.username ?? ""}
                      onChange={textChange}
                    />
                  </Col>
                </FormGroup>
                <FormGroup as={Row} className="mb-3">
                  <Form.Label column sm={4} htmlFor="tm-loggerSecret">
                    {t("platforms:tm_password")}
                  </Form.Label>
                  <Col sm={8}>
                    <Form.Control
                      type="password"
                      id="tm-loggerSecret"
                      name="password"
                      readOnly={!edit}
                      autoComplete="new-password"
                      data-1p-ignore=""
                      data-lpignore="true"
                      data-form-type="other"
                      defaultValue={local.password ?? ""}
                      onChange={textChange}
                    />
                  </Col>
                </FormGroup>
              </Col>
            </Row>
            {edit && (
              <Row className="mt-3">
                <Col className="d-flex justify-content-end gap-2">
                  <Button
                    onClick={cancel}
                    variant="secondary"
                    aria-label={t("platforms:cancel_transport_for", {
                      id: medium.mediumId ?? "",
                    })}
                  >
                    <X /> {t("translation:cancel")}
                  </Button>
                  <Button
                    onClick={save}
                    variant="primary"
                    aria-label={t("platforms:save_transport", {
                      id: medium.mediumId ?? "",
                    })}
                  >
                    <Save /> {t("translation:save")}
                  </Button>
                </Col>
              </Row>
            )}
          </Form>
        </Card.Body>
      </Card>
    </DetailFade>
  );
};

export default TransportMedium;

import { use, useCallback, useMemo, useReducer, useState } from "react";
import {
  Button,
  Card,
  Col,
  Form,
  FormGroup,
  InputGroup,
  Placeholder,
  Row,
} from "react-bootstrap";
import { Save, X } from "react-bootstrap-icons";
import { useTranslation } from "react-i18next";
import type { ApiAppRef, ApiRoutingRef, ApiScheduleEntry } from "opendcs-api";
import { DetailFade } from "../../components/data-table";
import type { CancelAction, SaveAction } from "../../util/Actions";
import { ExecutionSchedule } from "./ExecutionSchedule";
import { RoutingSpecSelectModal } from "./RoutingSpecSelectModal";
import { ScheduleReducer, type UiSchedule } from "./ScheduleReducer";

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };

const SKELETON_FIELDS = ["name", "appName", "routingSpecName", "enabled"] as const;

export interface ScheduleSkeletonProps {
  edit?: boolean;
  className?: string;
}

export const ScheduleSkeleton: React.FC<ScheduleSkeletonProps> = ({
  edit = false,
  className,
}) => (
  <Card
    className={["schedule-card", edit ? "schedule-card--edit" : null, className]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      <Row>
        <Col md={6}>
          {SKELETON_FIELDS.map((field) => (
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
        <Col md={6}>
          <Placeholder animation="glow" className="d-block">
            <Placeholder xs={12} style={{ height: "16rem" }} />
          </Placeholder>
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

export interface ScheduleDetails {
  schedule: UiSchedule;
}

export interface ScheduleProperties {
  details: Promise<ScheduleDetails> | ScheduleDetails;
  apps: ApiAppRef[];
  routings: ApiRoutingRef[];
  routingsLoading?: boolean;
  actions?: SaveAction<ApiScheduleEntry> & CancelAction<number>;
  edit?: boolean;
}

export const Schedule: React.FC<ScheduleProperties> = ({
  details,
  apps,
  routings,
  routingsLoading = false,
  actions = {},
  edit = false,
}) => {
  const [t] = useTranslation(["schedule", "translation"]);
  const resolved = details instanceof Promise ? use(details) : details;
  const provided = resolved.schedule;
  const [local, dispatch] = useReducer(ScheduleReducer, provided);
  const [showRoutingModal, setShowRoutingModal] = useState(false);

  const appOptions = useMemo(
    () =>
      apps
        .map((a) => a.appName ?? "")
        .filter(Boolean)
        .sort((a, b) => a.localeCompare(b)),
    [apps],
  );

  const textChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    dispatch({ type: "save", payload: { [name]: value } });
  }, []);

  const setField = useCallback(
    <K extends keyof UiSchedule>(name: K, value: UiSchedule[K]) =>
      dispatch({ type: "save", payload: { [name]: value } as UiSchedule }),
    [],
  );

  const onEnabledChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) =>
      setField("enabled", event.target.checked),
    [setField],
  );

  const onExecutionChange = useCallback(
    ({
      startTime,
      runInterval,
    }: {
      startTime: Date | undefined;
      runInterval: string | undefined;
    }) =>
      dispatch({
        type: "save",
        payload: { startTime, runInterval },
      }),
    [],
  );

  const onSelectRouting = useCallback((ref: ApiRoutingRef) => {
    dispatch({
      type: "save",
      payload: {
        routingSpecId: ref.routingId,
        routingSpecName: ref.name,
      },
    });
  }, []);

  const saveSchedule = useCallback(() => {
    // The appName select is by name; resolve back to the API's appId before
    // posting. The routing spec is already chosen via the modal which gives us
    // both id and name on the local record.
    const appRef = apps.find((a) => a.appName === local.appName);
    actions.save?.({
      ...local,
      appId: appRef?.appId,
      appName: appRef?.appName,
    } as ApiScheduleEntry);
  }, [actions, local, apps]);

  const cancel = useCallback(() => {
    if (provided.schedEntryId !== undefined) actions.cancel?.(provided.schedEntryId);
  }, [actions, provided.schedEntryId]);

  return (
    <DetailFade skeleton={<ScheduleSkeleton edit={edit} />}>
      <Card
        className={["schedule-card", edit ? "schedule-card--edit" : null]
          .filter(Boolean)
          .join(" ")}
      >
        <Card.Body>
          <Row>
            <Col md={6}>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="name">
                  {t("schedule:name")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="text"
                    id="name"
                    name="name"
                    readOnly={!edit}
                    defaultValue={local.name ?? ""}
                    onChange={textChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="appName">
                  {t("schedule:loading_app")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Select
                    id="appName"
                    aria-label={t("schedule:loading_app")}
                    value={local.appName ?? ""}
                    disabled={!edit}
                    onChange={(e) => setField("appName", e.currentTarget.value)}
                  >
                    <option value="" />
                    {appOptions.map((name) => (
                      <option key={name} value={name}>
                        {name}
                      </option>
                    ))}
                  </Form.Select>
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="routingSpecName">
                  {t("schedule:routing_spec")}
                </Form.Label>
                <Col sm={8}>
                  {edit ? (
                    <InputGroup>
                      <Form.Control
                        type="text"
                        id="routingSpecName"
                        name="routingSpecName"
                        readOnly={true}
                        disabled={true}
                        value={local.routingSpecName ?? ""}
                      />
                      <Button
                        variant="secondary"
                        className="dt-button"
                        aria-label={t("schedule:select_routing")}
                        onClick={() => setShowRoutingModal(true)}
                      >
                        {t("translation:choose")}
                      </Button>
                    </InputGroup>
                  ) : (
                    <Form.Control
                      type="text"
                      id="routingSpecName"
                      readOnly={true}
                      value={local.routingSpecName ?? ""}
                    />
                  )}
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3 align-items-center">
                <Form.Label column sm={4} htmlFor="enabled">
                  {t("schedule:enabled")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Check
                    id="enabled"
                    name="enabled"
                    disabled={!edit}
                    checked={local.enabled ?? false}
                    onChange={onEnabledChange}
                  />
                </Col>
              </FormGroup>
            </Col>
            <Col md={6}>
              <ExecutionSchedule
                startTime={local.startTime}
                runInterval={local.runInterval}
                timeZone={local.timeZone}
                edit={edit}
                onChange={onExecutionChange}
                onTimeZoneChange={(v) => setField("timeZone", v)}
              />
            </Col>
          </Row>

          {edit && (
            <Row className="mt-3">
              <Col className="d-flex justify-content-end gap-2">
                <Button
                  onClick={cancel}
                  variant="secondary"
                  aria-label={t("schedule:cancel_for", {
                    id: provided.schedEntryId,
                  })}
                >
                  <X /> {t("translation:cancel")}
                </Button>
                <Button
                  onClick={saveSchedule}
                  variant="primary"
                  aria-label={t("schedule:save_schedule", {
                    id: provided.schedEntryId,
                  })}
                >
                  <Save /> {t("translation:save")}
                </Button>
              </Col>
            </Row>
          )}
        </Card.Body>
      </Card>
      <RoutingSpecSelectModal
        show={showRoutingModal}
        onHide={() => setShowRoutingModal(false)}
        onSelect={(r) => {
          onSelectRouting(r);
          setShowRoutingModal(false);
        }}
        routings={routings}
        loading={routingsLoading}
      />
    </DetailFade>
  );
};

export default Schedule;

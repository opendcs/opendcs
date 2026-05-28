import { use, useCallback, useMemo, useReducer } from "react";
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
import type { ApiNetlistRef, ApiPlatformRef, ApiRouting } from "opendcs-api";
import { DetailFade } from "../../../components/data-table";
import { PropertiesTable, type Property } from "../../../components/properties";
import type {
  CancelAction,
  CollectionActions,
  SaveAction,
} from "../../../util/Actions";
import { RoutingReducer, type UiRouting } from "./RoutingReducer";
import RoutingPlatformsTable from "./RoutingPlatformsTable";
import RoutingNetlistsTable from "./RoutingNetlistsTable";
import { SinceUntilEditor } from "./SinceUntilEditor";
import {
  DataSourceSelect,
  PresentationGroupSelect,
  RefListSelect,
  TimezoneSelect,
} from "./RoutingSelects";

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };

const ROUTING_FIELDS = [
  "name",
  "dataSourceName",
  "destinationType",
  "destinationArg",
  "outputFormat",
  "outputTZ",
  "presGroupName",
] as const;

const APPLY_TIME_TO_OPTIONS = [
  "Local Receive Time",
  "Platform Xmit Time",
  "Both",
] as const;

const TablePlaceholder: React.FC = () => (
  <>
    <Placeholder animation="glow" className="d-block mb-2">
      <Placeholder xs={12} className="rounded" style={{ height: "2rem" }} />
    </Placeholder>
    <Placeholder animation="glow" className="d-block">
      <Placeholder xs={12} className="rounded" style={{ height: "8rem" }} />
    </Placeholder>
  </>
);

export const RoutingSkeleton: React.FC<{ edit?: boolean; className?: string }> = ({
  edit = false,
  className,
}) => (
  <Card
    className={["routing-card", edit ? "routing-card--edit" : null, className]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      <Row>
        <Col>
          {ROUTING_FIELDS.map((field) => (
            <Row key={field} className="mb-3 align-items-center">
              <Placeholder as={Col} sm={3} animation="glow">
                <Placeholder xs={10} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} sm={9} animation="glow">
                <Placeholder xs={12} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Row>
          ))}
        </Col>
        <Col>
          <Placeholder animation="glow" className="d-block">
            <Placeholder xs={12} style={{ height: "12rem" }} />
          </Placeholder>
        </Col>
      </Row>
      <Row className="mt-4">
        <Col md={6}>
          <TablePlaceholder />
        </Col>
        <Col md={6}>
          <TablePlaceholder />
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

export interface RoutingDetails {
  routing: UiRouting;
}

export interface RoutingProperties {
  details: Promise<RoutingDetails> | RoutingDetails;
  /** All platforms available to attach. Sourced from the platforms query. */
  platforms: ApiPlatformRef[];
  platformsLoading?: boolean;
  /** All network lists available to attach. Sourced from the netlists query. */
  netlists: ApiNetlistRef[];
  netlistsLoading?: boolean;
  actions?: SaveAction<ApiRouting> & CancelAction<number>;
  edit?: boolean;
}

const mapProps: (props: { [k: string]: string }) => Property[] = (props) =>
  Object.entries(props).map(([name, value]): Property => ({ name, value }));

export const Routing: React.FC<RoutingProperties> = ({
  details,
  platforms,
  platformsLoading = false,
  netlists,
  netlistsLoading = false,
  actions = {},
  edit = false,
}) => {
  const [t] = useTranslation(["routing", "translation"]);
  const resolved = details instanceof Promise ? use(details) : details;
  const provided = resolved.routing;
  const [local, dispatch] = useReducer(RoutingReducer, provided);

  const propsList = useMemo(() => mapProps(local.properties ?? {}), [local.properties]);

  const propertyActions: CollectionActions<Property, string> = edit
    ? {
        remove: (name) => dispatch({ type: "delete_prop", payload: { name } }),
        save: (prop) =>
          dispatch({
            type: "save_prop",
            payload: { name: prop.name, value: prop.value },
          }),
      }
    : {};

  const setField = useCallback(
    (name: keyof UiRouting, value: string | boolean) =>
      dispatch({ type: "save", payload: { [name]: value } }),
    [],
  );

  const textChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    dispatch({ type: "save", payload: { [name]: value } });
  }, []);

  const saveRouting = useCallback(() => {
    actions.save?.(local as ApiRouting);
  }, [actions, local]);

  const cancel = useCallback(() => {
    if (provided.routingId !== undefined) actions.cancel?.(provided.routingId);
  }, [actions, provided.routingId]);

  const onAddPlatformNames = useCallback(
    (names: string[]) => dispatch({ type: "add_platform_names", payload: { names } }),
    [],
  );
  const onRemovePlatformName = useCallback(
    (name: string) => dispatch({ type: "remove_platform_name", payload: { name } }),
    [],
  );
  const onRemovePlatformId = useCallback(
    (id: string) => dispatch({ type: "remove_platform_id", payload: { id } }),
    [],
  );
  const onAddNetlists = useCallback(
    (netlistNames: string[]) =>
      dispatch({ type: "add_netlists", payload: { netlistNames } }),
    [],
  );
  const onRemoveNetlist = useCallback(
    (netlistName: string) =>
      dispatch({ type: "remove_netlist", payload: { netlistName } }),
    [],
  );

  const flagCheck = (name: keyof UiRouting, label: string, extra?: React.ReactNode) => (
    <div className="d-flex align-items-center gap-2 mb-2">
      <FormCheck
        type="checkbox"
        id={`routing-${String(name)}`}
        label={label}
        disabled={!edit}
        checked={Boolean(local[name])}
        onChange={(e) => setField(name, e.currentTarget.checked)}
      />
      {extra}
    </div>
  );

  return (
    <DetailFade skeleton={<RoutingSkeleton edit={edit} />}>
      <Card
        className={["routing-card", edit ? "routing-card--edit" : null]
          .filter(Boolean)
          .join(" ")}
      >
        <Card.Body>
          <Row>
            <Col md={6}>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="name">
                  {t("routing:name")}
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
                <Form.Label column sm={4} htmlFor="dataSourceName">
                  {t("routing:data_source")}
                </Form.Label>
                <Col sm={8}>
                  <DataSourceSelect
                    id="dataSourceName"
                    value={local.dataSourceName}
                    edit={edit}
                    ariaLabel={t("routing:data_source")}
                    onChange={(v) => setField("dataSourceName", v)}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="destinationType">
                  {t("routing:consumer_type")}
                </Form.Label>
                <Col sm={8}>
                  <RefListSelect
                    refListName="DataConsumer"
                    id="destinationType"
                    value={local.destinationType}
                    edit={edit}
                    ariaLabel={t("routing:consumer_type")}
                    onChange={(v) => setField("destinationType", v)}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="destinationArg">
                  {t("routing:consumer_arg")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="text"
                    id="destinationArg"
                    name="destinationArg"
                    readOnly={!edit}
                    defaultValue={local.destinationArg ?? ""}
                    onChange={textChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="outputFormat">
                  {t("routing:output_format")}
                </Form.Label>
                <Col sm={8}>
                  <RefListSelect
                    refListName="OutputFormat"
                    id="outputFormat"
                    value={local.outputFormat}
                    edit={edit}
                    ariaLabel={t("routing:output_format")}
                    onChange={(v) => setField("outputFormat", v)}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="outputTZ">
                  {t("routing:output_tz")}
                </Form.Label>
                <Col sm={8}>
                  <TimezoneSelect
                    id="outputTZ"
                    value={local.outputTZ}
                    edit={edit}
                    ariaLabel={t("routing:output_tz")}
                    onChange={(v) => setField("outputTZ", v)}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="presGroupName">
                  {t("routing:pres_group")}
                </Form.Label>
                <Col sm={8}>
                  <PresentationGroupSelect
                    id="presGroupName"
                    value={local.presGroupName}
                    edit={edit}
                    ariaLabel={t("routing:pres_group")}
                    onChange={(v) => setField("presGroupName", v)}
                  />
                </Col>
              </FormGroup>
              <Row className="mb-2">
                <Col sm={{ span: 8, offset: 4 }} className="d-flex gap-4">
                  <FormCheck
                    type="checkbox"
                    id="enableEquations"
                    label={t("routing:enable_equations")}
                    disabled={!edit}
                    checked={Boolean(local.enableEquations)}
                    onChange={(e) =>
                      setField("enableEquations", e.currentTarget.checked)
                    }
                  />
                  <FormCheck
                    type="checkbox"
                    id="production"
                    label={t("routing:production")}
                    disabled={!edit}
                    checked={Boolean(local.production)}
                    onChange={(e) => setField("production", e.currentTarget.checked)}
                  />
                </Col>
              </Row>
            </Col>
            <Col md={6}>
              <PropertiesTable
                theProps={propsList}
                actions={propertyActions}
                edit={edit}
                canAdd={true}
                width={"100%"}
                height={"auto"}
              />
            </Col>
          </Row>

          <hr />
          <h6 className="mb-3">{t("routing:search_criteria")}</h6>

          <Row>
            <Col md={6}>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4}>
                  {t("routing:since")}
                </Form.Label>
                <Col sm={8}>
                  <SinceUntilEditor
                    kind="since"
                    value={local.since}
                    edit={edit}
                    onChange={(v) => setField("since", v)}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4}>
                  {t("routing:until")}
                </Form.Label>
                <Col sm={8}>
                  <SinceUntilEditor
                    kind="until"
                    value={local.until}
                    edit={edit}
                    onChange={(v) => setField("until", v)}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="applyTimeTo">
                  {t("routing:apply_time_to")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Select
                    id="applyTimeTo"
                    aria-label={t("routing:apply_time_to")}
                    value={local.applyTimeTo ?? APPLY_TIME_TO_OPTIONS[0]}
                    disabled={!edit}
                    onChange={(e) => setField("applyTimeTo", e.currentTarget.value)}
                  >
                    {APPLY_TIME_TO_OPTIONS.map((opt) => (
                      <option key={opt} value={opt}>
                        {opt}
                      </option>
                    ))}
                  </Form.Select>
                </Col>
              </FormGroup>
            </Col>
            <Col md={6}>
              {flagCheck("ascendingTime", t("routing:ascending_time"))}
              {flagCheck("settlingTimeDelay", t("routing:settling_delay"))}
              {flagCheck("goesSelfTimed", t("routing:goes_self_timed"))}
              {flagCheck("goesRandom", t("routing:goes_random"))}
              {flagCheck("networkDCP", t("routing:network_dcp"))}
              {flagCheck("iridium", t("routing:iridium"))}
              {flagCheck("qualityNotifications", t("routing:quality_notifications"))}
              {flagCheck(
                "goesSpacecraftCheck",
                t("routing:spacecraft"),
                <Form.Select
                  size="sm"
                  aria-label={t("routing:spacecraft")}
                  style={{ maxWidth: "7rem" }}
                  value={local.goesSpacecraftSelection ?? "East"}
                  disabled={!edit || !local.goesSpacecraftCheck}
                  onChange={(e) =>
                    setField("goesSpacecraftSelection", e.currentTarget.value)
                  }
                >
                  <option value="East">{t("routing:east")}</option>
                  <option value="West">{t("routing:west")}</option>
                </Form.Select>,
              )}
              {flagCheck(
                "parityCheck",
                t("routing:parity"),
                <Form.Select
                  size="sm"
                  aria-label={t("routing:parity")}
                  style={{ maxWidth: "7rem" }}
                  value={local.paritySelection ?? "Good"}
                  disabled={!edit || !local.parityCheck}
                  onChange={(e) => setField("paritySelection", e.currentTarget.value)}
                >
                  <option value="Good">{t("routing:good")}</option>
                  <option value="Bad">{t("routing:bad")}</option>
                </Form.Select>,
              )}
            </Col>
          </Row>

          <Row className="mt-4">
            <Col md={6}>
              <RoutingPlatformsTable
                allPlatforms={platforms}
                allPlatformsLoading={platformsLoading}
                selectedPlatformIds={local.platformIds ?? []}
                selectedPlatformNames={local.platformNames ?? []}
                edit={edit}
                onAddNames={onAddPlatformNames}
                onRemoveName={onRemovePlatformName}
                onRemoveId={onRemovePlatformId}
              />
            </Col>
            <Col md={6}>
              <RoutingNetlistsTable
                allNetlists={netlists}
                allNetlistsLoading={netlistsLoading}
                selectedNetlistNames={local.netlistNames ?? []}
                edit={edit}
                onAdd={onAddNetlists}
                onRemove={onRemoveNetlist}
              />
            </Col>
          </Row>

          {edit && (
            <Row className="mt-3">
              <Col className="d-flex justify-content-end gap-2">
                <Button
                  onClick={cancel}
                  variant="secondary"
                  aria-label={t("routing:cancel_for", { id: provided.routingId })}
                >
                  <X /> {t("translation:cancel")}
                </Button>
                <Button
                  onClick={saveRouting}
                  variant="primary"
                  aria-label={t("routing:save_routing", { id: provided.routingId })}
                >
                  <Save /> {t("translation:save")}
                </Button>
              </Col>
            </Row>
          )}
        </Card.Body>
      </Card>
    </DetailFade>
  );
};

export default Routing;

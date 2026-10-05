import { use, useCallback, useMemo, useReducer, useState } from "react";
import { Button, Card, Col, Form, FormGroup, Placeholder, Row } from "react-bootstrap";
import { Save, X } from "react-bootstrap-icons";
import { useTranslation } from "react-i18next";
import type { ApiDataSource, ApiDataSourceGroupMember } from "opendcs-api";
import { DetailFade } from "../../../components/data-table";
import { SaveErrorAlert } from "../../../components/forms";
import { useSaveError } from "../../../hooks/useSaveError";
import { PropertiesTable, type Property } from "../../../components/properties";
import { RefListSelect } from "../routing/RoutingSelects";
import type {
  CancelAction,
  CollectionActions,
  SaveAction,
} from "../../../util/Actions";
import { DataSourceReducer, type UiDataSource } from "./DataSourceReducer";
import DataSourceMembersTable from "./DataSourceMembersTable";

const INPUT_H = { height: "2.25rem" };
const LABEL_H = { height: "1rem" };

const DATA_SOURCE_FIELDS = ["name", "type"] as const;

// Group data sources hold member sources instead of free-form properties.
const GROUP_TYPES = new Set(["hotbackupgroup", "roundrobingroup"]);

const isGroupType = (type?: string): boolean =>
  type !== undefined && GROUP_TYPES.has(type.toLowerCase());

export const DataSourceSkeleton: React.FC<{ edit?: boolean; className?: string }> = ({
  edit = false,
  className,
}) => (
  <Card
    className={["datasource-card", edit ? "datasource-card--edit" : null, className]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      <Row>
        <Col md={6}>
          {DATA_SOURCE_FIELDS.map((field) => (
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
            <Placeholder xs={12} style={{ height: "12rem" }} />
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

export interface DataSourceDetails {
  dataSource: UiDataSource;
}

export interface DataSourceProperties {
  details: Promise<DataSourceDetails> | DataSourceDetails;
  actions?: SaveAction<ApiDataSource> & CancelAction<number>;
  edit?: boolean;
}

const mapProps: (props: { [k: string]: string }) => Property[] = (props) =>
  Object.entries(props).map(([name, value]): Property => ({ name, value }));

export const DataSource: React.FC<DataSourceProperties> = ({
  details,
  actions = {},
  edit = false,
}) => {
  const [t] = useTranslation(["datasources", "translation"]);
  const resolved = details instanceof Promise ? use(details) : details;
  const provided = resolved.dataSource;
  const [local, dispatch] = useReducer(DataSourceReducer, provided);
  const { saveError, clearSaveError, attemptSave } = useSaveError(
    t("datasources:save_error"),
    "Data source save failed",
  );
  // DATASOURCE.NAME and DATASOURCETYPE are NOT NULL, so a blank field can only
  // fail on the server.
  const [incomplete, setIncomplete] = useState(false);

  const propsList = useMemo(() => mapProps(local.props ?? {}), [local.props]);
  const group = isGroupType(local.type);

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
    (name: keyof UiDataSource, value: string) =>
      dispatch({ type: "save", payload: { [name]: value } }),
    [],
  );

  const textChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    dispatch({ type: "save", payload: { [name]: value } });
  }, []);

  const onAddMembers = useCallback(
    (members: ApiDataSourceGroupMember[]) =>
      dispatch({ type: "add_members", payload: { members } }),
    [],
  );
  const onRemoveMember = useCallback(
    (name: string) => dispatch({ type: "remove_member", payload: { name } }),
    [],
  );

  const saveDataSource = useCallback(() => {
    const missing = !local.name?.trim() || !local.type;
    setIncomplete(missing);
    if (missing) {
      clearSaveError();
      return;
    }
    void attemptSave(() => actions.save?.(local as ApiDataSource));
  }, [actions, local, attemptSave, clearSaveError]);

  const dismissError = useCallback(() => {
    setIncomplete(false);
    clearSaveError();
  }, [clearSaveError]);

  const cancel = useCallback(() => {
    if (provided.dataSourceId !== undefined) actions.cancel?.(provided.dataSourceId);
  }, [actions, provided.dataSourceId]);

  return (
    <DetailFade skeleton={<DataSourceSkeleton edit={edit} />}>
      <Card
        className={["datasource-card", edit ? "datasource-card--edit" : null]
          .filter(Boolean)
          .join(" ")}
      >
        <Card.Body>
          <Row>
            <Col md={6}>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="name">
                  {t("datasources:name")}
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
                <Form.Label column sm={4} htmlFor="type">
                  {t("datasources:type")}
                </Form.Label>
                <Col sm={8}>
                  <RefListSelect
                    refListName="DataSourceType"
                    id="type"
                    value={local.type}
                    edit={edit}
                    ariaLabel={t("datasources:type")}
                    includeBlank
                    onChange={(v) => setField("type", v)}
                  />
                </Col>
              </FormGroup>
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

          {group && (
            <Row className="mt-4">
              <Col>
                <DataSourceMembersTable
                  members={local.groupMembers ?? []}
                  selfName={local.name}
                  edit={edit}
                  onAdd={onAddMembers}
                  onRemove={onRemoveMember}
                />
              </Col>
            </Row>
          )}

          {edit && (
            <SaveErrorAlert
              error={incomplete ? t("datasources:required_fields") : saveError}
              onClose={dismissError}
            />
          )}
          {edit && (
            <Row className="mt-3">
              <Col className="d-flex justify-content-end gap-2">
                <Button
                  onClick={cancel}
                  variant="secondary"
                  aria-label={t("datasources:cancel_for", {
                    id: provided.dataSourceId,
                  })}
                >
                  <X /> {t("translation:cancel")}
                </Button>
                <Button
                  onClick={saveDataSource}
                  variant="primary"
                  aria-label={t("datasources:save_datasource", {
                    id: provided.dataSourceId,
                  })}
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

export default DataSource;

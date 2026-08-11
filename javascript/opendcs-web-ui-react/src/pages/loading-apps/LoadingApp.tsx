import { Alert, Card, Col, Form, FormGroup, Placeholder, Row } from "react-bootstrap";
import { PropertiesTable, type Property } from "../../components/properties";
import { use, useCallback, useMemo, useReducer, useState } from "react";
import type { ApiLoadingApp } from "opendcs-api";
import { useTranslation } from "react-i18next";
import type { CancelAction, CollectionActions, SaveAction } from "../../util/Actions";
import { LoadingAppReducer } from "./LoadingAppReducer";
import { DetailFade } from "../../components/data-table";
import {
  CancelButton,
  EditFormActions,
  INPUT_H,
  LABEL_H,
  SaveButton,
} from "../../components/forms";
import { apiErrorMessage } from "../../util/ApiError";

const APP_FIELDS = ["appName", "appType", "comment"] as const;

export type UiLoadingApp = Partial<ApiLoadingApp>;

export interface LoadingAppSkeletonProps {
  edit?: boolean;
  className?: string;
}

export const LoadingAppSkeleton: React.FC<LoadingAppSkeletonProps> = ({
  edit = false,
  className,
}) => (
  <Card className={["loading-app-card", className].filter(Boolean).join(" ")}>
    <Card.Body>
      <Row>
        <Col>
          {APP_FIELDS.map((f) => (
            <Row key={f} className="mb-3 align-items-center">
              <Placeholder as={Col} sm={3} animation="glow">
                <Placeholder xs={8} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} sm={9} animation="glow">
                <Placeholder xs={12} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Row>
          ))}
          <Row className="mb-3 align-items-center">
            <Placeholder as={Col} sm={3} animation="glow">
              <Placeholder xs={8} className="rounded" style={LABEL_H} />
            </Placeholder>
            <Col sm={9}>
              <Placeholder animation="glow">
                <Placeholder xs={2} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Col>
          </Row>
        </Col>
        <Col>
          <Placeholder animation="glow">
            <Placeholder xs={12} className="rounded" style={{ height: "10rem" }} />
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

export interface LoadingAppProperties {
  app: Promise<UiLoadingApp> | UiLoadingApp;
  actions?: SaveAction<ApiLoadingApp> & CancelAction<number>;
  edit?: boolean;
}

export const LoadingApp: React.FC<LoadingAppProperties> = ({
  app,
  actions = {},
  edit = false,
}) => {
  const { t } = useTranslation(["loadingapps", "translation"]);
  const providedApp = app instanceof Promise ? use(app) : app;
  const [localApp, dispatch] = useReducer(LoadingAppReducer, providedApp);
  const [saveError, setSaveError] = useState<string | null>(null);

  const props = useMemo<Property[]>(
    () =>
      Object.entries(localApp.properties ?? {}).map(([name, value]) => ({
        name,
        value,
      })),
    [localApp.properties],
  );

  const propertyActions: CollectionActions<Property, string> = edit
    ? {
        remove: (propName) => {
          dispatch({ type: "delete_prop", payload: { name: propName } });
        },
        save: (prop) => {
          dispatch({
            type: "save_prop",
            payload: { name: prop.name, value: prop.value },
          });
        },
      }
    : {};

  const saveApp = useCallback(async () => {
    setSaveError(null);
    try {
      await actions.save?.(localApp as ApiLoadingApp);
    } catch (err) {
      console.warn("Loading app save failed", err);
      setSaveError(apiErrorMessage(err, t("loadingapps:save_error")));
    }
  }, [actions, localApp, t]);

  const inputChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      const { name, value } = event.target;
      dispatch({ type: "save", payload: { [name]: value } });
    },
    [],
  );

  const checkChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    dispatch({ type: "save", payload: { manualEditingApp: event.target.checked } });
  }, []);

  return (
    <DetailFade skeleton={<LoadingAppSkeleton edit={edit} />}>
      <Card className="loading-app-card">
        <Card.Body>
          <Row>
            <Col>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="appName">
                  {t("loadingapps:app_name")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="text"
                    id="appName"
                    name="appName"
                    readOnly={!edit}
                    maxLength={24}
                    defaultValue={localApp.appName}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>

              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="appType">
                  {t("loadingapps:app_type")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    type="text"
                    id="appType"
                    name="appType"
                    readOnly={!edit}
                    list="appTypeOptions"
                    defaultValue={localApp.appType}
                    onChange={inputChange}
                  />
                  <datalist id="appTypeOptions">
                    <option value="computationprocess" />
                    <option value="routingscheduler" />
                    <option value="compdepends" />
                    <option value="utility" />
                    <option value="gui" />
                  </datalist>
                </Col>
              </FormGroup>

              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="comment">
                  {t("loadingapps:comment")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    as="textarea"
                    rows={3}
                    id="comment"
                    name="comment"
                    readOnly={!edit}
                    defaultValue={localApp.comment}
                    onChange={inputChange}
                  />
                </Col>
              </FormGroup>

              <FormGroup as={Row} className="mb-3 align-items-center">
                <Form.Label column sm={3} htmlFor="manualEditingApp">
                  {t("loadingapps:manual_editing")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Check
                    id="manualEditingApp"
                    name="manualEditingApp"
                    disabled={!edit}
                    defaultChecked={localApp.manualEditingApp ?? false}
                    onChange={checkChange}
                  />
                </Col>
              </FormGroup>
            </Col>

            <Col>
              <PropertiesTable
                theProps={props}
                actions={propertyActions}
                edit={edit}
                canAdd={true}
                width="100%"
                height="auto"
              />
            </Col>
          </Row>

          {saveError && (
            <Alert
              variant="danger"
              dismissible
              onClose={() => setSaveError(null)}
              className="mt-3"
            >
              {saveError}
            </Alert>
          )}

          {edit && (
            <EditFormActions>
              <CancelButton
                onClick={() =>
                  providedApp.appId !== undefined && actions.cancel?.(providedApp.appId)
                }
                aria-label={t("loadingapps:cancel_for", { id: providedApp.appId })}
              />
              <SaveButton
                onClick={() => {
                  void saveApp();
                }}
                aria-label={t("loadingapps:save_app", { id: localApp.appId })}
              />
            </EditFormActions>
          )}
        </Card.Body>
      </Card>
    </DetailFade>
  );
};

export default LoadingApp;

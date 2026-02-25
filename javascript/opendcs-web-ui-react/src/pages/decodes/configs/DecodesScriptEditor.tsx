import type React from "react";
import { Button, Card, Col, Row } from "react-bootstrap";
import {
  ApiConfigScriptDataOrderEnum,
  ApiConfigSensor,
  ApiDecodedMessage,
  type ApiConfigScript,
} from "opendcs-api";
import { useCallback, useEffect, useMemo, useReducer, useState } from "react";
import DecodesScriptHeader from "./DecodesScriptHeader";
import ClassicEditor from "./script/ClassicEditor";
import SensorConversion from "./script/SensorConversions";
import { useTranslation } from "react-i18next";
import DecodesSample from "./Sample/Sample";
import type { CancelAction, SaveAction } from "../../../util/Actions";
import decodesScriptReducer from "../../../data/reducers/DecodesScriptReducer";

export interface DecodesScriptEditorProperties {
  script?: Partial<ApiConfigScript>;
  sensors: ApiConfigSensor[];
  decodeData: (raw: string) => ApiDecodedMessage;
  edit?: boolean;
  actions?: SaveAction<ApiConfigScript> & CancelAction<string>;
}

export const DecodesScriptEditor: React.FC<DecodesScriptEditorProperties> = ({
  script,
  sensors,
  decodeData,
  edit = false,
  actions,
}) => {
  const { t } = useTranslation(["decodes"]);
  const [localScript, dispatch] = useReducer(decodesScriptReducer, script || {});

  const sensorMap: { [k: number]: ApiConfigSensor } = useMemo(
    () => Object.fromEntries(sensors.map((sensor) => [sensor.sensorNumber, sensor])),
    [sensors],
  );

  const showSaveCancel = edit && actions !== undefined;
  return (
    <Card>
      <Card.Body>
        <Row>
          <DecodesScriptHeader
            decodesScript={localScript}
            onOrderChange={(order) =>
              dispatch({ type: "set_data_order", payload: { order: order } })
            }
            onHeaderChange={(header) =>
              dispatch({ type: "set_header", payload: { header: header } })
            }
            onNameChange={(name) =>
              dispatch({ type: "set_name", payload: { name: name } })
            }
          />
        </Row>
        <Row>
          <Card>
            <Card.Header>
              {t("decodes:script_editor.format_statements.title")}
            </Card.Header>
            <Card.Body>
              <ClassicEditor
                formatStatements={localScript.formatStatements || []}
                edit={edit}
                onFormatStatementChange={(statements) =>
                  dispatch({
                    type: "set_statements",
                    payload: { statements: statements },
                  })
                }
              />
              Sensors {/** yes this needs much better styling. */}
              <SensorConversion
                configSensors={sensorMap}
                scriptSensors={localScript.scriptSensors || []}
                edit={edit}
                sensorChanged={(sensor) =>
                  dispatch({ type: "set_sensor", payload: { sensor: sensor } })
                }
              />
            </Card.Body>
          </Card>
        </Row>
        <Row>
          <Card>
            <Card.Header>{t("decodes:script_editor.sample.title")}</Card.Header>
            <Card.Body>
              <DecodesSample
                decodeData={(raw: string) => {
                  return decodeData(raw);
                }}
              />
            </Card.Body>
          </Card>
        </Row>
      </Card.Body>
      <Row>
        <Col />
        <Col lg={{ span: 1 }}>
          {showSaveCancel && (
            <>
              <Button
                onClick={() => {
                  actions.save?.(script as ApiConfigScript);
                }}
              >
                {t("translation:cancel")}
              </Button>
              <Button onClick={() => actions.cancel?.(script?.name!)}>
                {t("translation:save")}
              </Button>
            </>
          )}
        </Col>
      </Row>
    </Card>
  );
};

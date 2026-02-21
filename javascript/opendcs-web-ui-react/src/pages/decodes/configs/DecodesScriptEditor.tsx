import type React from "react";
import { Card, Row } from "react-bootstrap";
import {
  ApiConfigScriptDataOrderEnum,
  ApiConfigSensor,
  ApiDecodedMessage,
  type ApiConfigScript,
} from "opendcs-api";
import { useCallback, useEffect, useMemo, useState } from "react";
import DecodesScriptHeader from "./DecodesScriptHeader";
import ClassicEditor from "./script/ClassicEditor";
import SensorConversion from "./script/SensorConversions";
import { useTranslation } from "react-i18next";
import DecodesSample from "./Sample/Sample";

export interface DecodesScriptEditorProperties {
  script?: Partial<ApiConfigScript>;
  sensors: ApiConfigSensor[];
  decodeData: (raw: string) => ApiDecodedMessage;
  edit?: boolean;
}

export const DecodesScriptEditor: React.FC<DecodesScriptEditorProperties> = ({
  script,
  sensors,
  decodeData,
  edit = false,
}) => {
  const { t } = useTranslation(["decodes"]);
  const [localScript, setLocalScript] = useState<Partial<ApiConfigScript>>({});

  const sensorMap: { [k: number]: ApiConfigSensor } = useMemo(
    () => Object.fromEntries(sensors.map((sensor) => [sensor.sensorNumber, sensor])),
    [sensors],
  );

  useEffect(() => setLocalScript(script || {}), [script]);

  const orderChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>) => {
      setLocalScript((prev) => {
        return {
          ...prev,
          dataOrder:
            ApiConfigScriptDataOrderEnum[
              e.target.value as keyof typeof ApiConfigScriptDataOrderEnum
            ],
        };
      });
    },
    [setLocalScript],
  );

  return (
    <Card>
      <Card.Body>
        <Row>
          <DecodesScriptHeader
            decodesScript={localScript}
            onOrderChange={orderChange}
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
              />
              Sensors {/** yes this needs much better styling. */}
              <SensorConversion
                configSensors={sensorMap}
                scriptSensors={localScript.scriptSensors || []}
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
    </Card>
  );
};

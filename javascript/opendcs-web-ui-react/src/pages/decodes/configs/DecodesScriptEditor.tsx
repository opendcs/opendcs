import type React from "react";
import { Card, Col, Form, FormGroup, Row } from "react-bootstrap";
import { DataOrderSelect } from "./DataOrderSelect";
import { DecodesHeaderTypeSelect } from "./HeaderTypeSelect";
import { ApiConfigScriptDataOrderEnum, type ApiConfigScript } from "opendcs-api";
import { useCallback, useEffect, useState } from "react";

export interface DecodesScriptEditorProperties {
  script?: Partial<ApiConfigScript>;
}

export const DecodesScriptEditor: React.FC<DecodesScriptEditorProperties> = ({
  script,
}) => {
  const [localScript, setLocalScript] = useState<Partial<ApiConfigScript>>({});

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
          <Col lg={6}>
            <FormGroup as={Row}>
              <Form.Label column htmlFor="scriptName" lg="auto">
                ScriptName:
              </Form.Label>
              <Col>
                <Form.Control
                  type="text"
                  name="scriptName"
                  id="scriptName"
                  defaultValue={localScript.name}
                />
              </Col>
            </FormGroup>
          </Col>
          <Col xxl={false}></Col>
          <Col lg="auto">
            <FormGroup as={Row}>
              <Form.Label column htmlFor="dataOrder" lg="auto">
                Order:
              </Form.Label>
              <Col lg="auto">
                <DataOrderSelect
                  id="dataOrder"
                  defaultValue={localScript.dataOrder}
                  onChange={orderChange}
                />
              </Col>
            </FormGroup>
          </Col>
          <Col lg="auto">
            <FormGroup as={Row}>
              <Form.Label column lg="auto" htmlFor="headerType">
                Header Type:
              </Form.Label>
              <Col lg="auto">
                <DecodesHeaderTypeSelect
                  id="headerType"
                  defaultValue={localScript.headerType}
                />
              </Col>
            </FormGroup>
          </Col>
        </Row>
      </Card.Body>
    </Card>
  );
};

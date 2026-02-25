import type { ApiConfigScript, ApiConfigScriptDataOrderEnum } from "opendcs-api";
import type React from "react";
import { Col, Form, FormGroup, Row } from "react-bootstrap";
import { DataOrderSelect } from "./DataOrderSelect";
import { DecodesHeaderTypeSelect } from "./HeaderTypeSelect";

export interface DecodesScriptHeaderProperties {
  decodesScript: ApiConfigScript;
  onOrderChange?: (order: ApiConfigScriptDataOrderEnum) => void;
  onHeaderChange?: (head: string) => void;
  onNameChange?: (name: string) => void;
}

const DecodesScriptHeader: React.FC<DecodesScriptHeaderProperties> = ({
  decodesScript,
  onOrderChange,
  onHeaderChange,
  onNameChange,
}) => {
  return (
    <>
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
              defaultValue={decodesScript.name}
              onChange={(e) => {
                onNameChange?.(e.currentTarget.value);
              }}
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
              defaultValue={decodesScript.dataOrder}
              onChange={onOrderChange}
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
              defaultValue={decodesScript.headerType}
              onChange={onHeaderChange}
            />
          </Col>
        </FormGroup>
      </Col>
    </>
  );
};

export default DecodesScriptHeader;

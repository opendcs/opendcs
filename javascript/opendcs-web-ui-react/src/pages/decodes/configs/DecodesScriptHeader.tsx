import type { ApiConfigScript } from "opendcs-api";
import type React from "react";
import { Col, Form, FormGroup, Row } from "react-bootstrap";
import { DataOrderSelect } from "./DataOrderSelect";
import { DecodesHeaderTypeSelect } from "./HeaderTypeSelect";

export interface DecodesScriptHeaderProperties {
  decodesScript: ApiConfigScript;
  onOrderChange?: (e: React.ChangeEvent<HTMLSelectElement>) => void;
}

const DecodesScriptHeader: React.FC<DecodesScriptHeaderProperties> = ({
  decodesScript,
  onOrderChange,
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
            />
          </Col>
        </FormGroup>
      </Col>
    </>
  );
};

export default DecodesScriptHeader;

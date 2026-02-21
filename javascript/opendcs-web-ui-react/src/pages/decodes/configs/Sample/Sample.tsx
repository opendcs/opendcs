import type React from "react";
import DecodesSampleRetrieve from "./SampleRetrieve";
import { ApiDecodedMessage } from "opendcs-api";
import { useState } from "react";
import { Row } from "react-bootstrap";
import DecodesDataGrid from "./DecodesDataGrid";

export interface DecodesSampleProperties {
  decodeData?: (raw: string) => ApiDecodedMessage;
}

const DecodesSample: React.FC<DecodesSampleProperties> = ({ decodeData }) => {
  const [output, setOutput] = useState<ApiDecodedMessage | undefined>(undefined);

  return (
    <Row>
      <DecodesSampleRetrieve
        output={output}
        decodeData={(raw: string) => setOutput(decodeData?.(raw))}
      />
      <DecodesDataGrid output={output?.timeSeries} />
    </Row>
  );
};

export default DecodesSample;

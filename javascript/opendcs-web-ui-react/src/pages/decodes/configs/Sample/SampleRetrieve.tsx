import type React from "react";
import type { ApiDecodedMessage } from "opendcs-api";
import { useRef } from "react";
import { Button, Col } from "react-bootstrap";

export interface DecodesSampleRetrieveProperties {
  output?: ApiDecodedMessage; // used to highlight the data within the textarea... somehow, probably need to render different element and flip back and forth or something.
  decodeData?: (raw: string) => void;
}

const DecodesSampleRetrieve: React.FC<DecodesSampleRetrieveProperties> = ({
  output,
  decodeData,
}) => {
  const textAreaRef = useRef<HTMLTextAreaElement>(null);

  return (
    <>
      <Col lg={6}>
        <textarea
          aria-label="Raw Message Data input"
          ref={textAreaRef}
          style={{ resize: "both", width: "100%" }}
        ></textarea>
      </Col>
      <Col lg={4}>
        <Button
          variant="primary"
          onClick={(e) => {
            e.preventDefault();
            if (textAreaRef.current && textAreaRef.current.value.trim() !== "") {
              decodeData?.(textAreaRef.current.value);
            }
          }}
        >
          Decode
        </Button>
      </Col>
    </>
  );
};

export default DecodesSampleRetrieve;

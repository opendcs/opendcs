import { Col, Row } from "react-bootstrap";
import type UserProperties from "./UserProperties";

export default function TimeInfo({ user }: UserProperties) {
  return (
    <Row sm="auto">
      <Col sm="auto">Created At:</Col>
      <Col sm="auto">{user.createdAt?.toISOString()}</Col>
      <Col sm="auto">Updated At:</Col>
      <Col sm="auto">{user.updatedAt?.toISOString()}</Col>
    </Row>
  );
}

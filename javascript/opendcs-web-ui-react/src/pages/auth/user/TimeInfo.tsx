import { Col, Row } from "react-bootstrap";
import type UserProperties from "./UserProperties";
import { useTranslation } from "react-i18next";

export default function TimeInfo({ user }: UserProperties) {
  const { t } = useTranslation(["user-data"]);
  return (
    <Row sm="auto">
      <Col sm="auto">{t("userprofile.created_at")}:</Col>
      <Col sm="auto">{user.createdAt?.toISOString()}</Col>
      <Col sm="auto">{t("userprofile.updated_at")}:</Col>
      <Col sm="auto">{user.updatedAt?.toISOString()}</Col>
    </Row>
  );
}

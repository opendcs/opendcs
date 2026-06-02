import { use, useCallback, useMemo, useReducer } from "react";
import { Card, Col, Form, FormGroup, Placeholder, Row } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ApiNetList, ApiNetListItem } from "opendcs-api";
import { DetailFade } from "../../../components/data-table";
import {
  CancelButton,
  EditFormActions,
  INPUT_H,
  LABEL_H,
  SaveButton,
} from "../../../components/forms";
import type { CancelAction, SaveAction } from "../../../util/Actions";
import { RefListSelect } from "../routing/RoutingSelects";
import { NetlistReducer, type UiNetlist } from "./NetlistReducer";
import NetlistItemsTable from "./NetlistItemsTable";

const NETLIST_FIELDS = ["name", "transportMediumType", "siteNameTypePref"] as const;

export interface NetlistSkeletonProps {
  edit?: boolean;
  className?: string;
}

export const NetlistSkeleton: React.FC<NetlistSkeletonProps> = ({
  edit = false,
  className,
}) => (
  <Card
    className={["netlist-card", edit ? "netlist-card--edit" : null, className]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      <Row>
        <Col md={6}>
          {NETLIST_FIELDS.map((field) => (
            <Row key={field} className="mb-3 align-items-center">
              <Placeholder as={Col} sm={4} animation="glow">
                <Placeholder xs={10} className="rounded" style={LABEL_H} />
              </Placeholder>
              <Placeholder as={Col} sm={8} animation="glow">
                <Placeholder xs={12} className="rounded" style={INPUT_H} />
              </Placeholder>
            </Row>
          ))}
        </Col>
        <Col md={6}>
          <Placeholder animation="glow" className="d-block">
            <Placeholder xs={12} style={{ height: "16rem" }} />
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

export interface NetlistDetails {
  netlist: UiNetlist;
}

export interface NetlistProperties {
  details: Promise<NetlistDetails> | NetlistDetails;
  actions?: SaveAction<ApiNetList> & CancelAction<number>;
  edit?: boolean;
}

export const Netlist: React.FC<NetlistProperties> = ({
  details,
  actions = {},
  edit = false,
}) => {
  const [t] = useTranslation(["netlists", "translation"]);
  const resolved = details instanceof Promise ? use(details) : details;
  const provided = resolved.netlist;
  const [local, dispatch] = useReducer(NetlistReducer, provided);

  const itemsList = useMemo(() => Object.values(local.items ?? {}), [local.items]);

  const textChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    dispatch({ type: "save", payload: { [name]: value } });
  }, []);

  const setField = useCallback(
    <K extends keyof UiNetlist>(name: K, value: UiNetlist[K]) =>
      dispatch({ type: "save", payload: { [name]: value } as UiNetlist }),
    [],
  );

  const onItemSave = useCallback(
    (item: ApiNetListItem) => dispatch({ type: "save_item", payload: { item } }),
    [],
  );
  const onItemRemove = useCallback(
    (transportId: string) =>
      dispatch({ type: "delete_item", payload: { transportId } }),
    [],
  );

  const saveNetlist = useCallback(() => {
    actions.save?.(local as ApiNetList);
  }, [actions, local]);

  const cancel = useCallback(() => {
    if (provided.netlistId !== undefined) actions.cancel?.(provided.netlistId);
  }, [actions, provided.netlistId]);

  return (
    <DetailFade skeleton={<NetlistSkeleton edit={edit} />}>
      <Card
        className={["netlist-card", edit ? "netlist-card--edit" : null]
          .filter(Boolean)
          .join(" ")}
      >
        <Card.Body>
          <Row>
            <Col md={6}>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="name">
                  {t("netlists:name")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="text"
                    id="name"
                    name="name"
                    readOnly={!edit}
                    defaultValue={local.name ?? ""}
                    onChange={textChange}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="transportMediumType">
                  {t("netlists:transportMediumType")}
                </Form.Label>
                <Col sm={8}>
                  <RefListSelect
                    refListName="TransportMediumType"
                    id="transportMediumType"
                    value={local.transportMediumType}
                    edit={edit}
                    includeBlank
                    ariaLabel={t("netlists:transportMediumType")}
                    onChange={(v) => setField("transportMediumType", v)}
                  />
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="siteNameTypePref">
                  {t("netlists:siteNameTypePref")}
                </Form.Label>
                <Col sm={8}>
                  <RefListSelect
                    refListName="SiteNameType"
                    id="siteNameTypePref"
                    value={local.siteNameTypePref}
                    edit={edit}
                    includeBlank
                    ariaLabel={t("netlists:siteNameTypePref")}
                    onChange={(v) => setField("siteNameTypePref", v)}
                  />
                </Col>
              </FormGroup>
            </Col>
            <Col md={6}>
              <NetlistItemsTable
                items={itemsList}
                edit={edit}
                onSave={onItemSave}
                onRemove={onItemRemove}
              />
            </Col>
          </Row>

          {edit && (
            <EditFormActions>
              <CancelButton
                onClick={cancel}
                aria-label={t("netlists:cancel_for", { id: provided.netlistId })}
              />
              <SaveButton
                onClick={saveNetlist}
                aria-label={t("netlists:save_netlist", { id: provided.netlistId })}
              />
            </EditFormActions>
          )}
        </Card.Body>
      </Card>
    </DetailFade>
  );
};

export default Netlist;

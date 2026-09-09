import { use, useCallback, useMemo, useReducer, useState } from "react";
import { Button, Card, Col, Form, FormGroup, Placeholder, Row } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type {
  ApiDataType,
  ApiTimeSeriesIdentifier,
  ApiTsGroup,
  ApiTsGroupRef,
} from "opendcs-api";
import { DetailFade } from "../../../components/data-table";
import {
  CancelButton,
  EditFormActions,
  INPUT_H,
  LABEL_H,
  SaveButton,
} from "../../../components/forms";
import type { CancelAction, SaveAction } from "../../../util/Actions";
import { useSiteNameType } from "../../../contexts/app/SiteNameTypeContext";
import { criteriaRows, dataTypeLabel, siteCriterionLabel } from "./groupCriteria";
import {
  TsGroupReducer,
  type NewCriterion,
  type SubGroupCombine,
  type UiTsGroup,
} from "./TsGroupReducer";
import TsGroupMembersTable from "./TsGroupMembersTable";
import SubGroupsTable from "./SubGroupsTable";
import GroupCriteriaTable from "./GroupCriteriaTable";
import EvaluateGroupModal from "./EvaluateGroupModal";

const TS_GROUP_FIELDS = ["groupName", "groupType", "description"] as const;

export interface TsGroupSkeletonProps {
  edit?: boolean;
  className?: string;
}

export const TsGroupSkeleton: React.FC<TsGroupSkeletonProps> = ({
  edit = false,
  className,
}) => (
  <Card
    className={["tsgroup-card", edit ? "tsgroup-card--edit" : null, className]
      .filter(Boolean)
      .join(" ")}
  >
    <Card.Body>
      <Row>
        <Col md={6}>
          {TS_GROUP_FIELDS.map((field) => (
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
      </Row>
      <Placeholder animation="glow" className="d-block mt-3">
        <Placeholder xs={12} style={{ height: "10rem" }} />
      </Placeholder>
      <Placeholder animation="glow" className="d-block mt-3">
        <Placeholder xs={12} style={{ height: "8rem" }} />
      </Placeholder>
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

export interface TsGroupDetails {
  group: UiTsGroup;
}

export interface TsGroupProperties {
  details: Promise<TsGroupDetails> | TsGroupDetails;
  /** All groups in the database, for the sub-group chooser. */
  allGroups: ApiTsGroupRef[];
  allGroupsLoading?: boolean;
  /** Time series catalog, for the explicit-members chooser. */
  timeSeries: ApiTimeSeriesIdentifier[];
  timeSeriesLoading?: boolean;
  /** Data type catalog, used to label data-type criteria the API returns by id. */
  dataTypes?: ApiDataType[];
  actions?: SaveAction<ApiTsGroup> & CancelAction<number>;
  edit?: boolean;
}

/**
 * Time Series Group editor, mirroring the desktop toolkit's group editor tab:
 * identity fields on top, then the three ways a group gains members —
 * explicit time series, sub-groups combined in/out/intersected, and TSID-part
 * criteria — plus Evaluate to see what the saved definition resolves to.
 */
export const TsGroup: React.FC<TsGroupProperties> = ({
  details,
  allGroups,
  allGroupsLoading = false,
  timeSeries,
  timeSeriesLoading = false,
  dataTypes = [],
  actions = {},
  edit = false,
}) => {
  const [t] = useTranslation(["tsgroups", "translation"]);
  const { siteNameType } = useSiteNameType();
  const resolved = details instanceof Promise ? use(details) : details;
  const provided = resolved.group;
  const [local, dispatch] = useReducer(TsGroupReducer, provided);
  const [showEvaluate, setShowEvaluate] = useState(false);
  const [dirty, setDirty] = useState(false);

  // Group types are free-form strings; the desktop's "New Type" button just
  // lets you type one. Offer the types already in use as suggestions while
  // still allowing anything to be typed.
  const groupTypeOptions = useMemo(
    () =>
      [...new Set(allGroups.map((g) => g.groupType ?? "").filter(Boolean))].sort(
        (a, b) => a.localeCompare(b),
      ),
    [allGroups],
  );

  const { includeGroups, excludeGroups, intersectGroups } = local;
  const subGroups = useMemo(
    () =>
      ({
        include: includeGroups ?? [],
        exclude: excludeGroups ?? [],
        intersect: intersectGroups ?? [],
      }) satisfies Record<SubGroupCombine, ApiTsGroupRef[]>,
    [includeGroups, excludeGroups, intersectGroups],
  );

  // GET /tsgroup returns groupDataTypes with only `id` set, so resolve each id
  // against the catalog to show "CWMS:Opening" rather than a bare number.
  const dataTypesById = useMemo(
    () => new Map(dataTypes.map((dt) => [dt.id, dt])),
    [dataTypes],
  );

  const { groupSites, groupDataTypes, groupAttrs } = local;
  const criteria = useMemo(
    () =>
      criteriaRows(
        { groupSites, groupDataTypes, groupAttrs },
        (site) => siteCriterionLabel(site, siteNameType.preferredType),
        (dt) => dataTypeLabel(dataTypesById.get(dt.id) ?? dt),
      ),
    [groupSites, groupDataTypes, groupAttrs, siteNameType.preferredType, dataTypesById],
  );

  const change = useCallback((payload: UiTsGroup) => {
    setDirty(true);
    dispatch({ type: "save", payload });
  }, []);

  const textChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      const { name, value } = event.target;
      change({ [name]: value });
    },
    [change],
  );

  const onAddMembers = useCallback((tsIds: ApiTimeSeriesIdentifier[]) => {
    setDirty(true);
    dispatch({ type: "add_ts_members", payload: { tsIds } });
  }, []);

  const onRemoveMember = useCallback((key: number) => {
    setDirty(true);
    dispatch({ type: "remove_ts_member", payload: { key } });
  }, []);

  const onAddSubGroups = useCallback(
    (combine: SubGroupCombine, groups: ApiTsGroupRef[]) => {
      setDirty(true);
      dispatch({ type: "add_subgroups", payload: { combine, groups } });
    },
    [],
  );

  const onRemoveSubGroup = useCallback((combine: SubGroupCombine, groupId: number) => {
    setDirty(true);
    dispatch({ type: "remove_subgroup", payload: { combine, groupId } });
  }, []);

  const onAddCriterion = useCallback((criterion: NewCriterion) => {
    setDirty(true);
    dispatch({ type: "add_criterion", payload: { criterion } });
  }, []);

  const onRemoveCriterion = useCallback((key: string) => {
    setDirty(true);
    dispatch({ type: "remove_criterion", payload: { key } });
  }, []);

  const saveGroup = useCallback(() => {
    actions.save?.(local as ApiTsGroup);
  }, [actions, local]);

  const cancel = useCallback(() => {
    if (provided.groupId !== undefined) actions.cancel?.(provided.groupId);
  }, [actions, provided.groupId]);

  return (
    <DetailFade skeleton={<TsGroupSkeleton edit={edit} />}>
      <Card
        className={["tsgroup-card", edit ? "tsgroup-card--edit" : null]
          .filter(Boolean)
          .join(" ")}
      >
        <Card.Body>
          <Row>
            <Col md={6}>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="groupName">
                  {t("tsgroups:groupName")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="text"
                    id="groupName"
                    name="groupName"
                    readOnly={!edit}
                    defaultValue={local.groupName ?? ""}
                    onChange={textChange}
                  />
                  <Form.Text>{t("tsgroups:groupName_hint")}</Form.Text>
                </Col>
              </FormGroup>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={4} htmlFor="groupType">
                  {t("tsgroups:groupType")}
                </Form.Label>
                <Col sm={8}>
                  <Form.Control
                    type="text"
                    id="groupType"
                    name="groupType"
                    list="tsGroupTypeOptions"
                    readOnly={!edit}
                    defaultValue={local.groupType ?? ""}
                    onChange={textChange}
                  />
                  <datalist id="tsGroupTypeOptions">
                    {groupTypeOptions.map((type) => (
                      <option key={type} value={type} />
                    ))}
                  </datalist>
                </Col>
              </FormGroup>
            </Col>
            <Col md={6}>
              <FormGroup as={Row} className="mb-3">
                <Form.Label column sm={3} htmlFor="description">
                  {t("translation:description")}
                </Form.Label>
                <Col sm={9}>
                  <Form.Control
                    as="textarea"
                    rows={4}
                    id="description"
                    name="description"
                    readOnly={!edit}
                    defaultValue={local.description ?? ""}
                    onChange={textChange}
                  />
                </Col>
              </FormGroup>
            </Col>
          </Row>

          <Row className="mt-2">
            <Col>
              <TsGroupMembersTable
                members={local.tsIds ?? []}
                catalog={timeSeries}
                catalogLoading={timeSeriesLoading}
                edit={edit}
                onAdd={onAddMembers}
                onRemove={onRemoveMember}
              />
            </Col>
          </Row>

          <Row className="mt-4">
            <Col>
              <SubGroupsTable
                subGroups={subGroups}
                allGroups={allGroups}
                allGroupsLoading={allGroupsLoading}
                selfId={local.groupId}
                edit={edit}
                onAdd={onAddSubGroups}
                onRemove={onRemoveSubGroup}
              />
            </Col>
          </Row>

          <Row className="mt-4">
            <Col>
              <GroupCriteriaTable
                criteria={criteria}
                edit={edit}
                onAdd={onAddCriterion}
                onRemove={onRemoveCriterion}
              />
            </Col>
          </Row>

          <EditFormActions>
            <Button
              variant="outline-secondary"
              onClick={() => setShowEvaluate(true)}
              disabled={!(provided.groupId && provided.groupId > 0)}
              aria-label={t("tsgroups:evaluate.button_for", {
                name: local.groupName ?? provided.groupId,
              })}
            >
              {t("tsgroups:evaluate.button")}
            </Button>
            {edit && (
              <>
                <CancelButton
                  onClick={cancel}
                  aria-label={t("tsgroups:cancel_for", { id: provided.groupId })}
                />
                <SaveButton
                  onClick={saveGroup}
                  aria-label={t("tsgroups:save_group", { id: provided.groupId })}
                />
              </>
            )}
          </EditFormActions>
        </Card.Body>
      </Card>

      <EvaluateGroupModal
        show={showEvaluate}
        onHide={() => setShowEvaluate(false)}
        groupId={provided.groupId}
        groupName={local.groupName}
        dirty={dirty}
      />
    </DetailFade>
  );
};

export default TsGroup;

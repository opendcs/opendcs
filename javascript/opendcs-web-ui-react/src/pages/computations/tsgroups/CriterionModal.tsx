import { useCallback, useMemo, useState } from "react";
import { Button, Col, Form, Modal, Row, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ApiDataType, ApiSiteRef } from "opendcs-api";
import { ChooserTable, type ChooserColumnDef } from "../../../components/data-table";
import { useSiteNameType } from "../../../contexts/app/SiteNameTypeContext";
import { useSitesQuery } from "../../../queries/sites";
import { useDataTypeListQuery } from "../../../queries/dataTypes";
import { useIntervalsQuery } from "../../../queries/intervals";
import {
  siteCriterionLabel,
  splitBaseSub,
  PART_LOCATION,
  PART_PARAM,
} from "./groupCriteria";
import type { NewCriterion } from "./TsGroupReducer";

/** The TSID parts the editor's criteria buttons can add. */
export const CRITERION_PARTS = [
  "Location",
  "Param",
  "ParamType",
  "Interval",
  "Duration",
  "Version",
] as const;

export type CriterionPart = (typeof CRITERION_PARTS)[number];

/**
 * Location, Param and Version can each be matched in full, or by only their
 * base or sub half — the desktop's three radio buttons. The remaining parts
 * have no base/sub split.
 */
const SCOPED_PARTS: ReadonlySet<string> = new Set([
  PART_LOCATION,
  PART_PARAM,
  "Version",
]);

export type CriterionScope = "full" | "base" | "sub";

const SCOPES: CriterionScope[] = ["full", "base", "sub"];

/** Attribute name a scoped part resolves to, e.g. Location + base -> BaseLocation. */
const scopedAttrName = (part: CriterionPart, scope: CriterionScope): string => {
  if (scope === "base") return `Base${part}`;
  if (scope === "sub") return `Sub${part}`;
  return part;
};

export interface CriterionModalProps {
  /** Part being added, or undefined when the dialog is closed. */
  part?: CriterionPart;
  onHide: () => void;
  onAdd: (criterion: NewCriterion) => void;
}

/**
 * Adds one "other criteria" filter to a time series group.
 *
 * Replicates the desktop editor's Location / Param / ParamType / Interval /
 * Duration / Version dialogs in a single component: Location and Param list
 * the database's sites and data types to pick from, Interval offers the
 * defined intervals, and the rest are free text. Location, Param and Version
 * additionally offer full / base / sub scope, and leave the resulting value
 * editable so a sub-part can carry a '*' wildcard.
 */
export const CriterionModal: React.FC<CriterionModalProps> = ({
  part,
  onHide,
  onAdd,
}) => {
  const [t] = useTranslation(["tsgroups", "translation"]);
  const { siteNameType } = useSiteNameType();

  const needsSites = part === PART_LOCATION;
  const needsDataTypes = part === PART_PARAM;
  const needsIntervals = part === "Interval";

  const { data: sites = [], isFetching: sitesLoading } = useSitesQuery();
  const { data: dataTypes = [], isFetching: dataTypesLoading } = useDataTypeListQuery();
  const { data: intervals = [] } = useIntervalsQuery();

  const [scope, setScope] = useState<CriterionScope>("full");
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  // The result is editable so a sub-part can be given a wildcard; it is
  // recomputed from the selection whenever the user picks or re-scopes, and
  // `dirty` records that the user has typed over it.
  const [result, setResult] = useState("");
  const [dirty, setDirty] = useState(false);

  // Reset every time the dialog opens for a new part (in-render transition
  // compare, as elsewhere in the data-table modals).
  const [openedFor, setOpenedFor] = useState(part);
  if (part !== openedFor) {
    setOpenedFor(part);
    setScope("full");
    setSelectedIds([]);
    setResult("");
    setDirty(false);
  }

  const selectedSite = useMemo(
    () => sites.find((s) => s.siteId === selectedIds[0]),
    [sites, selectedIds],
  );
  const selectedDataType = useMemo(
    () => dataTypes.find((d) => d.id === selectedIds[0]),
    [dataTypes, selectedIds],
  );

  const fullValue = useMemo(() => {
    if (needsSites) {
      return selectedSite
        ? siteCriterionLabel(selectedSite, siteNameType.preferredType)
        : "";
    }
    if (needsDataTypes) return selectedDataType?.code ?? "";
    return "";
  }, [
    needsSites,
    needsDataTypes,
    selectedSite,
    selectedDataType,
    siteNameType.preferredType,
  ]);

  /** Value the current selection + scope implies, before any manual edit. */
  const derivedValue = useMemo(() => {
    if (!needsSites && !needsDataTypes) return result;
    if (scope === "full") return fullValue;
    const { base, sub } = splitBaseSub(fullValue);
    return scope === "base" ? base : sub;
  }, [needsSites, needsDataTypes, scope, fullValue, result]);

  const value = dirty ? result : derivedValue;

  const selectRows = useCallback((ids: number[]) => {
    setSelectedIds(ids);
    setDirty(false);
  }, []);

  const changeScope = useCallback((next: CriterionScope) => {
    setScope(next);
    setDirty(false);
  }, []);

  const siteColumns = useMemo<ChooserColumnDef<ApiSiteRef>[]>(
    () => [
      {
        data: null,
        header: t("tsgroups:criteria.header.Location"),
        render: (_d, _type, row) => siteCriterionLabel(row, siteNameType.preferredType),
      },
      {
        data: null,
        header: t("tsgroups:criteria.header.BasePart"),
        defaultContent: "",
        render: (_d, _type, row) =>
          splitBaseSub(siteCriterionLabel(row, siteNameType.preferredType)).base,
      },
      {
        data: null,
        header: t("tsgroups:criteria.header.SubPart"),
        defaultContent: "",
        render: (_d, _type, row) =>
          splitBaseSub(siteCriterionLabel(row, siteNameType.preferredType)).sub,
      },
      {
        data: "description",
        header: t("tsgroups:criteria.header.Description"),
        defaultContent: "",
      },
    ],
    [t, siteNameType.preferredType],
  );

  const dataTypeColumns = useMemo<ChooserColumnDef<ApiDataType>[]>(
    () => [
      { data: "standard", header: t("tsgroups:criteria.header.Standard") },
      { data: "code", header: t("tsgroups:criteria.header.Param") },
      {
        data: null,
        header: t("tsgroups:criteria.header.BasePart"),
        defaultContent: "",
        render: (_d, _type, row) => splitBaseSub(row.code ?? "").base,
      },
      {
        data: null,
        header: t("tsgroups:criteria.header.SubPart"),
        defaultContent: "",
        render: (_d, _type, row) => splitBaseSub(row.code ?? "").sub,
      },
      {
        data: "displayName",
        header: t("tsgroups:criteria.header.DisplayName"),
        defaultContent: "",
      },
    ],
    [t],
  );

  // A full Location/Param is stored as the picked database record, so its value
  // is not free text — only the base/sub halves (and the free-text parts) are
  // editable. Without this the dialog could emit a "Location="/"Param="
  // attribute, which is not a name the API defines.
  const resultLocked = (needsSites || needsDataTypes) && scope === "full";

  const canAdd = value.trim().length > 0;

  const confirm = useCallback(() => {
    if (!part || !canAdd) return;
    // A full location or param is stored as the resolved database record; the
    // base/sub halves, and every unscoped part, are stored as attributes.
    if (scope === "full" && needsSites && selectedSite) {
      onAdd({ kind: "site", site: selectedSite });
    } else if (scope === "full" && needsDataTypes && selectedDataType) {
      onAdd({ kind: "datatype", dataType: selectedDataType });
    } else {
      onAdd({
        kind: "attr",
        name: SCOPED_PARTS.has(part) ? scopedAttrName(part, scope) : part,
        value: value.trim(),
      });
    }
    onHide();
  }, [
    part,
    canAdd,
    scope,
    needsSites,
    needsDataTypes,
    selectedSite,
    selectedDataType,
    value,
    onAdd,
    onHide,
  ]);

  const loading =
    (needsSites && sitesLoading && sites.length === 0) ||
    (needsDataTypes && dataTypesLoading && dataTypes.length === 0);

  return (
    <Modal show={part !== undefined} onHide={onHide} size="lg" scrollable>
      <Modal.Header closeButton>
        <Modal.Title>
          {t("tsgroups:criteria.add_title", {
            part: part ? t(`tsgroups:criteria.part.${part}`) : "",
          })}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {loading && (
          <div className="text-center py-4">
            <Spinner animation="border" role="status" />
          </div>
        )}
        {!loading && needsSites && (
          <ChooserTable<ApiSiteRef, number>
            data={sites}
            getId={(s) => s.siteId!}
            columns={siteColumns}
            selectedIds={selectedIds}
            onSelectionChange={selectRows}
            tableId="tsGroupLocationChooser"
          />
        )}
        {!loading && needsDataTypes && (
          <ChooserTable<ApiDataType, number>
            data={dataTypes}
            getId={(d) => d.id!}
            columns={dataTypeColumns}
            selectedIds={selectedIds}
            onSelectionChange={selectRows}
            tableId="tsGroupParamChooser"
          />
        )}
        {!loading && needsIntervals && (
          <Form.Group as={Row} className="mb-3">
            <Form.Label column sm={3} htmlFor="criterionInterval">
              {t("tsgroups:criteria.part.Interval")}
            </Form.Label>
            <Col sm={9}>
              <Form.Select
                id="criterionInterval"
                value={value}
                onChange={(e) => {
                  setResult(e.currentTarget.value);
                  setDirty(true);
                }}
              >
                <option value="" />
                {intervals.map((interval) => (
                  <option
                    key={interval.intervalId ?? interval.name}
                    value={interval.name}
                  >
                    {interval.name}
                  </option>
                ))}
              </Form.Select>
            </Col>
          </Form.Group>
        )}

        {part && SCOPED_PARTS.has(part) && (
          <fieldset className="mt-3">
            <legend className="fs-6">{t("tsgroups:criteria.scope")}</legend>
            {SCOPES.map((s) => (
              <Form.Check
                key={s}
                inline
                type="radio"
                name="criterionScope"
                id={`criterionScope-${s}`}
                checked={scope === s}
                onChange={() => changeScope(s)}
                label={t(`tsgroups:criteria.scope_${s}`, {
                  part: t(`tsgroups:criteria.part.${part}`),
                })}
              />
            ))}
            {scope === "sub" && (
              <Form.Text className="d-block">
                {t("tsgroups:criteria.wildcard_hint")}
              </Form.Text>
            )}
          </fieldset>
        )}

        {!needsIntervals && (
          <Form.Group as={Row} className="mt-3">
            <Form.Label column sm={3} htmlFor="criterionResult">
              {t("tsgroups:criteria.result")}
            </Form.Label>
            <Col sm={9}>
              <Form.Control
                id="criterionResult"
                type="text"
                readOnly={resultLocked}
                value={value}
                onChange={(e) => {
                  setResult(e.currentTarget.value);
                  setDirty(true);
                }}
              />
              {resultLocked && (
                <Form.Text>{t("tsgroups:criteria.full_locked_hint")}</Form.Text>
              )}
            </Col>
          </Form.Group>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          {t("translation:cancel")}
        </Button>
        <Button variant="primary" onClick={confirm} disabled={!canAdd}>
          {t("tsgroups:criteria.add_confirm")}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default CriterionModal;

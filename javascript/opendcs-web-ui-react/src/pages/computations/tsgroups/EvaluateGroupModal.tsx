import { useMemo } from "react";
import { Alert, Button, Modal, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ApiTimeSeriesIdentifier } from "opendcs-api";
import { AppDataTable, type ColumnDef } from "../../../components/data-table";
import { useExpandTsGroupQuery } from "../../../queries/tsGroups";
import { tsIdColumns } from "./tsIdColumns";
import { apiErrorMessage } from "../../../util/ApiError";

export interface EvaluateGroupModalProps {
  show: boolean;
  onHide: () => void;
  groupId?: number;
  groupName?: string;
  /** True when the editor holds edits that have not been saved yet. */
  dirty?: boolean;
}

/**
 * The desktop editor's "Evaluate" button: the fully expanded list of time
 * series the group currently resolves to, including sub-group and criteria
 * matches.
 *
 * The server expands the *stored* definition, so unsaved edits are not
 * reflected — the dialog says so rather than silently showing stale results.
 */
export const EvaluateGroupModal: React.FC<EvaluateGroupModalProps> = ({
  show,
  onHide,
  groupId,
  groupName,
  dirty = false,
}) => {
  const [t] = useTranslation(["tsgroups", "translation"]);
  const {
    data: expanded = [],
    isFetching,
    error,
  } = useExpandTsGroupQuery(groupId, show);

  const columns = useMemo<ColumnDef<ApiTimeSeriesIdentifier>[]>(
    () => tsIdColumns(t),
    [t],
  );

  return (
    <Modal show={show} onHide={onHide} size="xl" scrollable>
      <Modal.Header closeButton>
        <Modal.Title>
          {t("tsgroups:evaluate.title", { name: groupName ?? groupId })}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {dirty && <Alert variant="warning">{t("tsgroups:evaluate.unsaved")}</Alert>}
        {error && (
          <Alert variant="danger">
            {apiErrorMessage(error, t("tsgroups:evaluate.failed"))}
          </Alert>
        )}
        {isFetching && (
          <div className="text-center py-4">
            <Spinner animation="border" role="status" />
          </div>
        )}
        {!isFetching && !error && (
          <AppDataTable<ApiTimeSeriesIdentifier, number>
            data={expanded}
            getId={(ts) => ts.key!}
            columns={columns}
            caption={t("tsgroups:evaluate.count", { count: expanded.length })}
            tableId="tsGroupEvaluateTable"
          />
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          {t("translation:Close")}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default EvaluateGroupModal;

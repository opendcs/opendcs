import { useMemo } from "react";
import { Alert } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import {
  MultiSelectorModal,
  type ChooserColumnDef,
} from "../../../components/data-table";
import {
  useAlgorithmCatalogQuery,
  useImportAlgorithmsMutation,
  type CatalogAlgorithm,
} from "../../../queries/algorithms";

interface Props {
  show: boolean;
  onHide: () => void;
  onImported: () => void;
}

export const CheckForNewModal: React.FC<Props> = ({ show, onHide, onImported }) => {
  const [t] = useTranslation(["algorithms", "translation"]);

  const {
    data: catalog = [],
    isLoading,
    isError: catalogError,
  } = useAlgorithmCatalogQuery(show);

  const available = useMemo(() => catalog.filter((a) => !a.alreadyImported), [catalog]);

  const importMutation = useImportAlgorithmsMutation({
    onSuccess: () => {
      onImported();
      onHide();
    },
  });

  const columns = useMemo<ChooserColumnDef<CatalogAlgorithm>[]>(
    () => [
      { data: "name", header: t("algorithms:header.Name") },
      { data: "execClass", header: t("algorithms:header.ExecClass") },
      { data: "description", header: t("algorithms:header.Description") },
    ],
    [t],
  );

  const banner = (
    <>
      {catalogError && (
        <Alert variant="danger">{t("algorithms:check_new.fetch_error")}</Alert>
      )}
      {importMutation.isError && (
        <Alert variant="danger">{t("algorithms:check_new.import_error")}</Alert>
      )}
    </>
  );

  return (
    <MultiSelectorModal<CatalogAlgorithm, string>
      show={show}
      onHide={onHide}
      onAdd={(execClasses) => importMutation.mutate(execClasses)}
      title={t("algorithms:check_new.title")}
      noneMessage={t("algorithms:check_new.none_found")}
      confirmLabel={(count) => t("algorithms:check_new.import_selected", { count })}
      cancelLabel={t("algorithms:check_new.close")}
      confirmPending={importMutation.isPending}
      closeOnConfirm={false}
      banner={banner}
      data={available}
      loading={isLoading}
      getId={(a) => a.execClass}
      columns={columns}
      selectAllAriaLabel={t("algorithms:check_new.select_all")}
      rowSelectAriaLabel={(a) => a.name}
    />
  );
};

import { useCallback, useMemo, useState } from "react";
import { Form } from "react-bootstrap";
import { renderToString } from "react-dom/server";
import { useTranslation } from "react-i18next";
import type { ApiNetListItem, ApiPlatformRef } from "opendcs-api";
import {
  AppDataTable,
  type ColumnDef,
  type HeaderButton,
} from "../../../components/data-table";
import {
  platformCandidates,
  toNetlistItem,
  type PlatformCandidate,
} from "./netlistPlatforms";
import { NetlistPlatformsModal } from "./NetlistPlatformsModal";

export interface NetlistItemsTableProperties {
  items: ApiNetListItem[];
  edit?: boolean;
  /** All platforms, for the "Select platforms" chooser. */
  platforms?: ApiPlatformRef[];
  platformsLoading?: boolean;
  /** The netlist's transport medium type — decides each platform's transport id. */
  transportMediumType?: string;
  /** The netlist's preferred site name type — decides each item's platform name. */
  siteNameTypePref?: string;
  onSave: (item: ApiNetListItem) => void;
  onRemove: (transportId: string) => void;
}

const NO_PLATFORMS: ApiPlatformRef[] = [];

const ROW_ID = (item: ApiNetListItem) => item.transportId ?? "";

export const NetlistItemsTable: React.FC<NetlistItemsTableProperties> = ({
  items,
  edit = false,
  platforms = NO_PLATFORMS,
  platformsLoading = false,
  transportMediumType,
  siteNameTypePref,
  onSave,
  onRemove,
}) => {
  const [t] = useTranslation(["netlists", "translation"]);
  const [showSelect, setShowSelect] = useState(false);

  const candidates = useMemo(
    () => platformCandidates(platforms, transportMediumType, siteNameTypePref),
    [platforms, transportMediumType, siteNameTypePref],
  );
  const existingTransportIds = useMemo(
    () => items.map((i) => i.transportId ?? ""),
    [items],
  );

  const addSelected = useCallback(
    (selected: PlatformCandidate[]) =>
      selected.forEach((c) => onSave(toNetlistItem(c))),
    [onSave],
  );

  // Items are only added through the platform chooser.
  const extraHeaderButtons = useMemo<HeaderButton[]>(() => {
    if (!edit) return [];
    return [
      {
        text: t("netlists:items.select_platforms"),
        ariaLabel: t("netlists:items.select_platforms"),
        icon: "bi-list-check",
        onClick: () => setShowSelect(true),
      },
    ];
  }, [edit, t]);

  // Transport id and platform name come from the selected platform, so only
  // the description is editable.
  const columns = useMemo<ColumnDef<ApiNetListItem>[]>(
    () => [
      {
        data: "transportId",
        header: t("netlists:items.transportId"),
        type: "string",
        defaultContent: "",
      },
      {
        data: "platformName",
        header: t("netlists:items.platformName"),
        type: "string",
        defaultContent: "",
      },
      {
        data: "description",
        header: t("netlists:items.description"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row) =>
            renderToString(
              <Form.Control
                type="text"
                name="description"
                defaultValue={row.description ?? ""}
                aria-label={t("netlists:items.description_input", {
                  name: row.transportId ?? "",
                })}
              />,
            ),
          read: (cell) =>
            cell.querySelector<HTMLInputElement>('input[name="description"]')?.value ??
            "",
        },
      },
    ],
    [t],
  );

  return (
    <>
      <AppDataTable<ApiNetListItem, string>
        data={items}
        getId={ROW_ID}
        columns={columns}
        caption={t("netlists:items.title")}
        actionsLabel={t("translation:actions")}
        extraHeaderButtons={extraHeaderButtons}
        inlineEdit={
          edit
            ? {
                onSave: (original, updated) => {
                  if (!original.transportId) return false;
                  onSave({
                    transportId: original.transportId,
                    platformName: original.platformName ?? "",
                    description: updated.description ?? "",
                  });
                },
                onRemove: (row) => {
                  if (row.transportId) onRemove(row.transportId);
                },
                labels: {
                  edit: (r) =>
                    t("netlists:items.edit", { transportId: r.transportId ?? "" }),
                  remove: (r) =>
                    t("netlists:items.remove", { transportId: r.transportId ?? "" }),
                  save: (r) =>
                    t("netlists:items.save_edit", {
                      transportId: r.transportId ?? "",
                    }),
                  cancel: () => t("translation:cancel"),
                },
              }
            : undefined
        }
        dataTableOptions={{
          paging: false,
          scrollY: "calc(10 * 2rem)",
          scrollCollapse: true,
        }}
        tableId="netlistItemsTable"
      />
      <NetlistPlatformsModal
        show={showSelect}
        onHide={() => setShowSelect(false)}
        candidates={candidates}
        loading={platformsLoading}
        mediumType={transportMediumType}
        existingTransportIds={existingTransportIds}
        onAdd={addSelected}
      />
    </>
  );
};

export default NetlistItemsTable;

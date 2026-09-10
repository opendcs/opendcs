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
  candidatesByTransportId,
  platformCandidates,
  toNetlistItem,
  withPlatformDefaults,
  type PlatformCandidate,
} from "./netlistPlatforms";
import { NetlistPlatformsModal } from "./NetlistPlatformsModal";

export interface NetlistItemsTableProperties {
  items: ApiNetListItem[];
  edit?: boolean;
  /** All platforms, for the "Select platforms" chooser and manual-add fill-in. */
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

const itemDisplayName = (row: ApiNetListItem, rowId: string): string =>
  row.transportId || rowId.replace("__appdt_new_", "") || "";

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
  const byTransportId = useMemo(
    () => candidatesByTransportId(candidates),
    [candidates],
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

  const columns = useMemo<ColumnDef<ApiNetListItem>[]>(
    () => [
      {
        data: "transportId",
        header: t("netlists:items.transportId"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Control
                type="text"
                name="transportId"
                defaultValue={row.transportId ?? ""}
                aria-label={t("netlists:items.transportId_input", {
                  name: itemDisplayName(row, rowId),
                })}
              />,
            ),
          read: (cell) =>
            cell
              .querySelector<HTMLInputElement>('input[name="transportId"]')
              ?.value.trim() ?? "",
        },
      },
      {
        data: "platformName",
        header: t("netlists:items.platformName"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Control
                type="text"
                name="platformName"
                defaultValue={row.platformName ?? ""}
                aria-label={t("netlists:items.platformName_input", {
                  name: itemDisplayName(row, rowId),
                })}
              />,
            ),
          read: (cell) =>
            cell.querySelector<HTMLInputElement>('input[name="platformName"]')?.value ??
            "",
        },
      },
      {
        data: "description",
        header: t("netlists:items.description"),
        type: "string",
        defaultContent: "",
        edit: {
          render: (row, rowId) =>
            renderToString(
              <Form.Control
                type="text"
                name="description"
                defaultValue={row.description ?? ""}
                aria-label={t("netlists:items.description_input", {
                  name: itemDisplayName(row, rowId),
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

  // Only the transport id is required; name and description fill in from the
  // matching platform when left blank.
  const toItem = (row: ApiNetListItem, transportId: string) =>
    withPlatformDefaults(
      {
        transportId,
        platformName: row.platformName ?? "",
        description: row.description ?? "",
      },
      byTransportId,
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
                  const transportId = updated.transportId?.trim();
                  if (!transportId) return false;
                  // Items are keyed by transport id, so a changed id replaces
                  // the old entry rather than adding a second one.
                  if (
                    original.transportId &&
                    original.transportId.toUpperCase() !== transportId.toUpperCase()
                  ) {
                    onRemove(original.transportId);
                  }
                  onSave(toItem(updated, transportId));
                },
                onAdd: (created, rowEl) => {
                  const transportId = created.transportId?.trim();
                  if (!transportId) {
                    // Flag just the required field, not every blank input.
                    rowEl
                      .querySelector('input[name="transportId"]')
                      ?.classList.add("border-warning");
                    return "marked";
                  }
                  onSave(toItem(created, transportId));
                },
                onRemove: (row) => {
                  if (row.transportId) onRemove(row.transportId);
                },
                newTemplate: () => ({
                  transportId: "",
                  platformName: "",
                  description: "",
                }),
                labels: {
                  edit: (r, rowId) =>
                    t("netlists:items.edit", {
                      transportId: itemDisplayName(r, rowId),
                    }),
                  remove: (r, rowId) =>
                    t("netlists:items.remove", {
                      transportId: itemDisplayName(r, rowId),
                    }),
                  save: (r, rowId) =>
                    t("netlists:items.save_edit", {
                      transportId: itemDisplayName(r, rowId),
                    }),
                  cancel: (r, rowId) =>
                    rowId.startsWith("__appdt_new_")
                      ? t("netlists:items.remove", {
                          transportId: itemDisplayName(r, rowId),
                        })
                      : t("translation:cancel"),
                  add: t("netlists:items.add"),
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

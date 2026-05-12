import { useEffect, type RefObject } from "react";
import type { DataTableRef } from "datatables.net-react";

/**
 * Toggles DataTables' built-in "Processing..." overlay based on a loading flag.
 *
 * The DataTable option `processing: true` must also be set so the feature is
 * enabled. The `processing()` runtime API isn't exposed on the official TS
 * typings so we narrow via `unknown` here.
 */
export function useTableProcessing(
  tableRef: RefObject<DataTableRef | null>,
  loading: boolean | undefined,
): void {
  useEffect(() => {
    (
      tableRef.current?.dt() as unknown as { processing?: (state: boolean) => void }
    )?.processing?.(loading ?? false);
  }, [tableRef, loading]);
}

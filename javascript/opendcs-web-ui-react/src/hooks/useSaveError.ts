import { useCallback, useState } from "react";
import { apiErrorMessage } from "../util/ApiError";

export interface SaveErrorState {
  /** Message from the last failed save, or null when there is nothing to show. */
  saveError: string | null;
  /** Clears the message — wire this to the alert's dismiss button. */
  clearSaveError: () => void;
  /**
   * Runs `save`, clearing any previous message first and capturing a failure
   * as user-facing text. Resolves true when the save completed, so callers
   * that need to close a form or navigate on success can branch on it.
   */
  attemptSave: (save: () => void | Promise<unknown>) => Promise<boolean>;
}

/**
 * The save-and-report-failure half of an edit form. Every editor page ran the
 * same block — clear the banner, await the save action, log, then translate the
 * error into something the user can read — so it lives here once. Pair it with
 * <SaveErrorAlert/> for the display half.
 *
 * @param fallback Message shown when the failure carries no usable text of its
 *   own (network error, empty body). Pass a translated string.
 * @param logLabel Prefix for the console.warn that records the raw error.
 */
export const useSaveError = (fallback: string, logLabel: string): SaveErrorState => {
  const [saveError, setSaveError] = useState<string | null>(null);

  const clearSaveError = useCallback(() => setSaveError(null), []);

  const attemptSave = useCallback(
    async (save: () => void | Promise<unknown>): Promise<boolean> => {
      setSaveError(null);
      try {
        await save();
        return true;
      } catch (err) {
        console.warn(logLabel, err);
        setSaveError(apiErrorMessage(err, fallback));
        return false;
      }
    },
    [fallback, logLabel],
  );

  return { saveError, clearSaveError, attemptSave };
};

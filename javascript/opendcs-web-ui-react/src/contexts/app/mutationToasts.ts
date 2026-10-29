import type { Mutation } from "@tanstack/react-query";
import { apiErrorMessage } from "../../util/ApiError";
import { pushToast } from "../../util/toastBus";

/** i18n keys a mutation opts into via `meta`; mutations without them stay silent. */
export interface ToastMeta extends Record<string, unknown> {
  successToast?: string;
  errorToast?: string;
}

declare module "@tanstack/react-query" {
  interface Register {
    mutationMeta: ToastMeta;
  }
}

export const DELETE_TOASTS: ToastMeta = {
  successToast: "translation:delete_success",
  errorToast: "translation:delete_failed",
};

type AnyMutation = Mutation<unknown, unknown, unknown, unknown>;

export const onMutationSuccess = (
  _data: unknown,
  _variables: unknown,
  _context: unknown,
  mutation: AnyMutation,
) => {
  const title = mutation.meta?.successToast;
  if (title) pushToast({ variant: "success", title });
};

export const onMutationError = (
  error: unknown,
  _variables: unknown,
  _context: unknown,
  mutation: AnyMutation,
) => {
  console.error("[mutation]", error);
  const title = mutation.meta?.errorToast;
  if (!title) return;
  const detail = apiErrorMessage(error, "");
  pushToast({ variant: "danger", title, detail: detail || undefined });
};

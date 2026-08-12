import type { Mutation } from "@tanstack/react-query";
import { pushToast } from "../../lib/toastBus";

export const extractErrorMessage = (error: unknown): string => {
  // Prefer the parsed body.message from a Status JSON response (400/409/500 with schema).
  if (
    error != null &&
    typeof error === "object" &&
    "body" in error &&
    error.body != null &&
    typeof error.body === "object" &&
    "message" in error.body &&
    typeof (error.body as { message?: unknown }).message === "string"
  ) {
    return (error.body as { message: string }).message;
  }
  // ApiException.message is a multi-line dump ("HTTP-Code: N\nMessage: X\n...").
  // Extract just the "Message: X" line so toasts stay readable.
  if (error instanceof Error) {
    const match = error.message.match(/^Message:(.*)$/m);
    if (match) return match[1].trim();
    return error.message;
  }
  return "An unexpected error occurred.";
};

export const onMutationError = (
  error: unknown,
  _variables: unknown,
  _context: unknown,
  mutation: Mutation<unknown, unknown, unknown, unknown>,
) => {
  console.error("[mutation]", error);
  const prefix = (mutation.options.meta as { errorContext?: string } | undefined)
    ?.errorContext;
  const reason = extractErrorMessage(error);
  const message = prefix ? `${prefix}: ${reason}` : reason;
  pushToast(message, "danger");
};

export const onMutationSuccess = (
  _data: unknown,
  _variables: unknown,
  _context: unknown,
  mutation: Mutation<unknown, unknown, unknown, unknown>,
) => {
  const successMessage = (
    mutation.options.meta as { successMessage?: string } | undefined
  )?.successMessage;
  if (successMessage) {
    pushToast(successMessage, "success");
  }
};

import { ApiException } from "opendcs-api";

/**
 * Pulls a user-facing message out of a failed save. The API reports validation
 * problems as an `ApiException` whose parsed body carries a `message`; anything
 * else (network failure, unparsed body, blank message) falls back to the
 * caller-supplied generic text.
 */
export const saveErrorMessage = (err: unknown, fallback: string): string => {
  if (err instanceof ApiException && err.body && typeof err.body === "object") {
    const message = (err.body as { message?: unknown }).message;
    if (typeof message === "string" && message.trim().length > 0) return message;
  }
  return fallback;
};

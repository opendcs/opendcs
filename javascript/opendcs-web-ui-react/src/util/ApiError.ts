import { ApiException } from "opendcs-api";

export const apiErrorMessage = (err: unknown, fallback: string): string => {
  if (!(err instanceof ApiException) || !err.body) return fallback;
  let body: unknown = err.body;
  if (typeof body === "string") {
    try {
      body = JSON.parse(body);
    } catch {
      return fallback;
    }
  }
  if (body && typeof body === "object") {
    const message = (body as { message?: unknown }).message;
    if (typeof message === "string" && message.trim().length > 0) return message;
  }
  return fallback;
};

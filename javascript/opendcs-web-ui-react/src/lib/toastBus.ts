export type ToastVariant = "success" | "danger" | "warning" | "info";

export interface ToastMessage {
  id: number;
  variant: ToastVariant;
  message: string;
  ttlMs: number;
}

let nextId = 1;
let toasts: ToastMessage[] = [];
const listeners = new Set<() => void>();

function notify() {
  listeners.forEach((l) => l());
}

/** For useSyncExternalStore — returns the current toast array reference. */
export function getToasts(): ToastMessage[] {
  return toasts;
}

/** For useSyncExternalStore — subscribe/unsubscribe pattern. */
export function subscribeToasts(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function pushToast(
  message: string,
  variant: ToastVariant = "info",
  ttlMs = variant === "danger" ? 8000 : 5000,
): void {
  const id = nextId++;
  toasts = [...toasts, { id, variant, message, ttlMs }];
  notify();
  setTimeout(() => {
    toasts = toasts.filter((t) => t.id !== id);
    notify();
  }, ttlMs);
}

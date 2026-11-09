export type ToastVariant = "success" | "danger";

export interface Toast {
  id: number;
  variant: ToastVariant;
  /** i18n key for the headline; ToastStack translates it at render time. */
  title: string;
  /** Untranslated text from the server, shown under the headline. */
  detail?: string;
}

const TTL_MS: Record<ToastVariant, number> = { success: 5000, danger: 8000 };

let nextId = 1;
let toasts: readonly Toast[] = [];
const listeners = new Set<() => void>();

const notify = () => listeners.forEach((listener) => listener());

/** Snapshot for useSyncExternalStore; the reference only changes on push/dismiss. */
export const getToasts = (): readonly Toast[] => toasts;

export const subscribeToasts = (listener: () => void): (() => void) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

export const dismissToast = (id: number) => {
  if (!toasts.some((t) => t.id === id)) return;
  toasts = toasts.filter((t) => t.id !== id);
  notify();
};

export const pushToast = (toast: Omit<Toast, "id">) => {
  const id = nextId++;
  toasts = [...toasts, { ...toast, id }];
  notify();
  setTimeout(() => dismissToast(id), TTL_MS[toast.variant]);
};

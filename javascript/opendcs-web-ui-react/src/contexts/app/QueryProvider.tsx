import { useState, type ReactNode } from "react";
import {
  MutationCache,
  QueryCache,
  QueryClient,
  QueryClientProvider,
  type Mutation,
} from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { QueryErrorBoundary } from "../../components/QueryErrorBoundary";
import { pushToast } from "../../lib/toastBus";

interface ProviderProps {
  children: ReactNode;
}

// Single integration point for a future toast/snackbar layer.
const logError = (scope: "query" | "mutation") => (error: unknown) => {
  console.error(`[${scope}]`, error);
};

const extractErrorMessage = (error: unknown): string => {
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
    const match = error.message.match(/^Message:\s*(.+)$/m);
    if (match) return match[1].trim();
    return error.message;
  }
  return "An unexpected error occurred.";
};

const onMutationError = (
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

const onMutationSuccess = (
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

export const QueryProvider = ({ children }: ProviderProps) => {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30_000,
            gcTime: 5 * 60_000,
            refetchOnWindowFocus: false,
            retry: 1,
          },
          mutations: {
            retry: 0,
          },
        },
        queryCache: new QueryCache({ onError: logError("query") }),
        mutationCache: new MutationCache({
          onError: onMutationError,
          onSuccess: onMutationSuccess,
        }),
      }),
  );

  return (
    <QueryClientProvider client={client}>
      <QueryErrorBoundary>{children}</QueryErrorBoundary>
      {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
    </QueryClientProvider>
  );
};

import { useState, type ReactNode } from "react";
import {
  MutationCache,
  QueryCache,
  QueryClient,
  QueryClientProvider,
} from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { QueryErrorBoundary } from "../../components/QueryErrorBoundary";
import { ToastStack } from "../../components/ToastStack";
import { onMutationError, onMutationSuccess } from "./mutationToasts";

interface ProviderProps {
  children: ReactNode;
}

const logQueryError = (error: unknown) => {
  console.error("[query]", error);
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
        queryCache: new QueryCache({ onError: logQueryError }),
        // Mutations opt into a toast through `meta` (see mutationToasts.ts).
        mutationCache: new MutationCache({
          onError: onMutationError,
          onSuccess: onMutationSuccess,
        }),
      }),
  );

  return (
    <QueryClientProvider client={client}>
      <QueryErrorBoundary>{children}</QueryErrorBoundary>
      <ToastStack />
      {/* Bottom-left so it doesn't cover the toasts' close buttons. */}
      {import.meta.env.DEV && (
        <ReactQueryDevtools initialIsOpen={false} buttonPosition="bottom-left" />
      )}
    </QueryClientProvider>
  );
};

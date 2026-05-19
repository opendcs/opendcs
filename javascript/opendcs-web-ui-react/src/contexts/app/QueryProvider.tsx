import { useState, type ReactNode } from "react";
import {
  MutationCache,
  QueryCache,
  QueryClient,
  QueryClientProvider,
} from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { QueryErrorBoundary } from "../../components/QueryErrorBoundary";

interface ProviderProps {
  children: ReactNode;
}

// Single integration point for a future toast/snackbar layer.
const logError = (scope: "query" | "mutation") => (error: unknown) => {
  console.error(`[${scope}]`, error);
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
        mutationCache: new MutationCache({ onError: logError("mutation") }),
      }),
  );

  return (
    <QueryClientProvider client={client}>
      <QueryErrorBoundary>{children}</QueryErrorBoundary>
      {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
    </QueryClientProvider>
  );
};

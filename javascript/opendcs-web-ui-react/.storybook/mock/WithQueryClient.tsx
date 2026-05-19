import { Decorator } from "@storybook/react-vite";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useMemo } from "react";

// Fresh client per story so cache state never leaks between stories or test
// runs. staleTime: Infinity so data seeded via setQueryData (e.g. by
// WithUnits) isn't refetched on mount — otherwise an unmocked refetch would
// race the play function and flip isSuccess back to false.
export const WithQueryClient: Decorator = (Story) => {
  const client = useMemo(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { retry: false, staleTime: Infinity, gcTime: Infinity },
          mutations: { retry: false },
        },
      }),
    [],
  );
  return (
    <QueryClientProvider client={client}>
      <Story />
    </QueryClientProvider>
  );
};

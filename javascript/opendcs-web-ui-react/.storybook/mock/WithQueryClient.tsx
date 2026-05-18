import { Decorator } from "@storybook/react-vite";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useMemo } from "react";

// Fresh client per story so cache state never leaks between stories or test
// runs. retry/staleTime are zeroed to keep play-tests deterministic.
export const WithQueryClient: Decorator = (Story) => {
  const client = useMemo(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { retry: false, staleTime: 0, gcTime: 0 },
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

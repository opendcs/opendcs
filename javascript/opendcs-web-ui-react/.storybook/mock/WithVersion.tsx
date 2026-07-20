/**
 * Seeds the TanStack QueryClient with stub version data so any story that
 * renders AppVersion (via useVersionQuery) sees ready data without a
 * network round-trip.
 *
 * Assumes WithQueryClient is mounted higher in the decorator chain.
 */

import { Decorator } from "@storybook/react-vite";
import { useQueryClient } from "@tanstack/react-query";
import { useMemo } from "react";
import { versionKeys } from "../../src/queries/keys";
import type { ApiVersion } from "../../src/queries/version";

export const WithVersion: Decorator = (Story) => {
  const queryClient = useQueryClient();

  useMemo(() => {
    const version: ApiVersion = {
      version: "99.main-SNAPSHOT",
      commitHash: "0123456789abcdef0123456789abcdef01234567",
    };
    queryClient.setQueryData(versionKeys.all(), version);
  }, [queryClient]);

  return <Story />;
};

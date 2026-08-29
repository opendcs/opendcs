import {
  Configuration,
  DefaultApi,
  ResponseError,
  type DcpMessages,
  type StatusGroupSummary,
} from "opendcs-dds-api";
import { DCPMON_API_BASE_URL } from "./constants";

const ddsApi = new DefaultApi(
  new Configuration({
    basePath: DCPMON_API_BASE_URL,
  }),
);

async function requestDds<T>(request: () => Promise<T>): Promise<T> {
  try {
    return await request();
  } catch (error) {
    if (error instanceof ResponseError) {
      throw new Error(
        `${error.response.status} ${error.response.statusText}`.trim(),
        { cause: error },
      );
    }
    throw error;
  }
}

export async function getStatusGroupSummary(
  group: string,
): Promise<StatusGroupSummary> {
  return requestDds(() =>
    ddsApi.dataSummaryGet({ dataGroup: group }),
  );
}

export async function getDcpMessages(
  dcpAddress: string,
): Promise<DcpMessages | null | undefined> {
  return requestDds(() =>
    ddsApi.dataQueryGet({
      source: ["GOES"],
      dcpAddress: [dcpAddress],
    }),
  );
}

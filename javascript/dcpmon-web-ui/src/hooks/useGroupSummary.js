/**
 * @typedef {import('dds-api').DdsDataQueryGetRequest} DdsDataQueryGetRequest
 * @typedef {import('@tanstack/react-query').UseQueryOptions} UseQueryOptions
 * @typedef {import('@tanstack/react-query').UseQueryResult} UseQueryResult
 * @typedef {import('axios').AxiosRequestConfig} AxiosRequestConfig
 */

import { useQuery } from '@tanstack/react-query';
import { DefaultApi } from 'dds-api';

/**
 * @param {{
 *   dataParams: DdsDataQueryGetRequest,
 *   queryParams?: UseQueryOptions,
 *   axiosRequestConfig?: AxiosRequestConfig
 * }} params
 * @returns {UseQueryResult<any, unknown>}
 */
function useGroupSummary({ dataParams, queryParams, axiosRequestConfig }) {
  return useQuery({
    queryKey: ['groupSummaryQuery', ...Object.values(dataParams)],
    queryFn: async () => {
      const api = new DefaultApi();
      return api.ddsDataSummaryGet(dataParams, axiosRequestConfig);
    },
    ...queryParams,
  });
}

export default useGroupSummary;
export { useGroupSummary };

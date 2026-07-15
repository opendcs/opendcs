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
function useDataQuery({ dataParams, queryParams, axiosRequestConfig }) {
  return useQuery({
    queryKey: ['dataQuery', ...Object.values(dataParams)],
    queryFn: async () => {
      const api = new DefaultApi();
      return api.ddsDataQueryGet(dataParams, axiosRequestConfig);
    },
    ...queryParams,
  });
}

export default useDataQuery;
export { useDataQuery };

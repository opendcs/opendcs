import { http, HttpResponse, passthrough } from "msw";
import { LRGS_DOMAIN } from "../../constants";
import schema_channel from "../schema/channel/index.json";
import { getDcpByAddress } from "../utils/dcp";
const groups = [
  // And here's a request handler with MSW
  // for the same "GET /user" request that
  // responds with a mock JSON response.
  http.get(`${LRGS_DOMAIN}/channel`, ({ request, params, cookies }) => {
    return HttpResponse.json(schema_channel);
  }),

  http.get(
    `${LRGS_DOMAIN}/channel/:channel`,
    async ({ request, params, cookies }) => {
      const { group } = params;
      const groupData = group ? await getDcpByAddress("KEYS") : schema_channel;
      if (groupData.error) {
        return HttpResponse.json({ message: groupData.error }, { status: 404 });
      }

      return HttpResponse.json(groupData);
    }
  ),

  // Although this handler also matches the request,
  // it will never be called because the previous handler
  // returned a mocked response.
  http.get(`${LRGS_DOMAIN}/channel`, () => passthrough()),
];

export default groups;
export { groups };

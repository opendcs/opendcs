import { http, HttpResponse } from "msw";
import { LRGS_DOMAIN } from "../../constants";
import index_schema from "../schema/dcp";

const index = [
  // And here's a request handler with MSW
  // for the same "GET /user" request that
  // responds with a mock JSON response.
  http.get(`${LRGS_DOMAIN}/index`, ({ request }) => {
    return HttpResponse.json(index_schema);
  }),

  http.get(`${LRGS_DOMAIN}/index/{}`, ({ request }) => {
    return HttpResponse.json(index_schema);
  }),
];

export default index;
export { index };

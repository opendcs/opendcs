import { http, HttpResponse, passthrough } from "msw";
import { LRGS_DOMAIN } from "../../constants";
import schema_group from "../schema/group/index.json";

const groups = [
  // And here's a request handler with MSW
  // for the same "GET /user" request that
  // responds with a mock JSON response.
  http.get(`${LRGS_DOMAIN}/groups`, ({ request, params, cookies }) => {
    return HttpResponse.json(schema_group);
  }),

  http.get(
    `${LRGS_DOMAIN}/groups/:group`,
    async ({ request, params, cookies }) => {
      const { group } = params;
      const groupData = group
        ? schema_group.map((g) => {
            if (g?.id?.toLowerCase() == group) return g;
          })
        : schema_group;
      if (groupData.error) {
        return HttpResponse.json({ message: groupData.error }, { status: 404 });
      }

      console.log({ groupData });

      return HttpResponse.json(groupData);
    }
  ),

  // Although this handler also matches the request,
  // it will never be called because the previous handler
  // returned a mocked response.
  http.get("/groups", () => passthrough()),
];

export default groups;
export { groups };

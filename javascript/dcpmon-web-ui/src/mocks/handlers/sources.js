import { http, HttpResponse } from "msw";
import mockSources from "../schema/dataSources.json";
import { LRGS_DOMAIN } from "../../constants";

export const sourceHandlers = [
  http.get(`${LRGS_DOMAIN}/sources`, () => {
    return HttpResponse.json(mockSources);
  })
];

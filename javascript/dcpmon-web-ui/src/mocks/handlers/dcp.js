import { http, HttpResponse } from "msw";
import mockMessages from "../schema/goesMessage.json";
import { LRGS_DOMAIN } from "../../constants";

export const dcpHandlers = [
  http.get(`${LRGS_DOMAIN}/data/next`, () => {
    return HttpResponse.json(mockMessages);
  }),

  http.post(`${LRGS_DOMAIN}/data/search`, async ({ request }) => {
    const body = await request.json();
    console.log("Mock search criteria:", body);
    return HttpResponse.text("mock-criteria-id", { status: 201 });
  })
];

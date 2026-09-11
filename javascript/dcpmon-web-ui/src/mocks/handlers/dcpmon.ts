import { http, HttpResponse } from "msw";
import { DCPMON_API_BASE_URL } from "../../constants";
import { goesMessages } from "../data/goesMessages";
import { statusGroupSummary } from "../data/statusGroupSummary";

export const dcpmonHandlers = [
  http.get(`${DCPMON_API_BASE_URL}/data/summary`, ({ request }) => {
    const url = new URL(request.url);
    const group = url.searchParams.get("group");

    if (!group) {
      return HttpResponse.text("Missing 'group' query parameter", { status: 400 });
    }

    if (group.toLowerCase() !== "swt") {
      return HttpResponse.text(`Group (${group}) Not Implemented`, { status: 404 });
    }

    return HttpResponse.json(statusGroupSummary);
  }),

  http.get(`${DCPMON_API_BASE_URL}/data/query`, ({ request }) => {
    const url = new URL(request.url);
    const source = url.searchParams.get("source");
    const dcpAddress = url.searchParams.get("dcpAddress");

    if (!source) {
      return HttpResponse.text("Missing 'source' query parameter", { status: 400 });
    }

    if (!dcpAddress) {
      return HttpResponse.text("Missing 'dcpAddress' query parameter", {
        status: 400,
      });
    }

    if (source.toLowerCase() !== "goes") {
      return HttpResponse.text(`Source (${source}) Not Implemented`, {
        status: 404,
      });
    }

    return HttpResponse.json(goesMessages);
  }),
];

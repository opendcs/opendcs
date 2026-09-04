import { StatusGroupSummaryToJSON } from "opendcs-dds-api";
import { HttpResponse, http } from "msw";
import { DCPMON_API_BASE_URL } from "../../constants";
import { goesMessages } from "../data/goesMessages";
import { statusGroupSummary } from "../data/statusGroupSummary";

export const dcpmonHandlers = [
  http.get(`${DCPMON_API_BASE_URL}/groups`, () =>
    HttpResponse.json([
      { id: "SWT", displayName: "Tulsa District" },
      { id: "NAE", displayName: "New England District" },
    ]),
  ),

  http.get(`${DCPMON_API_BASE_URL}/data/summary`, ({ request }) => {
    const url = new URL(request.url);
    const group = url.searchParams.get("data-group");

    if (!group) {
      return HttpResponse.text("Missing 'data-group' query parameter", {
        status: 400,
      });
    }

    if (!["swt", "nae"].includes(group.toLowerCase())) {
      return HttpResponse.text(`Group (${group}) Not Implemented`, { status: 404 });
    }

    return HttpResponse.json(StatusGroupSummaryToJSON(statusGroupSummary));
  }),

  http.get(`${DCPMON_API_BASE_URL}/data/query`, ({ request }) => {
    const url = new URL(request.url);
    const source = url.searchParams.get("source");
    const dcpAddress = url.searchParams.get("dcpAddress");
    const since = url.searchParams.get("since");

    if (!source) {
      return HttpResponse.text("Missing 'source' query parameter", { status: 400 });
    }

    if (!dcpAddress) {
      return HttpResponse.text("Missing 'dcpAddress' query parameter", {
        status: 400,
      });
    }

    if (!since || Number.isNaN(Date.parse(since))) {
      return HttpResponse.text("Missing 24-hour query window", { status: 400 });
    }

    if (source.toLowerCase() !== "goes") {
      return HttpResponse.text(`Source (${source}) Not Implemented`, {
        status: 404,
      });
    }

    return HttpResponse.json(goesMessages);
  }),
];

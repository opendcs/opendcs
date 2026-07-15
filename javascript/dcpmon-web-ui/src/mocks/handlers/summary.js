import { http, HttpResponse } from "msw";
import statusGroupSummary from "../schema/status-group-summary.json";
import { LRGS_DOMAIN } from "../../constants";

export const summaryHandlers = [
    http.get(`${LRGS_DOMAIN}/data/summary`, ({ request }) => {
        const url = new URL(request.url);
        const group = url.searchParams.get("group");
        if (!group) {
            // set status code for invalid input
            return HttpResponse.text("Missing 'group' query parameter", { status: 400 });
        }

        switch (group.toLowerCase()) {
            case "swt":
                return HttpResponse.json(statusGroupSummary)
            default:
                return HttpResponse.text(`Group (${group}) Not Implemented`, { status: 404 });
        }
    })
]

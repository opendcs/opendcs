import { http, HttpResponse } from "msw";
import goesMock from "../schema/goesMessage.json";
import iridiumMock from "../schema/iridiumMessage.json";
import { LRGS_DOMAIN } from "../../constants";

export const queryHandlers = [
    http.get(`${LRGS_DOMAIN}/data/query`, ({ request }) => {
        const url = new URL(request.url);
        const source = url.searchParams.get("source");
        if (!source) {
            // set status code for invalid input
            return HttpResponse.text("Missing 'source' query parameter", { status: 400 });
        }

        switch (source.toLowerCase()) {
            case "iridium":
                return HttpResponse.json(iridiumMock)
            case "goes":
                return HttpResponse.json(goesMock);
            default:
                return HttpResponse.text(`Source (${source}) Not Implemented`, { status: 404 });
        }
    })
]

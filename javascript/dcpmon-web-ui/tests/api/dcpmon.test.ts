import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";
import { DCPMON_API_BASE_URL } from "../../src/constants";
import { getDcpMessages, getStatusGroupSummary } from "../../src/api";
import { server } from "../../src/mocks/server";

describe("DCPMon mock API", () => {
  it("returns the SWT status group summary through the DCPMon API client", async () => {
    const summary = await getStatusGroupSummary("swt");

    expect(summary.group).toBe("SWT");
    expect(summary.locations).toHaveLength(3);
    expect(summary.locations[0]).toMatchObject({
      stationId: "NMBA4",
      dcpAddress: "CE1F40D4",
    });
  });

  it("returns GOES messages through the DCPMon API client", async () => {
    const messages = await getDcpMessages("CE1F40D4");

    expect(messages.total).toBe(2);
    expect(messages.messages[0]).toMatchObject({
      channel: "162W",
      quality: "N",
    });
  });

  it("surfaces mock API errors to callers", async () => {
    server.use(
      http.get(`${DCPMON_API_BASE_URL}/data/summary`, () =>
        HttpResponse.text("No mock for this group", { status: 404 }),
      ),
    );

    await expect(getStatusGroupSummary("unknown")).rejects.toThrow("404");
  });
});

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { DcpMonDashboard } from "./DcpMonDashboard";
import { DcpMonTopBar } from "./DcpMonTopBar";
import { DisplaySettingsProvider } from "../DisplaySettingsContext";

function renderDashboard() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <DisplaySettingsProvider>
        <DcpMonDashboard />
      </DisplaySettingsProvider>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  localStorage.clear();
  document.documentElement.removeAttribute("data-bs-theme");
  document.documentElement.style.colorScheme = "";
});

describe("DcpMonDashboard", () => {
  it("uses the OpenDCS application header", () => {
    render(
      <DisplaySettingsProvider>
        <DcpMonTopBar />
      </DisplaySettingsProvider>,
    );

    expect(screen.getByText("OpenDCS")).toBeVisible();
    expect(screen.getByText("DCP Monitor")).toBeVisible();
    expect(screen.getByRole("button", { name: "Switch to dark mode" })).toBeVisible();
  });

  it("uses system appearance by default and toggles an explicit theme", async () => {
    const user = userEvent.setup();
    render(
      <DisplaySettingsProvider>
        <DcpMonTopBar />
      </DisplaySettingsProvider>,
    );

    expect(document.documentElement).toHaveAttribute("data-bs-theme", "light");
    await user.click(screen.getByRole("button", { name: "Switch to dark mode" }));
    expect(document.documentElement).toHaveAttribute("data-bs-theme", "dark");
    expect(localStorage.getItem("opendcs.dcpmon.display-settings")).toContain(
      '"theme":"dark"',
    );
    expect(screen.getByRole("button", { name: "Switch to light mode" })).toBeVisible();
  });

  it("renders the mocked status summary and stations", async () => {
    renderDashboard();

    expect(await screen.findByRole("heading", { name: "DCPMon" })).toBeVisible();
    expect(screen.getByText(/Group SWT for the last 24 hours/)).toBeVisible();
    expect(screen.getByRole("searchbox", { name: "Search locations" })).toBeVisible();
    expect(screen.getByRole("button", { name: /Complete data/ })).toBeVisible();
    expect(screen.queryByRole("button", { name: /Unknown schedule/ })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Low battery.*1 locations/ })).toBeVisible();
    expect(screen.getByRole("button", { name: /GPS sync issues.*1 locations/ })).toBeVisible();
    expect(screen.getByRole("heading", { name: "Status legend" })).toBeVisible();
    expect(
      screen.getByText("All expected transmissions received with no parity failures."),
    ).toBeVisible();
  });

  it("loads mocked GOES messages when a station opens", async () => {
    const user = userEvent.setup();
    renderDashboard();

    const search = await screen.findByRole("searchbox", { name: "Search locations" });
    await user.type(search, "NIMB");
    await user.click(await screen.findByRole("button", { name: /CE1F40D4/ }));

    const table = await screen.findByRole("table");
    expect(within(table).getAllByText("162W")).toHaveLength(2);
    expect(within(table).getAllByText(/749\.73/).length).toBeGreaterThan(0);
    expect(within(table).getByText("Missing transmission")).toBeVisible();
    expect(within(table).getByText("Transmit Time (GMT)")).toBeVisible();
  });

  it("changes timestamp display settings and labels the alternate timezone", async () => {
    const user = userEvent.setup();
    renderDashboard();

    await screen.findByRole("heading", { name: "DCPMon" });
    const firstTimestamp = document.querySelector("time");
    expect(firstTimestamp).not.toBeNull();
    await user.hover(firstTimestamp!);
    expect(await screen.findByText(/Local time \(/)).toBeVisible();

    await user.click(screen.getByRole("button", { name: "Settings" }));
    const timeZone = screen.getByRole("combobox", { name: "Display timezone" });
    await user.clear(timeZone);
    await user.type(timeZone, "America/New_York");
    await user.click(screen.getByRole("button", { name: "Save settings" }));

    expect(localStorage.getItem("opendcs.dcpmon.display-settings")).toContain(
      "America/New_York",
    );
    await user.hover(document.querySelector("time")!);
    expect(await screen.findByText("GMT")).toBeVisible();
  });

  it("searches by configured identifiers", async () => {
    const user = userEvent.setup();
    renderDashboard();

    const search = await screen.findByRole("searchbox", { name: "Search locations" });
    await user.type(search, "BMRA4");

    expect(await screen.findByText("1 locations")).toBeVisible();
    expect(screen.getByRole("button", { name: /CE1F2532/ })).toBeVisible();
  });

  it("discovers and switches configured DCP groups", async () => {
    const user = userEvent.setup();
    renderDashboard();

    const groupSelect = await screen.findByRole("combobox", { name: "Group" });
    expect(groupSelect).toHaveValue("SWT");
    expect(within(groupSelect).getByRole("option", { name: "New England District" })).toBeVisible();

    await user.selectOptions(groupSelect, "NAE");
    expect(await screen.findByText(/Group NAE for the last 24 hours/)).toBeVisible();
  });
});

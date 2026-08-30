import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { DcpMonDashboard } from "./DcpMonDashboard";
import { DcpMonTopBar } from "./DcpMonTopBar";

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
      <DcpMonDashboard />
    </QueryClientProvider>,
  );
}

describe("DcpMonDashboard", () => {
  it("uses the OpenDCS application header", () => {
    render(<DcpMonTopBar />);

    expect(screen.getByText("OpenDCS")).toBeVisible();
    expect(screen.getByText("DCP Monitor")).toBeVisible();
  });

  it("renders the mocked status summary and stations", async () => {
    renderDashboard();

    expect(await screen.findByRole("heading", { name: "DCPMon" })).toBeVisible();
    expect(screen.getByText(/Group SWT for the last 24 hours/)).toBeVisible();
    expect(screen.getByRole("searchbox", { name: "Search locations" })).toBeVisible();
    expect(screen.getByRole("button", { name: /Complete data/ })).toBeVisible();
    expect(screen.getByText("Low battery: CE1F40D4")).toBeVisible();
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

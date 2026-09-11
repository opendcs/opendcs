import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { DcpMonDashboard } from "./DcpMonDashboard";

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
  it("renders the mocked status summary and stations", async () => {
    renderDashboard();

    expect(await screen.findByRole("heading", { name: "DCPMon" })).toBeVisible();
    expect(screen.getByText("Group SWT for the last 24 hours")).toBeVisible();
    expect(screen.getByText("CE1F40D4")).toBeVisible();
    expect(screen.getByText("BMRA4")).toBeVisible();
    expect(screen.getByText("Low battery: CE1F40D4")).toBeVisible();
  });

  it("loads mocked GOES messages when a station opens", async () => {
    const user = userEvent.setup();
    renderDashboard();

    await user.click(await screen.findByRole("button", { name: /CE1F40D4/ }));

    const table = await screen.findByRole("table");
    expect(within(table).getAllByText("162W")).toHaveLength(2);
    expect(within(table).getAllByText(/749\.73/).length).toBeGreaterThan(0);
  });
});

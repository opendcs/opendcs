import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
// import SiteWrapper from "./components/composite/SiteWrapper.jsx";
import { Route, Routes } from "react-router-dom";

import useGroupSummary from "./hooks/useGroupSummary.js";
import { GageAccordion } from "./components/GageAccordion.jsx";

import { AppSidebar } from "@/components/app-sidebar";
import { ChartAreaInteractive } from "@/components/chart-area-interactive";
// import { DataTable } from "@/components/data-table"
import { SectionCards } from "@/components/section-cards";
import { SiteHeader } from "@/components/site-header";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";

// Start MSW conditionally in dev
if (import.meta.env.DEV) {
  const setupMocks = async () => {
    const { worker } = await import("./mocks/browser");
    await worker.start({
      onUnhandledRequest: "bypass",
    });
  };
  setupMocks();
}

const queryClient = new QueryClient();

function App() {
  const swtGroup = useGroupSummary({ dataParams: { group: "swt" } });
  return (
    <QueryClientProvider client={queryClient}>
      {/* <SiteWrapper> */}
      <SidebarProvider
        style={{
          "--sidebar-width": "calc(var(--spacing) * 72)",
          "--header-height": "calc(var(--spacing) * 12)",
        }}
      >
        <AppSidebar variant="inset" />
        <SidebarInset>
          <SiteHeader  />
          <div className="flex flex-1 flex-col">
            <div className="@container/main flex flex-1 flex-col gap-2">
              <div className="flex flex-col gap-4 py-4 md:gap-6 md:py-6">
                <Routes>
                  <Route
                    path="/"
                    element={
                      <div className="p-5">
                        <h2>DCPMon</h2>
                        {swtGroup.data &&
                          swtGroup.data.data.locations.map((location) => {
                            return (
                              <GageAccordion
                                location={location}
                                key={location.stationId}
                              />
                            );
                          })}
                        {/*  {!form ? <ReportSelect onForm={setForm} /> : <Report />} */}
                      </div>
                    }
                  />
                </Routes>

                <SectionCards />
                <div className="px-4 lg:px-6">
                  <ChartAreaInteractive />
                </div>
                {/* <DataTable data={data} /> */}
              </div>
            </div>
          </div>
        </SidebarInset>
      </SidebarProvider>
      {/* </SiteWrapper> */}
    </QueryClientProvider>
  );
}

export default App;

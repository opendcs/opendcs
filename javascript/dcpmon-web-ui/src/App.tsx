import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import Container from "react-bootstrap/Container";
import { DcpMonDashboard } from "./components/DcpMonDashboard";
import { DcpMonTopBar } from "./components/DcpMonTopBar";
import { ErrorBoundary } from "./components/ErrorBoundary";
import { DisplaySettingsProvider } from "./DisplaySettingsContext";

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <DisplaySettingsProvider>
        <DcpMonTopBar />
        <main className="dcpmon-main">
          <Container fluid className="dcpmon-shell py-4">
            <ErrorBoundary
              fallback={
                <div className="alert alert-danger" role="alert">
                  DCPMon encountered an unexpected display error. Reload the page
                  to try again.
                </div>
              }
            >
              <DcpMonDashboard />
            </ErrorBoundary>
          </Container>
        </main>
      </DisplaySettingsProvider>
    </QueryClientProvider>
  );
}

export default App;

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import Container from "react-bootstrap/Container";
import { DcpMonDashboard } from "./components/DcpMonDashboard";
import { DcpMonTopBar } from "./components/DcpMonTopBar";

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <DcpMonTopBar />
      <main className="dcpmon-main">
        <Container fluid className="dcpmon-shell py-4">
          <DcpMonDashboard />
        </Container>
      </main>
    </QueryClientProvider>
  );
}

export default App;

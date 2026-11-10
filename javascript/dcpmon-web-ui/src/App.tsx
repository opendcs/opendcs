import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import Container from "react-bootstrap/Container";
import { DcpMonDashboard } from "./components/DcpMonDashboard";

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Container fluid className="dcpmon-shell py-4">
        <DcpMonDashboard />
      </Container>
    </QueryClientProvider>
  );
}

export default App;

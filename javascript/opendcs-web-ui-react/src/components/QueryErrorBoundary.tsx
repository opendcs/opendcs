import { Component, type ErrorInfo, type ReactNode } from "react";
import { Alert, Button } from "react-bootstrap";
import { QueryErrorResetBoundary } from "@tanstack/react-query";

interface InnerProps {
  children: ReactNode;
  onReset: () => void;
}

interface InnerState {
  error: Error | null;
}

// Catches errors from React's render phase, including the thrown promises that
// TanStack's `throwOnError` queries surface to Suspense/error boundaries.
class InnerBoundary extends Component<InnerProps, InnerState> {
  state: InnerState = { error: null };

  static getDerivedStateFromError(error: Error): InnerState {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("[QueryErrorBoundary]", error, info.componentStack);
  }

  reset = () => {
    this.props.onReset();
    this.setState({ error: null });
  };

  render() {
    if (this.state.error) {
      return (
        <Alert variant="danger" className="m-3">
          <Alert.Heading>Something went wrong</Alert.Heading>
          <p className="mb-2">{this.state.error.message}</p>
          <Button size="sm" variant="outline-danger" onClick={this.reset}>
            Try again
          </Button>
        </Alert>
      );
    }
    return this.props.children;
  }
}

interface Props {
  children: ReactNode;
}

// QueryErrorResetBoundary lets the boundary's "Try again" button reset every
// query that threw inside the boundary, so the next render retries the fetch
// instead of immediately re-throwing the cached error.
export const QueryErrorBoundary = ({ children }: Props) => (
  <QueryErrorResetBoundary>
    {({ reset }) => <InnerBoundary onReset={reset}>{children}</InnerBoundary>}
  </QueryErrorResetBoundary>
);

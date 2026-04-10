import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../contexts/app/AuthContext";
import { Container, Spinner } from "react-bootstrap";

export const PublicOnlyRoute = () => {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return (
      <Container className="d-flex justify-content-center align-items-center vh-100">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </Container>
    );
  }

  if (user) {
    return <Navigate to="/platforms" replace />;
  }

  return <Outlet />;
};

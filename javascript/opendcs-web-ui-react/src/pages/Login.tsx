import { type FormEvent } from "react";
import { useAuth } from "../contexts/AuthContext";
import { Button, Container, Form, Image } from "react-bootstrap";
import './Login.css';
import user_img from '../assets/img/user_profile_image_large.png'
import { Credentials, RESTAuthenticationAndAuthorizationApi } from "opendcs-api";
import { useApi } from "../contexts/ApiContext";

export default function Login() {
    const {setUser} = useAuth();
    const api = useApi();

    const auth = new RESTAuthenticationAndAuthorizationApi(api.conf);

    function handleLogin(event: FormEvent<HTMLFormElement>): void {
        event.preventDefault();
        event.stopPropagation();
        const form = event.currentTarget;

        const formData = new FormData(form);

        const dataObject = Object.fromEntries(formData.entries());
        // Convert FormData to a plain object for easier use
        const credentials: Credentials = {
            username: dataObject.username.toString(),
            password: dataObject.password.toString()
        };

        auth.postCredentials("", credentials)
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            .then((user_value: any) => setUser(user_value))
            .catch((error_: { toString: () => string; }) => alert('Login failed' + error_.toString()));

    }

    
    return (
        <Container className="page-content d-flex" fluid>
            <Container className="content-wrapper" fluid>
                <Container className="content loginPageBackground" fluid>
                    <Container className="wrapper fadeInDown" fluid>
                        <Container id="formContent" className="slightOpacity" fluid>
                            <div className="fadeIn first">
                                <Image src={user_img} id="icon" alt="User icon" />
                            </div>
                            <Form onSubmit={handleLogin}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Username</Form.Label>
                                    <Form.Control type="text" id="username" required name="username"/>
                                </Form.Group>
                                <Form.Group className="mb-3">
                                    <Form.Label>Password</Form.Label>
                                    <Form.Control type="password" id="password" required name="password"/>
                                </Form.Group>
                                <Button variant="primary" type="submit">
                                    Login
                                </Button>
                            </Form>
                        </Container>    
                    </Container>
                </Container>
            </Container>
        </Container>
    );
}
import { useContext, useEffect, type FormEvent } from "react";
import { AuthContext } from "../contexts/AuthContext";
import { Container, Image } from "react-bootstrap";
import './Login.css';
import user_img from '../assets/img/user_profile_image_large.png'
import { createConfiguration, RESTAuthenticationAndAuthorizationApi, ServerConfiguration } from "opendcs-api";

const conf = createConfiguration({ 
        baseServer: new ServerConfiguration("/odcsapi", {}),
        });
const auth = new RESTAuthenticationAndAuthorizationApi(conf);

export default function Login() {
    const user = useContext(AuthContext)!;
    function handleLogin(event: FormEvent<HTMLFormElement>): void {
        event.preventDefault();
        const formData = new FormData(event.currentTarget);
        const dataObject = Object.fromEntries(formData.entries());
        // Convert FormData to a plain object for easier use
        
        
        auth.postCredentials("", {username: dataObject.username.toString(), password: dataObject.password.toString()})
            .then(() => user.setUser({username: dataObject.username.toString()}))
            .catch((error_: { toString: () => string; }) => alert('Login failed' + error_.toString()));

    }

    
    return (
        <Container className="page-content d-flex" fluid>
            <Container className="content-wrapper" fluid>
                <Container className="page-header page-header-light" fluid>
					<div className="page-header-content header-elements-md-inline">
						<div className="page-title d-flex">
							<h4><i className="bi bi-arrow-left me-2"></i> <span
									className="font-weight-semibold">OpenDCS Web Client</span> - Login</h4>
							<a href="#" className="header-elements-toggle text-default d-md-none"><i
									className="bi bi-three-dots-vertical"></i></a>
						</div>
					</div>
				</Container>
                <Container className="content loginPageBackground" fluid>
                    <Container className="wrapper fadeInDown" fluid>
                        <Container id="formContent" className="slightOpacity" fluid>
                            <div className="fadeIn first">
                                <Image src={user_img} id="icon" alt="User icon" />
                            </div>
                            <form onSubmit={handleLogin}>
                                Username: <input name="username" /><br/>
                                Password: <input name="password" type="password" /><br/>
                                Organization: TODO (select)<br/>
                                <button type="submit">Login</button>
                            </form>
                        </Container>    
                    </Container>
                    
                </Container>
            </Container>
            <div>
            
            
        </div>
        </Container>
        
    );
}
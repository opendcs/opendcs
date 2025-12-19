import { useContext, type FormEvent } from "react";
import { AuthContext } from "../contexts/AuthContext";
import { Container, Image } from "react-bootstrap";
import './Login.css';
import user_img from '../assets/img/user_profile_image_large.png'

export default function Login() {
    const user = useContext(AuthContext);
    function handleLogin(event: FormEvent<HTMLFormElement>): void {
        event.preventDefault();
        const formData = new FormData(event.currentTarget);
    // Convert FormData to a plain object for easier use
        const dataObject = Object.fromEntries(formData.entries());
        user?.setUser({username: dataObject.username.toString()})
    }

    return (
        <Container className="page-content" fluid>
            <Container className="content-wrapper" fluid>
                <div className="page-header page-header-light">
					<div className="page-header-content header-elements-md-inline">
						<div className="page-title d-flex">
							<h4><i className="bi bi-arrow-left me-2"></i> <span
									className="font-weight-semibold">OpenDCS Web Client</span> - Login</h4>
							<a href="#" className="header-elements-toggle text-default d-md-none"><i
									className="bi bi-three-dots-vertical"></i></a>
						</div>
					</div>
				</div>
                <Container className="content loginPageBackground" fluid>
                    <Container className="wrapper fadeInDown" fluid>
                        <Container id="formContent" className="slightOpacity" fluid>
                            <div className="fadeIn first">
                                <Image src={user_img} id="icon" alt="User icon" />
                            </div>
                            <form onSubmit={handleLogin}>
                                Username: <input name="username" defaultValue={user?.user?.username}/><br/>
                                Password: TODO<br/>
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
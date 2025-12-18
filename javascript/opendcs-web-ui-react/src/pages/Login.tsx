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
        user?.setUser(dataObject.username.toString())
    }

    return (
        <Container className="page-content">
            <Container className="content-wrapper">
                
                <Container className="content loginPageBackground">
                    <div className="fadeIn first">
                        <Image src={user_img} id="icon" alt="User icon" />
                    </div>
                    <form onSubmit={handleLogin}>
                        Username: <input name="username" defaultValue={user?.user}/><br/>
                        Password: TODO<br/>
                        Organization: TODO (select)<br/>
                        <button type="submit">Login</button>
                    </form>
                </Container>
            </Container>
            <div>
            
            
        </div>
        </Container>
        
    );
}
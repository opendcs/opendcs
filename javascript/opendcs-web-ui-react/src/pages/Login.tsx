import { useContext, type FormEvent } from "react";
import { AuthContext } from "../contexts/AuthContext";

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
        <div>
            <form onSubmit={handleLogin}>
                Username: <input name="username" defaultValue={user?.user}/>
                <button type="submit">Login</button>
            </form>
            
        </div>
    );
}
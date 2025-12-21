import { useEffect } from 'react'
import './App.css'
import { useAuth } from './contexts/AuthContext';
import Login from './pages/Login';
import { TopBar } from './components/TopBar';
import { ModeIcons } from './components/ModeIcon';

import {createConfiguration, RESTAuthenticationAndAuthorizationApi, ServerConfiguration} from 'opendcs-api'
const conf = createConfiguration({ 
    baseServer: new ServerConfiguration("/odcsapi", {}),

});
const auth = new RESTAuthenticationAndAuthorizationApi(conf);


function App() {  
  const {user, setUser} = useAuth();
  useEffect(() => {
        console.log("Checking auth");
        auth.checkSessionAuthorization()
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            .then((value: any) => {
                console.log(value);
                setUser({username: value.username});
            })
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            .catch((_error) => { /* do nothing, just means we don't have a session */});
    }, [setUser]);


  return (
    <>
        <ModeIcons />
        <TopBar />
        
        {user?.username ? <div>Hello, {user.username}</div> : <Login />}
      </>
  )
}

export default App

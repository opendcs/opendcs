import { useEffect } from 'react'
import './App.css'
import { useAuth } from './contexts/AuthContext';
import Login from './pages/Login';
import { TopBar } from './components/TopBar';
import { ModeIcons } from './components/ModeIcon';

import {RESTAuthenticationAndAuthorizationApi} from 'opendcs-api'
import { useApi } from './contexts/ApiContext';

function App() {  
  const {user, setUser} = useAuth();
  const api = useApi();

  useEffect(() => {
        const auth = new RESTAuthenticationAndAuthorizationApi(api.conf);
        auth.checkSessionAuthorization()
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            .then((value: any) => {
                console.log(value);
                setUser({username: value.username});
            })
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            .catch((_error) => { /* do nothing, just means we don't have a session */});
    }, [api.conf, setUser]);


  return (
    <>
        <ModeIcons />
        <TopBar />
        
        {user?.username ? <div>Hello, {user.username}</div> : <Login />}
      </>
  )
}

export default App

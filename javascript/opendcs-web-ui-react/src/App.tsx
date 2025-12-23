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

  const username: string | undefined = user?.email;
  const haveUser = user !== undefined;

  useEffect(() => {
    const auth = new RESTAuthenticationAndAuthorizationApi(api.conf);
    auth.checkSessionAuthorization()
        // The current API spec (in the generated api) shows string as the return still
        // the endpoint *correctly* returns a user object. likely just an issue
        // with the autocomplete cache on my system.
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        .then((value: any) => setUser(value))
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        .catch((_error) => { /* do nothing, just means we don't have a session */});
  }, [api.conf, setUser]);


  return (
    <>
        <ModeIcons />
        <TopBar />
        
        {haveUser ? 
          <div>Hello, {username}</div> 
          :
          <Login />}
      </>
  )
}

export default App

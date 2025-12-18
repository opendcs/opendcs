import { useState } from 'react'
import './App.css'
import { AuthContext } from './contexts/AuthContext';
import Login from './pages/Login';

//import {createConfiguration, RESTAuthenticationAndAuthorizationApi, ServerConfiguration} from 'opendcs-api'


function App() {
   const [user, setUser] = useState("");
  // const conf = createConfiguration({ 
  //   baseServer: new ServerConfiguration("/odcsapi", {}),

  // });
  //const tmp = new RESTAuthenticationAndAuthorizationApi(conf);
  //tmp.checkSessionAuthorization();
  return (
    <AuthContext value={{user, setUser}}>
      Current user: {user}
      <Login />
    </AuthContext>
  )
}

export default App

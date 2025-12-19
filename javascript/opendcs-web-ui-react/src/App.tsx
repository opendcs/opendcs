import { useState } from 'react'
import './App.css'
import { AuthContext, type User } from './contexts/AuthContext';
import Login from './pages/Login';
import { TopBar } from './components/TopBar';

//import {createConfiguration, RESTAuthenticationAndAuthorizationApi, ServerConfiguration} from 'opendcs-api'


function App() {
  const [user, setUser] = useState<User>({});
  // const conf = createConfiguration({ 
  //   baseServer: new ServerConfiguration("/odcsapi", {}),

  // });
  //const tmp = new RESTAuthenticationAndAuthorizationApi(conf);
  //tmp.checkSessionAuthorization();
  return (
    <AuthContext value={{user, setUser}}>
      <TopBar />
      Current user: {user.username}
      <Login />
    </AuthContext>
  )
}

export default App

import { useState } from 'react'
import './App.css'
import { AuthContext, type User } from './contexts/AuthContext';
import Login from './pages/Login';
import { TopBar } from './components/TopBar';
import { ModeIcons } from './components/ModeIcon';
import { ThemeContext } from './contexts/ThemeContext';
import ColorModes from './components/ColorMode';

//import {createConfiguration, RESTAuthenticationAndAuthorizationApi, ServerConfiguration} from 'opendcs-api'


function App() {
  const [user, setUser] = useState<User>({});
  // const conf = createConfiguration({ 
  //   baseServer: new ServerConfiguration("/odcsapi", {}),

  // });
  //const tmp = new RESTAuthenticationAndAuthorizationApi(conf);
  //tmp.checkSessionAuthorization();
  return (
    <ThemeContext value={{colorMode: 'auto'}}>
    <AuthContext value={{user, setUser}}>
      <ModeIcons />
      <TopBar />
      
      {user?.username ? <div>Hello</div> : <Login />}
    </AuthContext>
    </ThemeContext>
  )
}

export default App

import { useContext } from 'react';
import { Container, NavDropdown } from 'react-bootstrap';
import { PersonGear } from 'react-bootstrap-icons';
import Nav from 'react-bootstrap/Nav';
import Navbar from 'react-bootstrap/Navbar';
import { AuthContext } from '../contexts/AuthContext';
import ModeIcon from './ModeIcon';



export function TopBar() {
    const user = useContext(AuthContext)
    
    return (
        <Navbar fixed="top" expand="lg" className="bg-primary">
            <Container fluid>
                    <Navbar.Brand href="#">OpenDCS</Navbar.Brand>
            </Container>
            <Nav className="navbar-right ms-md-auto flex-row-flex-warp">
                <Nav.Item>
                    <NavDropdown title={<ModeIcon name='circle-half' className='bi me-2 opacity-50 my-1 mode-icon-active' />} id="color-mode" drop='start'>
                        <NavDropdown.Item><ModeIcon name='sun-fill' className='bi me-2 opacity-50 theme-icon' />Light</NavDropdown.Item>
                        <NavDropdown.Item><ModeIcon name='moon-starts-fill' className='bi me-2 opacity-50 theme-icon' />Dark</NavDropdown.Item>
                        <NavDropdown.Item><ModeIcon name='circle-half' className='bi me-2 opacity-50 theme-icon' />Auto</NavDropdown.Item>
                    </NavDropdown>
                </Nav.Item>
                {user?.user?.username ? 
                <NavDropdown title={<PersonGear className="theme-icon my-1"/>} id="user-settings" drop="start" >
                    <NavDropdown.Item>Profile</NavDropdown.Item>
                    <NavDropdown.Item>Admin</NavDropdown.Item>
                </NavDropdown> : <div/>
                }
                
                
            </Nav>
        </Navbar>        
        //  <nav class="navbar  navbar-expand-lg bg-primary sticky-top">
                
                
        //         <div class="container-fluid">
        //             <div class="navbar-header">
        //                 <a class="navbar-brand" href="#">OpenDCS</a>
        //             </div>
        //             <ul class="navbar-nav navbar-right ms-md-auto flex-row-flex-warp">
        //                 <li class="nav-item dropdown ">
                            

        //                     <button class="btn btn-link nav-link py-2 px-0 px-lg-2 dropdown-toggle d-flex align-items-center"
        //                                 id="bd-theme-mode"
        //                                 type="button"
        //                                 aria-expanded="false"
        //                                 data-bs-toggle="dropdown"
        //                                 data-bs-display="static"
        //                                 aria-label="Toggle theme (auto)">
        //                         <svg class="bi my-1 mode-icon-active"><use href="#circle-half"></use></svg>
        //                         <span class="d-lg-none ms-2" id="bd-theme-mode-text">Toggle theme</span>
        //                     </button>
        //                     <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="bd-theme-mode-text">
        //                         <li>
        //                             <button type="button" class="dropdown-item d-flex align-items-center" data-bs-theme-value="light" aria-pressed="false">
        //                             <svg class="bi me-2 opacity-50 theme-icon"><use href="#sun-fill"></use></svg>
        //                             Light
        //                             <svg class="bi ms-auto d-none  theme-icon"><use href="#check2"></use></svg>
        //                             </button>
        //                         </li>
        //                         <li>
        //                             <button type="button" class="dropdown-item d-flex align-items-center" data-bs-theme-value="dark" aria-pressed="false">
        //                             <svg class="bi me-2 opacity-50 theme-icon"><use href="#moon-stars-fill"></use></svg>
        //                             Dark
        //                             <svg class="bi ms-auto d-none theme-icon"><use href="#check2"></use></svg>
        //                             </button>
        //                         </li>
        //                         <li>
        //                             <button type="button" class="dropdown-item d-flex align-items-center active" data-bs-theme-value="auto" aria-pressed="true">
        //                             <svg class="bi me-2 opacity-50 theme-icon"><use href="#circle-half"></use></svg>
        //                             Auto
        //                             <svg class="bi ms-auto d-none theme-icon"><use href="#check2"></use></svg>
        //                             </button>
        //                         </li>
        //                     </ul>
        //                 </li>
        //                 <li class="nav-item dropdown ">
        //                     <button class="btn btn-link nav-link py-2 px-0 px-lg-2 dropdown-toggle d-flex align-items-center"
        //                                 id="user-settings"
        //                                 type="button"
        //                                 aria-expanded="false"
        //                                 data-bs-toggle="dropdown"
                                        
        //                                 aria-label="User Settings">
        //                         <i class="bi my-1 bi-person-gear theme-icon"></i>
        //                     </button>
        //                     <ul class="dropdown-menu dropdown-menu-end">
        //                         <li><a class="dropdown-item" href="#">Profile</a></li>
        //                         <li><a class="dropdown-item" href="#">Admin</a></li>
        //                         <li class="dropdown-submenu">
        //                             <a class="dropdown-item dropdown-toggle" href="#" id="moreOptions">
        //                                 Organization:
        //                                 <span id="organization_id"></span>
        //                             </a>
        //                             <ul class="dropdown-menu submenu-menu p-3" aria-labelledby="moreOptions" style="min-width: 250px;right: 100%; left: auto; top: 0">
        //                                 <li>
        //                                     <label for="select_organization" class="form-label">Choose Organization:</label>
        //                                     <select id="select_organization" class="form-select" name="select_organization">
        //                                         <option></option>
        //                                     </select>
        //                                 </li>
        //                             </ul>
        //                         </li>
        //                         <li>
        //                             <hr class="dropdown-divider"/>
        //                         </li>
        //                         <%@include file="/WEB-INF/common/theme-selection.jspf"%>
        //                         <li>
        //                             <hr class="dropdown-divider"/>
        //                         </li>
        //                         <li><a class="dropdown-item" href="#">Logout</a></li>
        //                     </ul>
        //                 </li>
        //             </ul>
        //         </div>
        //     </nav>
    );
}
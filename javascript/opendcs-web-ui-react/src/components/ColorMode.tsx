import ModeIcon from "./ModeIcon";
import {NavDropdown} from 'react-bootstrap'


export default function ColorModes () {

    return (
        <NavDropdown title={<ModeIcon name='circle-half' className='bi me-2 opacity-50 my-1 mode-icon-active  theme-icon' />} id="color-mode" drop='start'>
            <NavDropdown.Item><ModeIcon name='sun-fill' className='bi me-2 opacity-50 theme-icon' />Light</NavDropdown.Item>
            <NavDropdown.Item><ModeIcon name='moon-stars-fill' className='bi me-2 opacity-50 theme-icon' />Dark</NavDropdown.Item>
            <NavDropdown.Item><ModeIcon name='circle-half' className='bi me-2 opacity-50 theme-icon' />Auto</NavDropdown.Item>
        </NavDropdown>
    );
}
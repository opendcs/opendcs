import { useState, type ReactNode } from "react";
import { ThemeContext, type ColorMode, type Theme } from "./ThemeContext";

interface ProviderProps {
    children: ReactNode;
}

export const ThemeProvider = ({children}: ProviderProps) => {
    const stored: ColorMode = localStorage.getItem('theme-mode') as ColorMode || 'auto';


    const [theme, setTheme] = useState<Theme>({colorMode: stored});    
    return (
        <ThemeContext value={{theme, setTheme}}>
            {children}
        </ThemeContext>
    );
};



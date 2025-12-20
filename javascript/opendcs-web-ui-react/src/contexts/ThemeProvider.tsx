import { useState, type ReactNode } from "react";
import { ThemeContext, type Theme } from "./ThemeContext";

interface ProviderProps {
    children: ReactNode;
}

export const ThemeProvider = ({children}: ProviderProps) => {
    const [theme, setTheme] = useState<Theme>({colorMode: 'auto'});

    return (
        <ThemeContext value={{theme, setTheme}}>
            {children}
        </ThemeContext>
    );
};



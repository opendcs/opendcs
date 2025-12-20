import { createContext, useContext } from "react";

export type Theme = {
    colorMode: 'light' | 'dark' | 'auto';
}

export interface ThemeContextType {
    theme: Theme,
    setTheme: React.Dispatch<React.SetStateAction<Theme>>
}

export const ThemeContext = createContext<ThemeContextType|undefined>(undefined);

export const useTheme = () => {
    const context = useContext(ThemeContext);
    if (context == undefined)
    {
        throw new Error("Theme isn't defined?");
    }
    return context;
}
import { createContext } from "react";

export type Theme = {
    colorMode: 'light' | 'dark' | 'auto';
}

export interface ThemeContextType {
    theme: Theme,
    setUser: React.Dispatch<React.SetStateAction<Theme>>
}

export const ThemeContext = createContext<Theme>({colorMode: "auto"});
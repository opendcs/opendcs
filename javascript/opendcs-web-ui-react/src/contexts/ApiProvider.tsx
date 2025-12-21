import { type ReactNode } from "react";
import { ApiContext, defaultValue } from "./ApiContext";


interface ProviderProps {
    children: ReactNode;
}

export const ApiProvider = ({children}: ProviderProps) => {
    

    return (
        <ApiContext value={defaultValue}>
            {children}
        </ApiContext>
    );
};
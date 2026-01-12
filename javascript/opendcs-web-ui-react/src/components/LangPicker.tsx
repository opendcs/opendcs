import type { i18n } from "i18next";
import React from "react";
import { Button, Dropdown } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import availableLanguages from "../lang";

interface ToggleProperties {
    i18n: i18n;
    //args: unknown[];
}

const LangToggle: React.FC<ToggleProperties> = ({i18n, ...args}) => {
    return (
        <Button {...args}>
            {i18n.language}
        </Button>
    );
}


export const LangPicker = () => {
    const [t, i18n] = useTranslation();

    const changeLang = (lang: string) => {
        i18n.changeLanguage(lang);
    };

    return (
        <Dropdown drop="start">
            <Dropdown.Toggle as={LangToggle} aria-label={t("Language Menu")} i18n={i18n}>
                
            </Dropdown.Toggle>
            <Dropdown.Menu>
                {   availableLanguages.map((lang) => {
                        const locale = new Intl.DisplayNames([lang], {type: "language"})
                        const nativeName = locale.of(lang);
                        return (
                            <Dropdown.Item key={lang}>
                                <Button aria-label={t("Change language", {lang: nativeName})}
                                        onClick={() => changeLang(lang)}>
                                    {locale.of(lang)}
                                </Button>
                            </Dropdown.Item>
                            )
                        }
                    )
                }
            </Dropdown.Menu>
        </Dropdown>
    )
};


export default LangPicker;
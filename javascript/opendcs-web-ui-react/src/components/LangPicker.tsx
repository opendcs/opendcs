import type { i18n } from "i18next";
import React from "react";
import { Button, Dropdown } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import availableLanguages from "../lang";

interface ToggleProperties {
    i18n: i18n;
}

function getRegion(locale: Intl.Locale) {
    if (locale.region) {
        return locale.region
    } else {
        const tmp = locale.maximize();
        return tmp.region;
    }
}

// from https://dev.to/jorik/country-code-to-flag-emoji-a21
function getFlagEmoji(countryCode: string) {
  const codePoints = countryCode
    .toUpperCase()
    .split('')
    .map(char =>  127397 + char.charCodeAt(0));
  return String.fromCodePoint(...codePoints);
}


const LangToggle: React.FC<ToggleProperties> = ({i18n, ...args}) => {
    const lang = i18n.language;
    const region = getRegion(new Intl.Locale(lang))?.toLocaleUpperCase();
    const flagEmoji = getFlagEmoji(region!);
    return (
        <Button {...args} variant="link">
            {flagEmoji} - {lang}
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
            <Dropdown.Toggle as={LangToggle} aria-label={t("Language Menu")} i18n={i18n} />
            <Dropdown.Menu>
                {availableLanguages.map((lang) => {
                    const locale = new Intl.DisplayNames([lang], {type: "language"})
                    const nativeName = locale.of(lang);
                    const region = getRegion(new Intl.Locale(lang))?.toLocaleUpperCase();
                    const flagEmoji = getFlagEmoji(region!);
                    const ariaLabel = t("Change language", {lang: nativeName, lng: lang});
                    return (
                        <Dropdown.Item key={lang}>
                            <Button aria-label={ariaLabel} variant="link"
                                    onClick={() => changeLang(lang)}>
                                {flagEmoji} - {locale.of(lang)}
                            </Button>
                        </Dropdown.Item>
                        )
                    })
                }
            </Dropdown.Menu>
        </Dropdown>
    )
};


export default LangPicker;
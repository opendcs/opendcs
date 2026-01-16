import type { Meta, StoryObj } from "@storybook/react-vite";
import { userEvent } from 'storybook/test';

import LangPicker from "./LangPicker";
import i18n from "../i18n";

const meta = {
  component: LangPicker,
} satisfies Meta<typeof LangPicker>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
  globals: {
    locale: "en-US",
  },
};

export const StartingFromEnglish: Story = {
  args: {
    ...Default.args,
  },
  play: async ({ canvas, userEvent, mount }) => {
    await mount();
    const spanishLocale = "es-ES";
    const menu = canvas.getByRole("button", { name: i18n.t("Language Menu") });
    await userEvent.click(menu);

    const nativeName = new Intl.DisplayNames(["es"], { type: "language" }).of("es-ES");
    const spanish = await canvas.findByRole("button", {
      name: i18n.t("Change language", { lang: nativeName, lng: spanishLocale }),
    });
    await userEvent.click(spanish);

    const menuSpanish = await canvas.findByRole("button", {
      name: i18n.t("Language Menu", { lng: spanishLocale }),
    });
    await userEvent.click(menuSpanish);

    const english = await canvas.findByRole("button", {
      name: "Change language to American English",
    });
    await userEvent.click(english);

    await canvas.findByRole("button", { name: "Open Translation Menu" });
  },
};

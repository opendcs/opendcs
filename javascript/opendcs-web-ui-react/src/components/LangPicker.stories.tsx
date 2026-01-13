import type { Meta, StoryObj } from "@storybook/react-vite";

import LangPicker from "./LangPicker";
import i18n from "../i18n";
import { t } from "i18next";
import { act } from "@testing-library/react";

const meta = {
  component: LangPicker,
} satisfies Meta<typeof LangPicker>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
  play: async ({}) => {

  }
};


export const StartingFromEnglish : Story = {
  args: {
    ...Default.args
  },
  play: async ({ canvas, userEvent, mount, context }) => {
    i18n.changeLanguage("en-US"); // make sure we're starting with english
    await mount();

    const menu = canvas.getByRole("button", {name: "Open Translation Menu"});
    await act(async () => userEvent.click(menu));

    const nativeName = new Intl.DisplayNames( ['es'], {type: "language"} ).of("es")
    const spanish = await canvas.findByRole("button", {name: i18n.t("Change language", {lang: nativeName, lng: 'es'})})
    await act(async () => userEvent.click(spanish));

    const menuSpanish = await canvas.findByRole("button", {name: i18n.t("Language Menu", {lng: "es"})})
    await act(async () => userEvent.click(menuSpanish));

    const english = await canvas.findByRole("button", {name: "Change language to American English"});
    await act(async () => userEvent.click(english));

    const _menu2 = await canvas.findByRole("button", {name: "Open Translation Menu"});
  }
}



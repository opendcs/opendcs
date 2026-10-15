import type { Meta, StoryObj } from "@storybook/react-vite";
// eslint-disable-next-line storybook/use-storybook-testing-library
import { act, fireEvent } from "@testing-library/react";
import { expect } from "storybook/test";
import { ImportPage } from "./ImportPage";

const meta = {
  component: ImportPage,
} satisfies Meta<typeof ImportPage>;

export default meta;
type Story = StoryObj<typeof meta>;

export const ImportTypes: Story = {
  play: async ({ mount }) => {
    await mount();
  },
};

export const PreviewAndImport: Story = {
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const t = (key: string) => parameters.i18n.t(`administration:${key}`);
    await act(async () =>
      userEvent.click(
        await canvas.findByRole("button", { name: t("decodesImportTitle") }),
      ),
    );
    expect(canvas.queryByText(t("timeSeriesImportTitle"))).not.toBeInTheDocument();
    const preview = canvas.getByRole("button", { name: t("decodesImport.preview") });
    const importButton = canvas.getByRole("button", {
      name: t("decodesImport.import"),
    });
    expect(preview).toBeDisabled();
    expect(importButton).toBeDisabled();

    await act(async () =>
      userEvent.upload(
        canvas.getByLabelText(t("decodesImport.chooseFile")),
        new File(["<Database/>"], "decodes.xml", { type: "application/xml" }),
      ),
    );
    expect(preview).toBeEnabled();
    expect(
      (canvas.getByLabelText(t("decodesImport.chooseFile")) as HTMLInputElement)
        .files?.[0]?.name,
    ).toBe("decodes.xml");
    expect(importButton).toBeDisabled();
    await act(async () => userEvent.click(preview));
    const log = canvas.getByRole("log");
    expect(log).toHaveTextContent("[Sample preview] Preview complete.");
    expect(importButton).toBeEnabled();
    await act(async () => userEvent.click(importButton));
    expect(log).toHaveTextContent("[Sample import] Simulation complete.");
    await act(async () =>
      userEvent.click(
        canvas.getByRole("button", { name: t("decodesImport.clearLog") }),
      ),
    );
    expect(log).toHaveTextContent(t("decodesImport.emptyLog"));
    await act(async () =>
      userEvent.click(canvas.getByRole("button", { name: t("decodesImport.back") })),
    );
    expect(canvas.getByText(t("timeSeriesImportTitle"))).toBeInTheDocument();
  },
};

export const DropFileAndResetPreview: Story = {
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const t = (key: string) => parameters.i18n.t(`administration:${key}`);
    await act(async () =>
      userEvent.click(
        await canvas.findByRole("button", { name: t("decodesImportTitle") }),
      ),
    );
    const dropArea = canvas.getByText(t("decodesImport.dropFile")).parentElement!;
    const drop = (files: File[]) =>
      act(async () => {
        const dataTransfer = new DataTransfer();
        files.forEach((file) => dataTransfer.items.add(file));
        fireEvent(dropArea, new DragEvent("drop", { bubbles: true, dataTransfer }));
      });
    await drop([new File(["<Database/>"], "dropped.xml")]);
    expect(canvas.getByRole("log")).toHaveTextContent("dropped.xml");
    const fileInput = canvas.getByLabelText(
      t("decodesImport.chooseFile"),
    ) as HTMLInputElement;
    expect(fileInput.files?.[0]?.name).toBe("dropped.xml");
    await act(async () =>
      userEvent.click(canvas.getByRole("button", { name: t("decodesImport.preview") })),
    );
    const importButton = canvas.getByRole("button", {
      name: t("decodesImport.import"),
    });
    expect(importButton).toBeEnabled();

    await drop([new File(["<Database/>"], "replacement.xml")]);
    expect(fileInput.files?.[0]?.name).toBe("replacement.xml");
    expect(importButton).toBeDisabled();
    expect(canvas.getByRole("log")).not.toHaveTextContent(
      "[Sample preview] Preview complete.",
    );
    await drop([new File(["plain text"], "invalid.txt")]);
    expect(fileInput.files?.length).toBe(0);
    expect(canvas.getByText(t("decodesImport.invalidFile"))).toBeInTheDocument();
    expect(
      canvas.getByRole("button", { name: t("decodesImport.preview") }),
    ).toBeDisabled();
    await drop([new File([""], "one.xml"), new File([""], "two.xml")]);
    expect(canvas.getByText(t("decodesImport.invalidFile"))).toBeInTheDocument();
  },
};

import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
// eslint-disable-next-line storybook/use-storybook-testing-library
import { act } from "@testing-library/react";
import { expect, fn, screen, waitFor } from "storybook/test";
import { CheckForNewModal } from "./CheckForNewModal";
import type { CatalogAlgorithm } from "../../../queries/algorithms";

const CATALOG: CatalogAlgorithm[] = [
  {
    name: "CopyAlgorithm",
    execClass: "decodes.comp.CopyAlgorithm",
    description: "Copies one TS to another.",
    alreadyImported: false,
  },
  {
    name: "AlreadyHere",
    execClass: "decodes.comp.AlreadyHere",
    description: "Already imported.",
    alreadyImported: true,
  },
];

const meta = {
  component: CheckForNewModal,
  args: { show: true, onHide: fn(), onImported: fn() },
  parameters: {
    msw: {
      handlers: {
        get: http.get("/odcsapi/algorithmcatalog", () =>
          HttpResponse.json<CatalogAlgorithm[]>(CATALOG),
        ),
        post: http.post(
          "/odcsapi/algorithmcatalog",
          () => new HttpResponse(null, { status: 200 }),
        ),
      },
    },
  },
} satisfies Meta<typeof CheckForNewModal>;

export default meta;
type Story = StoryObj<typeof meta>;

/** "No new algorithms found." when every entry is already imported. */
export const NoneAvailable: Story = {
  parameters: {
    msw: {
      handlers: {
        get: http.get("/odcsapi/algorithmcatalog", () =>
          HttpResponse.json<CatalogAlgorithm[]>([
            { name: "X", execClass: "x.X", description: "", alreadyImported: true },
          ]),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    await mount();
    await screen.findByText(parameters.i18n.t("algorithms:check_new.none_found"));
  },
};

/**
 * Verifies: already-imported rows are hidden, import button is disabled with
 * no selection, then select + import calls both callbacks.
 */
export const SelectAndImport: Story = {
  play: async ({ mount, args, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    // Modal portals outside canvas -> use screen.
    await screen.findByText("CopyAlgorithm");
    expect(screen.queryByText("AlreadyHere")).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", {
        name: i18n.t("algorithms:check_new.import_selected", { count: 0 }),
      }),
    ).toBeDisabled();

    await act(async () => userEvent.click(screen.getByText("CopyAlgorithm")));

    const importBtn = await screen.findByRole("button", {
      name: i18n.t("algorithms:check_new.import_selected", { count: 1 }),
    });
    expect(importBtn).not.toBeDisabled();
    await act(async () => userEvent.click(importBtn));

    await waitFor(() => {
      expect(args.onImported).toHaveBeenCalled();
      expect(args.onHide).toHaveBeenCalled();
    });
  },
};

/** Error Alert when catalog GET returns a non-ok status. */
export const CatalogFetchError: Story = {
  parameters: {
    msw: {
      handlers: {
        get: http.get(
          "/odcsapi/algorithmcatalog",
          () => new HttpResponse(null, { status: 500 }),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    await mount();
    await screen.findByText(parameters.i18n.t("algorithms:check_new.fetch_error"));
  },
};

/** Error Alert when the POST import returns a non-ok status. */
export const ImportError: Story = {
  parameters: {
    msw: {
      handlers: {
        get: http.get("/odcsapi/algorithmcatalog", () =>
          HttpResponse.json<CatalogAlgorithm[]>(CATALOG),
        ),
        post: http.post(
          "/odcsapi/algorithmcatalog",
          () => new HttpResponse(null, { status: 500 }),
        ),
      },
    },
  },
  play: async ({ mount, args, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    await screen.findByText("CopyAlgorithm");
    await act(async () => userEvent.click(screen.getByText("CopyAlgorithm")));
    await act(async () =>
      userEvent.click(
        await screen.findByRole("button", {
          name: i18n.t("algorithms:check_new.import_selected", { count: 1 }),
        }),
      ),
    );
    await screen.findByText(i18n.t("algorithms:check_new.import_error"));
    expect(args.onImported).not.toHaveBeenCalled();
  },
};

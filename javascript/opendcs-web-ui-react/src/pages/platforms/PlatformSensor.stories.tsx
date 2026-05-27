import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
import type { ApiConfigSensor, ApiPlatformSensor, ApiSiteRef } from "opendcs-api";
import { expect, screen, waitFor } from "storybook/test";
import { PlatformSensor } from "./PlatformSensor";

const configSensor: ApiConfigSensor = {
  sensorNumber: 1,
  sensorName: "Stage",
  dataTypes: { SHEF: "HG", CWMS: "Stage" },
  recordingMode: "F",
  recordingInterval: 3600,
  absoluteMin: 0,
  absoluteMax: 100,
  usgsStatCode: "00003",
};

const overrideWithSite: ApiPlatformSensor = {
  sensorNum: 1,
  actualSiteId: 22,
  min: 5,
  max: 95,
  usgsDdno: 4242,
};

const noOverride: ApiPlatformSensor = { sensorNum: 1 };

const mockSiteRefs: ApiSiteRef[] = [
  {
    siteId: 22,
    publicName: "Beta Site",
    sitenames: { local: "Beta" },
  },
  {
    siteId: 33,
    publicName: "Charlie Station",
    sitenames: { cwms: "Charlie" },
  },
];

const siteHandler = http.get("/odcsapi/siterefs", () =>
  HttpResponse.json<ApiSiteRef[]>(mockSiteRefs),
);

const meta = {
  title: "Platforms/PlatformSensor",
  component: PlatformSensor,
  parameters: { msw: { handlers: [siteHandler] } },
} satisfies Meta<typeof PlatformSensor>;
export default meta;

type Story = StoryObj<typeof meta>;

// View mode renders the config fields read-only.
export const ReadOnly: Story = {
  args: {
    configSensor,
    override: overrideWithSite,
    edit: false,
  },
  play: async ({ mount }) => {
    const canvas = await mount();
    await waitFor(() => expect(canvas.getByText("Stage")).toBeInTheDocument());
    // No Choose / clear buttons in view mode.
    expect(canvas.queryByRole("button", { name: /Choose/i })).not.toBeInTheDocument();
  },
};

// Edit mode with an existing override: clear-button enabled, override fields
// editable, choose-button visible.
export const EditWithOverride: Story = {
  args: {
    configSensor,
    override: overrideWithSite,
    edit: true,
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const choose = await waitFor(() =>
      canvas.getByRole("button", { name: i18n.t("platforms:select_site") }),
    );
    expect(choose).toBeInTheDocument();
    const clearBtn = canvas.getByRole("button", {
      name: i18n.t("platforms:clear_actual_site"),
    });
    expect(clearBtn).toBeEnabled();
  },
};

// No override: actualSiteId is undefined, so the clear button is disabled.
export const EditNoOverrideClearDisabled: Story = {
  args: {
    configSensor,
    override: noOverride,
    edit: true,
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const clearBtn = await waitFor(() =>
      canvas.getByRole("button", {
        name: i18n.t("platforms:clear_actual_site"),
      }),
    );
    expect(clearBtn).toBeDisabled();
  },
};

// Clicking Choose, picking a site, and verifying the input reflects the
// chosen site name end-to-end.
export const PickSiteFromChooser: Story = {
  args: {
    configSensor,
    override: noOverride,
    edit: true,
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const choose = await waitFor(() =>
      canvas.getByRole("button", { name: i18n.t("platforms:select_site") }),
    );
    await userEvent.click(choose);

    // SelectorModal renders in a portal, so look up via document-scoped screen.
    const charlieRow = await screen.findByText("Charlie Station", undefined, {
      timeout: 5000,
    });
    await userEvent.click(charlieRow);

    const selectBtn = await screen.findByRole("button", {
      name: i18n.t("translation:select", { defaultValue: "Select" }),
    });
    await userEvent.click(selectBtn);

    // After selection the input value should equal the picked site's display name.
    await waitFor(() => {
      const input = canvas.getByLabelText(
        i18n.t("platforms:actual_site_id"),
      ) as HTMLInputElement;
      expect(input.value).toBe("Charlie Station");
    });
  },
};

// Clicking the clear (X) button resets the displayed name and disables the
// clear button afterwards.
export const ClearOverrideSite: Story = {
  args: {
    configSensor,
    override: overrideWithSite,
    edit: true,
  },
  play: async ({ mount, parameters, userEvent }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const clearBtn = await waitFor(() =>
      canvas.getByRole("button", {
        name: i18n.t("platforms:clear_actual_site"),
      }),
    );
    await userEvent.click(clearBtn);

    await waitFor(() => {
      const input = canvas.getByLabelText(
        i18n.t("platforms:actual_site_id"),
      ) as HTMLInputElement;
      expect(input.value).toBe("");
      // After clearing, the clear button should be disabled.
      expect(clearBtn).toBeDisabled();
    });
  },
};

import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
import type { ApiConfigRef } from "opendcs-api";
import { useCallback, useState } from "react";
import { expect, fn, screen, waitFor } from "storybook/test";
import { ConfigSelectModal } from "./ConfigSelectModal";

const mockConfigRefs: ApiConfigRef[] = [
  {
    configId: 50,
    name: "Shef-WSC-Hydro",
    description: "WSC SHEF Hydro config",
    numPlatforms: 3,
  },
  {
    configId: 51,
    name: "Shef-AE-Met",
    description: "AE SHEF Met config",
    numPlatforms: 1,
  },
];

const handlers = {
  configRefs: http.get("/odcsapi/configrefs", () =>
    HttpResponse.json<ApiConfigRef[]>(mockConfigRefs),
  ),
};

const emptyConfigRefsHandler = http.get("/odcsapi/configrefs", () =>
  HttpResponse.json<ApiConfigRef[]>([]),
);

interface DemoProps {
  onSelect: (config: ApiConfigRef) => void;
}

const Demo: React.FC<DemoProps> = ({ onSelect }) => {
  const [show, setShow] = useState(true);
  const handleSelect = useCallback(
    (config: ApiConfigRef) => {
      onSelect(config);
      setShow(false);
    },
    [onSelect],
  );
  return (
    <ConfigSelectModal
      show={show}
      onHide={() => setShow(false)}
      onSelect={handleSelect}
    />
  );
};

const meta = {
  component: Demo,
  args: { onSelect: fn() },
  parameters: { msw: { handlers } },
} satisfies Meta<typeof Demo>;

export default meta;

type Story = StoryObj<typeof meta>;

export const PicksConfig: Story = {
  play: async ({ args, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;
    await screen.findByText(i18n.t("platforms:select_config"));

    await userEvent.click(await screen.findByText("Shef-AE-Met"));
    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:select") }),
    );

    await waitFor(() =>
      expect(args.onSelect).toHaveBeenCalledWith(
        expect.objectContaining({ configId: 51 }),
      ),
    );
  },
};

export const NoConfigsShowsEmptyMessage: Story = {
  parameters: {
    msw: { handlers: { configRefs: emptyConfigRefsHandler } },
  },
  play: async ({ mount, parameters }) => {
    await mount();
    const { i18n } = parameters;
    expect(
      await screen.findByText(i18n.t("platforms:select_config_none")),
    ).toBeInTheDocument();
  },
};

import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
import type { ApiAlgorithm, ApiAlgorithmRef } from "opendcs-api";
import { useCallback, useState } from "react";
import { expect, fn, screen, waitFor } from "storybook/test";
import { AlgorithmSelectModal } from "./AlgorithmSelectModal";

const mockAlgorithmRefs: ApiAlgorithmRef[] = [
  {
    algorithmId: 10,
    algorithmName: "AverageAlgorithm",
    execClass: "decodes.comp.AverageAlgorithm",
    description: "Reference description for average values.",
    numCompsUsing: 2,
  },
  {
    algorithmId: 11,
    algorithmName: "MaxToDate",
    execClass: "decodes.comp.MaxToDate",
    description: "Reference description for max values.",
    numCompsUsing: 1,
  },
];

const mockAlgorithms: ApiAlgorithm[] = [
  {
    algorithmId: 10,
    name: "AverageAlgorithm",
    execClass: "decodes.comp.AverageAlgorithm",
    description: "Full description loaded after selecting AverageAlgorithm.",
    props: {},
    parms: [
      { roleName: "input", parmType: "i" },
      { roleName: "output", parmType: "o" },
    ],
  },
  {
    algorithmId: 11,
    name: "MaxToDate",
    execClass: "decodes.comp.MaxToDate",
    description: "Full description loaded after selecting MaxToDate.",
    props: {},
    parms: [
      { roleName: "input", parmType: "i" },
      { roleName: "output", parmType: "o" },
    ],
  },
];

const getAlgorithm = async (algorithmId: number): Promise<ApiAlgorithm> => {
  const algorithm = mockAlgorithms.find((a) => a.algorithmId === algorithmId);
  if (!algorithm) throw new Error(`No algorithm with id ${algorithmId}`);
  return algorithm;
};

const handlers = {
  algorithmRefs: http.get("/odcsapi/algorithmrefs", () =>
    HttpResponse.json<ApiAlgorithmRef[]>(mockAlgorithmRefs),
  ),
};

type AlgorithmSelectModalDemoProps = {
  onSelect: (algo: ApiAlgorithmRef) => void | Promise<void>;
};

const AlgorithmSelectModalDemo: React.FC<AlgorithmSelectModalDemoProps> = ({
  onSelect,
}) => {
  const [show, setShow] = useState(true);

  const handleSelect = useCallback(
    async (algo: ApiAlgorithmRef) => {
      await onSelect(algo);
      setShow(false);
    },
    [onSelect],
  );

  return (
    <AlgorithmSelectModal
      show={show}
      onHide={() => setShow(false)}
      onSelect={handleSelect}
      getAlgorithm={getAlgorithm}
    />
  );
};

const meta = {
  component: AlgorithmSelectModalDemo,
  args: {
    onSelect: fn(),
  },
  parameters: {
    msw: {
      handlers,
    },
  },
} satisfies Meta<typeof AlgorithmSelectModalDemo>;

export default meta;

type Story = StoryObj<typeof meta>;

export const FilterAndSelect: Story = {
  play: async ({ args, mount, userEvent }) => {
    await mount();

    await screen.findByText("Select Algorithm");
    expect(await screen.findByText("AverageAlgorithm")).toBeInTheDocument();
    expect(await screen.findByText("MaxToDate")).toBeInTheDocument();

    await userEvent.type(
      screen.getByRole("searchbox", { name: "Filter algorithms" }),
      "max",
    );
    expect(screen.queryByText("AverageAlgorithm")).not.toBeInTheDocument();

    await userEvent.click(screen.getByText("MaxToDate"));
    await waitFor(() =>
      expect(
        screen.getByText("Full description loaded after selecting MaxToDate."),
      ).toBeInTheDocument(),
    );

    await userEvent.click(screen.getByRole("button", { name: "Select" }));

    await waitFor(() =>
      expect(args.onSelect).toHaveBeenCalledWith(mockAlgorithmRefs[1]),
    );
    await waitFor(() =>
      expect(screen.queryByText("Select Algorithm")).not.toBeInTheDocument(),
    );
  },
};

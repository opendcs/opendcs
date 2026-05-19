import type { Meta, StoryObj } from "@storybook/react-vite";
import { http, HttpResponse } from "msw";
import type { ApiAlgorithm, ApiAlgorithmRef } from "opendcs-api";
import { useCallback, useState } from "react";
import { expect, fn, screen, waitFor } from "storybook/test";
import { AlgorithmSelectModal } from "./AlgorithmSelectModal";

const emptyAlgorithmRefsHandler = http.get("/odcsapi/algorithmrefs", () =>
  HttpResponse.json<ApiAlgorithmRef[]>([]),
);

const failingGetAlgorithm = async (): Promise<ApiAlgorithm> => {
  throw new Error("simulated failure");
};

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
  play: async ({ args, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await screen.findByText(i18n.t("computations:editor.select_algorithm_title"));
    expect(await screen.findByText("AverageAlgorithm")).toBeInTheDocument();
    expect(await screen.findByText("MaxToDate")).toBeInTheDocument();

    await userEvent.type(
      screen.getByRole("searchbox", {
        name: i18n.t("computations:editor.select_algorithm_filter"),
      }),
      "max",
    );
    expect(screen.queryByText("AverageAlgorithm")).not.toBeInTheDocument();

    await userEvent.click(screen.getByText("MaxToDate"));
    await waitFor(() =>
      expect(screen.getByText(mockAlgorithms[1].description ?? "")).toBeInTheDocument(),
    );

    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:select") }),
    );

    await waitFor(() =>
      expect(args.onSelect).toHaveBeenCalledWith(mockAlgorithmRefs[1]),
    );
    await waitFor(() =>
      expect(
        screen.queryByText(i18n.t("computations:editor.select_algorithm_title")),
      ).not.toBeInTheDocument(),
    );
  },
};

export const FilterByExecClass: Story = {
  play: async ({ mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await screen.findByText("AverageAlgorithm");
    await userEvent.type(
      screen.getByRole("searchbox", {
        name: i18n.t("computations:editor.select_algorithm_filter"),
      }),
      "MaxToDate",
    );

    await waitFor(() =>
      expect(screen.queryByText("AverageAlgorithm")).not.toBeInTheDocument(),
    );
    expect(screen.getByText("MaxToDate")).toBeInTheDocument();
  },
};

export const FilterByDescription: Story = {
  play: async ({ mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await screen.findByText("AverageAlgorithm");
    await userEvent.type(
      screen.getByRole("searchbox", {
        name: i18n.t("computations:editor.select_algorithm_filter"),
      }),
      "max values",
    );

    await waitFor(() =>
      expect(screen.queryByText("AverageAlgorithm")).not.toBeInTheDocument(),
    );
    expect(screen.getByText("MaxToDate")).toBeInTheDocument();
  },
};

export const NoAlgorithmsFound: Story = {
  parameters: {
    msw: { handlers: { algorithmRefs: emptyAlgorithmRefsHandler } },
  },
  play: async ({ mount, parameters }) => {
    await mount();
    const { i18n } = parameters;

    expect(
      await screen.findByText(i18n.t("computations:editor.select_algorithm_none")),
    ).toBeInTheDocument();
  },
};

export const NoAlgorithmsAfterFilter: Story = {
  play: async ({ mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await screen.findByText("AverageAlgorithm");
    await userEvent.type(
      screen.getByRole("searchbox", {
        name: i18n.t("computations:editor.select_algorithm_filter"),
      }),
      "no-such-algorithm-xyz",
    );

    expect(
      await screen.findByText(i18n.t("computations:editor.select_algorithm_none")),
    ).toBeInTheDocument();
  },
};

export const SortByNameColumn: Story = {
  play: async ({ mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await screen.findByText("AverageAlgorithm");

    const readRowOrder = (): string[] => {
      const cells = screen
        .getAllByRole("row")
        .slice(1)
        .map((row) => row.querySelectorAll("td")[1]?.textContent ?? "");
      return cells;
    };

    expect(readRowOrder()).toEqual(["AverageAlgorithm", "MaxToDate"]);

    const nameHeader = screen.getByRole("columnheader", {
      name: new RegExp(i18n.t("computations:editor.name"), "i"),
    });
    // DataTables marks orderable headers with `dt-orderable-asc` and renders a
    // `.dt-column-order` arrow indicator inside the header — proves the sort
    // handlers got wired up.
    expect(nameHeader.classList.contains("dt-orderable-asc")).toBe(true);
    expect(nameHeader.querySelector(".dt-column-order")).not.toBeNull();

    await userEvent.click(nameHeader);
    await userEvent.click(nameHeader);
    await waitFor(() =>
      expect(readRowOrder()).toEqual(["MaxToDate", "AverageAlgorithm"]),
    );
    expect(nameHeader.getAttribute("aria-sort")).toBe("descending");

    await userEvent.click(nameHeader);
    await waitFor(() =>
      expect(readRowOrder()).toEqual(["AverageAlgorithm", "MaxToDate"]),
    );
    expect(nameHeader.getAttribute("aria-sort")).toBe("ascending");
  },
};

export const CancelButtonClosesModal: Story = {
  play: async ({ args, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await screen.findByText(i18n.t("computations:editor.select_algorithm_title"));

    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:cancel") }),
    );

    await waitFor(() =>
      expect(
        screen.queryByText(i18n.t("computations:editor.select_algorithm_title")),
      ).not.toBeInTheDocument(),
    );
    expect(args.onSelect).not.toHaveBeenCalled();
  },
};

const NoGetAlgorithmDemo: React.FC<AlgorithmSelectModalDemoProps> = ({ onSelect }) => {
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
    />
  );
};

export const WithoutGetAlgorithmFallsBackToRefDescription: Story = {
  render: (args) => <NoGetAlgorithmDemo onSelect={args.onSelect} />,
  play: async ({ args, mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await screen.findByText(i18n.t("computations:editor.select_algorithm_title"));
    await userEvent.click(await screen.findByText("AverageAlgorithm"));

    await waitFor(() =>
      expect(
        screen.getByText(mockAlgorithmRefs[0].description ?? ""),
      ).toBeInTheDocument(),
    );

    await userEvent.click(
      screen.getByRole("button", { name: i18n.t("translation:select") }),
    );

    await waitFor(() =>
      expect(args.onSelect).toHaveBeenCalledWith(mockAlgorithmRefs[0]),
    );
  },
};

const FailingDemo: React.FC<AlgorithmSelectModalDemoProps> = ({ onSelect }) => {
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
      getAlgorithm={failingGetAlgorithm}
    />
  );
};

export const GetAlgorithmErrorFallsBackToRefDescription: Story = {
  render: (args) => <FailingDemo onSelect={args.onSelect} />,
  play: async ({ mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await screen.findByText(i18n.t("computations:editor.select_algorithm_title"));
    await userEvent.click(await screen.findByText("AverageAlgorithm"));

    await waitFor(() =>
      expect(
        screen.getByText(mockAlgorithmRefs[0].description ?? ""),
      ).toBeInTheDocument(),
    );
  },
};

type RefWithoutDescription = ApiAlgorithmRef;

const refsMissingDescriptionHandler = http.get("/odcsapi/algorithmrefs", () =>
  HttpResponse.json<RefWithoutDescription[]>([
    {
      algorithmId: 99,
      algorithmName: "BlankDescriptionAlgo",
      execClass: "decodes.comp.BlankDescriptionAlgo",
      numCompsUsing: 0,
    },
  ]),
);

export const NoDescriptionFallbackMessage: Story = {
  parameters: {
    msw: { handlers: { algorithmRefs: refsMissingDescriptionHandler } },
  },
  render: (args) => <NoGetAlgorithmDemo onSelect={args.onSelect} />,
  play: async ({ mount, parameters, userEvent }) => {
    await mount();
    const { i18n } = parameters;

    await userEvent.click(await screen.findByText("BlankDescriptionAlgo"));
    expect(
      await screen.findByText(
        i18n.t("computations:editor.select_algorithm_no_description"),
      ),
    ).toBeInTheDocument();
  },
};

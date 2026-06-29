import type { Meta, StoryObj } from "@storybook/react-vite";
import { act, useMemo } from "react";
import { http, HttpResponse } from "msw";
import type { ApiDataType, ApiPresentationElement } from "opendcs-api";
import { expect, fn, waitFor } from "storybook/test";
import { useQueryClient } from "@tanstack/react-query";
import { PresentationElementsTable } from "./PresentationElementsTable";

const ELEMENTS: ApiPresentationElement[] = [
  {
    dataTypeStd: "SHEF-PE",
    dataTypeCode: "HG",
    units: "ft",
    fractionalDigits: 2,
    min: 0,
    max: 100,
  },
];

const MOCK_DATA_TYPES: ApiDataType[] = [
  { standard: "SHEF-PE", code: "HG", displayName: "Stage" },
  { standard: "CWMS", code: "Stage", displayName: "Stage" },
];

const baseHandlers = {
  dataTypes: http.get("/odcsapi/datatypelist", () =>
    HttpResponse.json<ApiDataType[]>(MOCK_DATA_TYPES),
  ),
};

const meta = {
  component: PresentationElementsTable,
  parameters: { msw: { handlers: baseHandlers } },
  args: {
    elements: ELEMENTS,
    edit: false,
    onSave: fn(),
    onRemove: fn(),
  },
} satisfies Meta<typeof PresentationElementsTable>;

export default meta;
type Story = StoryObj<typeof meta>;

// Read-only render — verifies caption and element data appear without edit controls.
export const ReadOnly: Story = {
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(
      await canvas.findByText(i18n.t("presentations:elements.title")),
    ).toBeInTheDocument();
    expect(await canvas.findByText("HG")).toBeInTheDocument();
  },
};

// Edit mode: open an element row — the dataTypeStd column renders a <select>
// populated from the data-type list (covers dataTypeStandards.map render path).
export const EditModeSelectOptions: Story = {
  args: { edit: true },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:elements.edit", { name: "HG" }),
    });
    await act(async () => userEvent.click(editBtn));
    await waitFor(() => {
      const sel = canvas.getByLabelText(
        i18n.t("presentations:elements.dataTypeStd_input", { name: "HG" }),
      ) as HTMLSelectElement;
      expect(sel).toBeInTheDocument();
      expect(sel.querySelector('option[value="SHEF-PE"]')).not.toBeNull();
    });
  },
};

// Data types without a standard field — exercises the if (dt.standard) false
// branch inside the dataTypeStandards memo.
export const DataTypesWithoutStandard: Story = {
  parameters: {
    msw: {
      handlers: {
        dataTypes: http.get("/odcsapi/datatypelist", () =>
          HttpResponse.json<ApiDataType[]>([
            { code: "HG", displayName: "Stage" }, // no standard
            { standard: "CWMS", code: "Stage", displayName: "Stage" },
          ]),
        ),
      },
    },
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(
      await canvas.findByText(i18n.t("presentations:elements.title")),
    ).toBeInTheDocument();
  },
};

// Elements without a dataTypeStd — exercises the if (el.dataTypeStd) false
// branch inside the dataTypeStandards memo.
export const ElementsWithoutDataTypeStd: Story = {
  args: {
    elements: [
      { dataTypeCode: "HG", units: "ft" },
      { dataTypeStd: "SHEF-PE", dataTypeCode: "QR", units: "cfs" },
    ],
  },
  play: async ({ mount, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    expect(
      await canvas.findByText(i18n.t("presentations:elements.title")),
    ).toBeInTheDocument();
  },
};

// Save an edited element — exercises the edit.read callbacks for dataTypeStd
// (select) and units (select/input), plus the onSave validation success path.
export const SaveEditedElement: Story = {
  args: { edit: true },
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:elements.edit", { name: "HG" }),
    });
    await act(async () => userEvent.click(editBtn));
    await waitFor(() => {
      expect(
        canvas.getByRole("button", {
          name: i18n.t("presentations:elements.save_edit", { name: "HG" }),
        }),
      ).toBeInTheDocument();
    });
    const saveBtn = canvas.getByRole("button", {
      name: i18n.t("presentations:elements.save_edit", { name: "HG" }),
    });
    await act(async () => userEvent.click(saveBtn));
    await waitFor(() => {
      expect(
        canvas.queryByRole("button", {
          name: i18n.t("presentations:elements.save_edit", { name: "HG" }),
        }),
      ).not.toBeInTheDocument();
    });
  },
};

// Units not ready: clears the cache pre-seeded by WithUnits so the unit list
// query stays in a non-success state. When a row is opened for editing the
// units column falls back to a plain <input type="text"> instead of UnitSelect.
export const UnitsNotReady: Story = {
  parameters: {
    msw: {
      handlers: {
        ...baseHandlers,
        units: http.get("/odcsapi/unitlist", () => HttpResponse.error()),
      },
    },
  },
  args: { edit: true },
  decorators: [
    (Story) => {
      const queryClient = useQueryClient();
      // Clear the synchronously-seeded unit data so useUnitListQuery refetches
      // and gets the error response above — keeping isSuccess = false.
      useMemo(() => {
        queryClient.removeQueries({ queryKey: ["units"] });
      }, [queryClient]);
      return <Story />;
    },
  ],
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;
    const editBtn = await canvas.findByRole("button", {
      name: i18n.t("presentations:elements.edit", { name: "HG" }),
    });
    await act(async () => userEvent.click(editBtn));
    await waitFor(() => {
      const unitsField = canvas.getByLabelText(
        i18n.t("presentations:elements.units_input", { name: "HG" }),
      );
      // Fallback renders a plain text input, not a select.
      expect(unitsField.tagName.toLowerCase()).toBe("input");
    });
  },
};

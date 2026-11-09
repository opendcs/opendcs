import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ApiException } from "opendcs-api";
import { expect, screen, spyOn, waitFor } from "storybook/test";
import { QueryProvider } from "../contexts/app/QueryProvider";
import { DELETE_TOASTS } from "../contexts/app/mutationToasts";
import { dismissToast, getToasts } from "../util/toastBus";

// Drives the real QueryProvider, so these stories cover the whole loop:
// mutation meta -> MutationCache callback -> toastBus -> ToastStack.
const DeleteButtons: React.FC = () => {
  const succeed = useMutation({
    mutationFn: () => Promise.resolve(),
    meta: DELETE_TOASTS,
  });
  const fail = useMutation({
    mutationFn: () =>
      Promise.reject(
        new ApiException(
          409,
          "Site is in use and cannot be deleted",
          { message: "Cannot perform operation because the data is in use." },
          {},
        ),
      ),
    meta: DELETE_TOASTS,
  });
  return (
    <>
      <button onClick={() => succeed.mutate()}>delete ok</button>
      <button onClick={() => fail.mutate()}>delete in use</button>
    </>
  );
};

const BrokenQuery: React.FC = () => {
  useQuery({
    queryKey: ["toast-stack-broken-query"],
    queryFn: () => Promise.reject(new Error("boom")),
    retry: false,
  });
  return null;
};

const Harness: React.FC = () => (
  <QueryProvider>
    <DeleteButtons />
  </QueryProvider>
);

const meta = {
  component: Harness,
  // toastBus is module state, so a toast from an earlier story (or a retry)
  // would still be on screen.
  beforeEach: () => getToasts().forEach((t) => dismissToast(t.id)),
} satisfies Meta<typeof Harness>;

export default meta;

type Story = StoryObj<typeof meta>;

export const DeleteSucceeded: Story = {
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const button = canvas.getByRole("button", { name: "delete ok" });
    await act(() => userEvent.click(button));

    const toast = await screen.findByRole("alert");
    expect(toast).toHaveTextContent(i18n.t("translation:delete_success"));
  },
};

export const DeleteFailedShowsServerMessage: Story = {
  play: async ({ mount, userEvent, parameters }) => {
    const canvas = await mount();
    const { i18n } = parameters;

    const button = canvas.getByRole("button", { name: "delete in use" });
    await act(() => userEvent.click(button));

    const toast = await screen.findByRole("alert");
    expect(toast).toHaveTextContent(i18n.t("translation:delete_failed"));
    expect(toast).toHaveTextContent(
      "Cannot perform operation because the data is in use.",
    );

    const close = await screen.findByRole("button", {
      name: i18n.t("translation:close"),
    });
    await act(() => userEvent.click(close));
    await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument());
  },
};

// Only mutations opt into toasts; a failed query is logged and nothing more.
export const FailedQueryLogsWithoutToast: Story = {
  render: () => (
    <QueryProvider>
      <BrokenQuery />
    </QueryProvider>
  ),
  play: async ({ mount }) => {
    const errorSpy = spyOn(console, "error").mockImplementation(() => {});
    await mount();

    await waitFor(() =>
      expect(errorSpy).toHaveBeenCalledWith("[query]", expect.any(Error)),
    );
    expect(getToasts()).toEqual([]);
  },
};

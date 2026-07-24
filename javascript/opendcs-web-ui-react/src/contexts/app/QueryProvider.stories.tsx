import type { Meta, StoryObj } from "@storybook/react-vite";
import { act } from "react";
import { useMutation } from "@tanstack/react-query";
import { expect, waitFor } from "storybook/test";
import { QueryProvider } from "./QueryProvider";
import { ToastStack } from "../../components/notifications/ToastStack";

// Exercises QueryProvider's mutationCache wiring end-to-end: a mutation's
// meta.successMessage / meta.errorContext drive a toast via toastBus, and
// ToastStack renders whatever toastBus currently holds.
const MutationHarness: React.FC = () => {
  const succeed = useMutation({
    mutationFn: async () => "ok",
    meta: { successMessage: "Saved successfully" },
  });
  const failWithBody = useMutation({
    mutationFn: async () => {
      throw { body: { message: "Name already in use" } };
    },
    meta: { errorContext: "Save" },
  });
  const failPlain = useMutation({
    mutationFn: async () => {
      throw new Error("HTTP-Code: 500\nMessage: Server exploded\nURL: /x");
    },
  });

  return (
    <>
      <button onClick={() => succeed.mutate()}>Trigger Success</button>
      <button onClick={() => failWithBody.mutate()}>Trigger Body Error</button>
      <button onClick={() => failPlain.mutate()}>Trigger Plain Error</button>
      <ToastStack />
    </>
  );
};

const Harness: React.FC = () => (
  <QueryProvider>
    <MutationHarness />
  </QueryProvider>
);

const meta = {
  component: Harness,
} satisfies Meta<typeof Harness>;

export default meta;

type Story = StoryObj<typeof meta>;

export const SuccessShowsToast: Story = {
  play: async ({ mount, userEvent }) => {
    const canvas = await mount();
    await act(async () => userEvent.click(canvas.getByText("Trigger Success")));
    await waitFor(() =>
      expect(canvas.getByText("Saved successfully")).toBeInTheDocument(),
    );
  },
};

export const ErrorWithBodyMessageShowsPrefixedToast: Story = {
  play: async ({ mount, userEvent }) => {
    const canvas = await mount();
    await act(async () => userEvent.click(canvas.getByText("Trigger Body Error")));
    await waitFor(() =>
      expect(canvas.getByText("Save: Name already in use")).toBeInTheDocument(),
    );
  },
};

export const ErrorWithoutContextShowsExtractedMessage: Story = {
  play: async ({ mount, userEvent }) => {
    const canvas = await mount();
    await act(async () => userEvent.click(canvas.getByText("Trigger Plain Error")));
    await waitFor(() =>
      expect(canvas.getByText("Server exploded")).toBeInTheDocument(),
    );
  },
};

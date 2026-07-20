import type { Decorator, Meta, StoryObj } from "@storybook/react-vite";
import { expect } from "storybook/test";
import { http, HttpResponse } from "msw";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AppVersion } from "./AppVersion";

const meta = {
  component: AppVersion,
} satisfies Meta<typeof AppVersion>;

export default meta;

type Story = StoryObj<typeof meta>;

// The global WithVersion decorator always pre-seeds a ready query, so every
// other story that renders AppVersion only ever exercises the isSuccess
// branch. Give this component its own fresh QueryClient so the pending
// (isSuccess === false) branch is reachable too.
const freshQueryClientDecorator: Decorator = (Story) => {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Infinity } },
  });
  return (
    <QueryClientProvider client={client}>
      <Story />
    </QueryClientProvider>
  );
};

export const Loading: Story = {
  decorators: [freshQueryClientDecorator],
  parameters: {
    msw: {
      handlers: [
        // Never resolves, so the query stays pending and the component's
        // "render nothing until success" branch stays observable.
        http.get("/odcsapi/version", () => new Promise(() => {})),
      ],
    },
  },
  play: async ({ canvasElement, mount }) => {
    await mount();
    expect(canvasElement.textContent).toBe("");
  },
};

export const Loaded: Story = {
  decorators: [freshQueryClientDecorator],
  parameters: {
    msw: {
      handlers: [
        http.get("/odcsapi/version", () =>
          HttpResponse.json({
            version: "99.main-SNAPSHOT",
            commitHash: "0123456789abcdef0123456789abcdef01234567",
          }),
        ),
      ],
    },
  },
  play: async ({ mount }) => {
    const canvas = await mount();
    const text = await canvas.findByText(/0123456/);
    expect(text).toBeInTheDocument();
  },
};

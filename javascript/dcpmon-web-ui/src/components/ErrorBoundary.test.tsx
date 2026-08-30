import { render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ErrorBoundary } from "./ErrorBoundary";

function BrokenContent(): ReactNode {
  throw new Error("render failure");
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe("ErrorBoundary", () => {
  it("contains a rendering failure and shows its local fallback", () => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);

    render(
      <div>
        <h1>Still available</h1>
        <ErrorBoundary fallback={<div role="alert">Section unavailable</div>}>
          <BrokenContent />
        </ErrorBoundary>
      </div>,
    );

    expect(screen.getByRole("heading", { name: "Still available" })).toBeVisible();
    expect(screen.getByRole("alert")).toHaveTextContent("Section unavailable");
  });
});

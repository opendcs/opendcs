import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const styles = readFileSync(resolve(process.cwd(), "src/styles.css"), "utf8");

function cssBlock(selector: string) {
  const match = styles.match(
    new RegExp(`${selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}\\s*\\{([^}]+)\\}`),
  );
  if (!match?.[1]) throw new Error(`Missing CSS block: ${selector}`);
  return match[1];
}

function customProperty(block: string, property: string) {
  const match = block.match(new RegExp(`${property}:\\s*(#[0-9a-f]{6})`, "i"));
  if (!match?.[1]) throw new Error(`Missing CSS property: ${property}`);
  return match[1];
}

function channel(value: number) {
  const srgb = value / 255;
  return srgb <= 0.04045
    ? srgb / 12.92
    : Math.pow((srgb + 0.055) / 1.055, 2.4);
}

function luminance(hex: string) {
  const value = hex.replace("#", "");
  const [red, green, blue] = [0, 2, 4].map((offset) =>
    Number.parseInt(value.slice(offset, offset + 2), 16),
  );
  return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue);
}

function contrast(foreground: string, background: string) {
  const first = luminance(foreground);
  const second = luminance(background);
  return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05);
}

function blend(foreground: string, background: string, alpha: number) {
  const color = [0, 2, 4]
    .map((offset) => {
      const foregroundChannel = Number.parseInt(
        foreground.replace("#", "").slice(offset, offset + 2),
        16,
      );
      const backgroundChannel = Number.parseInt(
        background.replace("#", "").slice(offset, offset + 2),
        16,
      );
      return Math.round(
        foregroundChannel * alpha + backgroundChannel * (1 - alpha),
      )
        .toString(16)
        .padStart(2, "0");
    })
    .join("");
  return `#${color}`;
}

describe("DCPMon theme contrast", () => {
  const light = cssBlock(":root");
  const dark = cssBlock('[data-bs-theme="dark"]');
  const lightSecondary = customProperty(light, "--odcs-text-secondary");
  const darkSecondary = customProperty(dark, "--odcs-text-secondary");
  const lightBorder = customProperty(light, "--odcs-border");
  const darkBorder = customProperty(dark, "--odcs-border");

  it("keeps normal and condition-muted text at 4.5:1 or greater", () => {
    const lightCondition = blend("#ffc107", "#f8f9fa", 0.1);
    const darkCondition = blend("#ffc107", "#2b3035", 0.1);

    expect(contrast(lightSecondary, "#ffffff")).toBeGreaterThanOrEqual(4.5);
    expect(contrast(lightSecondary, "#f9fafb")).toBeGreaterThanOrEqual(4.5);
    expect(contrast(lightSecondary, lightCondition)).toBeGreaterThanOrEqual(4.5);
    expect(contrast(darkSecondary, "#212529")).toBeGreaterThanOrEqual(4.5);
    expect(contrast(darkSecondary, "#111827")).toBeGreaterThanOrEqual(4.5);
    expect(contrast(darkSecondary, darkCondition)).toBeGreaterThanOrEqual(4.5);
  });

  it("keeps control boundaries at 3:1 or greater", () => {
    expect(contrast(lightBorder, "#ffffff")).toBeGreaterThanOrEqual(3);
    expect(contrast(lightBorder, "#f9fafb")).toBeGreaterThanOrEqual(3);
    expect(contrast(darkBorder, "#212529")).toBeGreaterThanOrEqual(3);
    expect(contrast(darkBorder, "#111827")).toBeGreaterThanOrEqual(3);
  });

  it("uses dark text on Bootstrap warning and info badges", () => {
    expect(contrast("#111827", "#ffc107")).toBeGreaterThanOrEqual(4.5);
    expect(contrast("#111827", "#0dcaf0")).toBeGreaterThanOrEqual(4.5);
    expect(styles).toMatch(
      /\.badge\.bg-warning,\s*\.badge\.bg-info\s*\{\s*color:\s*#111827;/,
    );
  });
});

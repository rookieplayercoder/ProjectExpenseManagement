import { describe, it, expect } from "vitest";
import { formatCurrency, formatDate } from "@/utils/format";

describe("formatCurrency", () => {
  it("formats a known currency", () => {
    expect(formatCurrency(1234.5, "USD")).toContain("1,234.5");
  });

  it("falls back to a plain number for an invalid currency code", () => {
    expect(formatCurrency(10, "XXX_BAD")).toBe("10.00 XXX_BAD");
  });
});

describe("formatDate", () => {
  it("formats a valid ISO date", () => {
    expect(formatDate("2026-01-15")).toMatch(/Jan/);
  });

  it("returns the raw input for an invalid date", () => {
    expect(formatDate("not-a-date")).toBe("not-a-date");
  });
});

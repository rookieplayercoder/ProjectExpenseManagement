import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { ExpenseCard } from "./ExpenseCard";

const expense = {
  expenseId: "e1",
  title: "Dinner",
  totalAmount: 42.5,
  currencyCode: "USD",
  paidByUserId: "u1",
  paidByName: "Alex",
  expenseDate: "2026-01-10",
  splitType: "EQUAL",
  groupName: "Roommates",
} as const;

describe("ExpenseCard", () => {
  it("renders expense details and links to its detail page", () => {
    render(
      <MemoryRouter>
        <ExpenseCard expense={expense} />
      </MemoryRouter>
    );

    expect(screen.getByText("Dinner")).toBeInTheDocument();
    expect(screen.getByText(/Roommates/)).toBeInTheDocument();
    expect(screen.getByText(/paid by Alex/)).toBeInTheDocument();
    expect(screen.getByRole("link")).toHaveAttribute("href", "/expenses/e1");
  });
});

import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import DashboardPage from "./DashboardPage";
import { useDashboardData } from "@/hooks/useDashboardData";

vi.mock("@/hooks/useDashboardData");

describe("DashboardPage (integration)", () => {
  it("shows an error state when the dashboard fails to load", () => {
    vi.mocked(useDashboardData).mockReturnValue({
      data: null,
      isLoading: false,
      isError: true,
    } as unknown as ReturnType<typeof useDashboardData>);

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/Couldn't load your dashboard/i)).toBeInTheDocument();
  });

  it("renders groups and recent expenses once data loads", () => {
    vi.mocked(useDashboardData).mockReturnValue({
      isLoading: false,
      isError: false,
      data: {
        groups: [{ groupId: "g1", name: "Roommates", memberCount: 3 }],
        youOwe: [],
        youAreOwed: [],
        netBalance: [],
        totalExpenses: [],
        recentExpenses: [
          {
            expenseId: "e1",
            title: "Dinner",
            totalAmount: 42.5,
            currencyCode: "USD",
            paidByName: "Alex",
            expenseDate: "2026-01-10",
            splitType: "EQUAL",
            groupId: "g1",
            groupName: "Roommates",
          },
        ],
        allExpenses: [],
        recentSettlements: [],
      },
    } as unknown as ReturnType<typeof useDashboardData>);

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    expect(screen.getByText("Dinner")).toBeInTheDocument();
    expect(screen.getAllByText(/Roommates/).length).toBeGreaterThan(0);
  });
});

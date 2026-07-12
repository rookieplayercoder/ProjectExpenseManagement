import { test, expect } from "@playwright/test";

test("redirects unauthenticated users from a protected route to /login", async ({ page }) => {
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/login$/);
});

test("login page renders its form", async ({ page }) => {
  await page.goto("/login");
  await expect(page.getByRole("button", { name: /log in|sign in/i })).toBeVisible();
});

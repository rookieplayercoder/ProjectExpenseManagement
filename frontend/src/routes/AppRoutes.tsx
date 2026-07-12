import { lazy, Suspense } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { AppLayout } from "@/layouts/AppLayout";
import { PageLoader } from "@/components/ui/page-loader";

const LoginPage = lazy(() => import("@/pages/auth/LoginPage"));
const RegisterPage = lazy(() => import("@/pages/auth/RegisterPage"));
const DashboardPage = lazy(() => import("@/pages/dashboard/DashboardPage"));
const GroupsPage = lazy(() => import("@/pages/groups/GroupsPage"));
const GroupDetailsPage = lazy(() => import("@/pages/groups/GroupDetailsPage"));
const CreateExpensePage = lazy(() => import("@/pages/expenses/CreateExpensePage"));
const EditExpensePage = lazy(() => import("@/pages/expenses/EditExpensePage"));
const ExpenseDetailsPage = lazy(() => import("@/pages/expenses/ExpenseDetailsPage"));
const ProfilePage = lazy(() => import("@/pages/profile/ProfilePage"));
const NotFoundPage = lazy(() => import("@/pages/NotFoundPage"));

export function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Everything below requires a valid session */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/groups" element={<GroupsPage />} />
            <Route path="/groups/:groupId" element={<GroupDetailsPage />} />
            <Route path="/groups/:groupId/expenses/new" element={<CreateExpensePage />} />
            <Route path="/expenses/:expenseId" element={<ExpenseDetailsPage />} />
            <Route path="/expenses/:expenseId/edit" element={<EditExpensePage />} />
            <Route path="/profile" element={<ProfilePage />} />
          </Route>
        </Route>

        <Route path="/404" element={<NotFoundPage />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>
    </Suspense>
  );
}

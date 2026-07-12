import { useNavigate } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { Button, type ButtonProps } from "@/components/ui/button";
import { LogOut } from "lucide-react";

export function LogoutButton(props: Omit<ButtonProps, "onClick">) {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <Button variant="ghost" onClick={handleLogout} {...props}>
      <LogOut className="h-4 w-4" />
      Log out
    </Button>
  );
}

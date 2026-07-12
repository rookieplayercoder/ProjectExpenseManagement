import { useMyProfile } from "@/hooks/useMyProfile";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar } from "@/components/ui/avatar";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/ui/error-state";
import { LogoutButton } from "@/components/auth/LogoutButton";
import { Mail, Phone, Calendar, ShieldCheck } from "lucide-react";
import { formatDate } from "@/utils/format";

export default function ProfilePage() {
  const { data: profile, isLoading, isError } = useMyProfile();

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <div>
        <h1 className="text-2xl font-semibold">Profile</h1>
        <p className="text-sm text-muted-foreground">Your account details.</p>
      </div>

      {isError ? (
        <ErrorState description="Couldn't load your profile. Please try again." />
      ) : isLoading || !profile ? (
        <Skeleton className="h-64" />
      ) : (
        <Card>
          <CardHeader className="flex flex-row items-center gap-4 space-y-0">
            <Avatar name={profile.fullName} size="lg" />
            <div>
              <CardTitle className="text-lg">{profile.fullName}</CardTitle>
              <Badge variant="secondary" className="mt-1">
                {profile.role}
              </Badge>
            </div>
          </CardHeader>
          <CardContent className="space-y-3 border-t border-border pt-4 text-sm">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Mail className="h-4 w-4" />
              {profile.email}
            </div>
            {profile.mobileNumber && (
              <div className="flex items-center gap-2 text-muted-foreground">
                <Phone className="h-4 w-4" />
                {profile.mobileNumber}
              </div>
            )}
            <div className="flex items-center gap-2 text-muted-foreground">
              <Calendar className="h-4 w-4" />
              Member since {formatDate(profile.createdAt)}
            </div>
            <div className="flex items-center gap-2 text-muted-foreground">
              <ShieldCheck className="h-4 w-4" />
              User ID: <span className="font-mono text-xs">{profile.userId}</span>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Session</CardTitle>
        </CardHeader>
        <CardContent>
          <LogoutButton variant="outline" />
        </CardContent>
      </Card>
    </div>
  );
}

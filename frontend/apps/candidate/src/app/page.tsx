import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@interview/shared/components/ui/card";

export default function HomePage() {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-8">
      <div className="max-w-md w-full">
        <Card>
          <CardHeader>
            <CardTitle>AI 程式面試平台</CardTitle>
            <CardDescription>
              請使用面試官提供的邀請連結進入面試。
              <br />
              直接輸入此頁面 URL 無法加入面試。
            </CardDescription>
          </CardHeader>
        </Card>
      </div>
    </div>
  );
}

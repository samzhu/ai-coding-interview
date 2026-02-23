import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default function HomePage() {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-8">
      <div className="max-w-4xl w-full space-y-8">
        <div className="text-center">
          <h1 className="text-4xl font-bold mb-4">AI 程式面試平台</h1>
          <p className="text-muted-foreground text-lg">
            由 AI 輔助的程式技術面試系統
          </p>
        </div>

        <div className="grid gap-6 md:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>面試官</CardTitle>
              <CardDescription>
                建立面試、選擇題目，產生候選人邀請連結
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Button asChild className="w-full">
                <Link href="/admin">進入管理後台</Link>
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>候選人</CardTitle>
              <CardDescription>
                使用面試官提供的邀請連結加入面試
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                請直接點擊面試官傳送給您的邀請連結
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

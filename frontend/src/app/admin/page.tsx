import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default function AdminPage() {
  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold mb-8">面試管理後台</h1>

        <div className="grid gap-6 md:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>建立面試</CardTitle>
              <CardDescription>
                選擇題目、設定候選人資訊，產生邀請連結
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Button asChild>
                <Link href="/admin/interviews/new">建立新面試</Link>
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>管理面試</CardTitle>
              <CardDescription>
                查看所有面試清單、狀態與詳情
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Button asChild variant="outline">
                <Link href="/admin/interviews">查看面試列表</Link>
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>題目管理</CardTitle>
              <CardDescription>
                建立、編輯、刪除面試題目與評估指標
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Button asChild variant="outline">
                <Link href="/admin/questions">管理題目</Link>
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

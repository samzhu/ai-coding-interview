const CONSTRAINTS = [
  { label: "Memory", value: "1 GB" },
  { label: "CPU", value: "1 core" },
  { label: "Network", value: "允許（可連外網）" },
  { label: "Shell", value: "bash -c" },
  { label: "exam.yml 路徑", value: "/workspace/exam.yml" },
  { label: "預設工作目錄", value: "/workspace" },
];

export function ConstraintTable() {
  return (
    <div className="rounded-lg border border-border overflow-hidden">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-muted/50">
            <th className="text-left px-4 py-2 font-medium text-muted-foreground">
              限制條件
            </th>
            <th className="text-left px-4 py-2 font-medium text-muted-foreground">
              值
            </th>
          </tr>
        </thead>
        <tbody>
          {CONSTRAINTS.map((row) => (
            <tr
              key={row.label}
              className="border-t border-border hover:bg-muted/30 transition-colors"
            >
              <td className="px-4 py-2 text-foreground/80">{row.label}</td>
              <td className="px-4 py-2 font-mono text-primary/80">{row.value}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

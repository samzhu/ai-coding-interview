interface SchemaFieldProps {
  name: string;
  type: string;
  required: boolean;
  defaultValue?: string;
  children: React.ReactNode;
}

export function SchemaField({
  name,
  type,
  required,
  defaultValue,
  children,
}: SchemaFieldProps) {
  return (
    <div className="py-3 border-b border-border last:border-0">
      {/* 第一層：欄位名 + type badge + required badge — flex-wrap 不怕長名稱溢出 */}
      <div className="flex flex-wrap items-center gap-2 mb-1.5">
        <span className="font-mono text-sm bg-primary/10 text-primary px-1.5 py-0.5 rounded whitespace-nowrap">
          {name}
        </span>
        <span className="text-xs font-mono text-muted-foreground bg-muted px-1.5 py-0.5 rounded">
          {type}
        </span>
        {required ? (
          <span className="text-xs font-medium text-red-400 bg-red-400/10 px-1.5 py-0.5 rounded">
            required
          </span>
        ) : (
          <span className="text-xs text-muted-foreground/70 bg-muted px-1.5 py-0.5 rounded">
            optional
          </span>
        )}
      </div>
      {/* 第二層：說明文字獨佔一行，不與 badges 競爭空間 */}
      <div className="text-sm text-muted-foreground pl-0.5">
        {children}
        {defaultValue && (
          <span className="ml-2 text-xs text-muted-foreground/60">
            預設：
            <code className="font-mono bg-muted px-1 rounded">{defaultValue}</code>
          </span>
        )}
      </div>
    </div>
  );
}

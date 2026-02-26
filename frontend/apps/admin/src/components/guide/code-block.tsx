interface CodeBlockProps {
  title?: string;
  language?: string;
  children: string;
  annotations?: Record<number, string>;
}

const LANGUAGE_BADGE_COLORS: Record<string, string> = {
  yaml: "bg-amber-500/15 text-amber-400",
  dockerfile: "bg-sky-500/15 text-sky-400",
  bash: "bg-emerald-500/15 text-emerald-400",
  java: "bg-orange-500/15 text-orange-400",
  python: "bg-blue-500/15 text-blue-400",
  javascript: "bg-yellow-500/15 text-yellow-400",
  typescript: "bg-blue-400/15 text-blue-300",
};

export function CodeBlock({
  title,
  language,
  children,
  annotations = {},
}: CodeBlockProps) {
  const lines = children.trimEnd().split("\n");
  const badgeClass =
    language && LANGUAGE_BADGE_COLORS[language.toLowerCase()]
      ? LANGUAGE_BADGE_COLORS[language.toLowerCase()]
      : "bg-muted text-muted-foreground";

  return (
    <div className="rounded-lg overflow-hidden border border-border my-4">
      {/* 標題列 */}
      {(title || language) && (
        <div className="flex items-center justify-between px-4 py-2 bg-[#2d2d2d] border-b border-border">
          {title ? (
            <span className="font-mono text-sm text-muted-foreground">
              {title}
            </span>
          ) : (
            <span />
          )}
          {language && (
            <span
              className={`text-xs font-mono px-2 py-0.5 rounded ${badgeClass}`}
            >
              {language}
            </span>
          )}
        </div>
      )}

      {/* 程式碼區 */}
      <div className="bg-[#1e1e1e] p-4 overflow-x-auto">
        <table className="w-full border-collapse">
          <tbody>
            {lines.map((line, idx) => {
              const lineNum = idx + 1;
              const annotation = annotations[lineNum];
              return (
                <tr key={idx}>
                  {/* 行號 */}
                  <td className="select-none w-8 text-right pr-4 font-mono text-xs text-muted-foreground/40 align-top leading-relaxed">
                    {lineNum}
                  </td>
                  {/* 程式碼 */}
                  <td className="font-mono text-sm leading-relaxed text-slate-200 whitespace-pre">
                    {line}
                    {annotation && (
                      <span className="ml-3 inline-flex items-center bg-primary/10 text-primary text-xs px-2 py-0.5 rounded font-sans">
                        {annotation}
                      </span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

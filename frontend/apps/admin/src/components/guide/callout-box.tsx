interface CalloutBoxProps {
  variant: "tip" | "warning" | "important";
  title?: string;
  children: React.ReactNode;
}

const VARIANT_STYLES = {
  tip: {
    border: "border-l-4 border-emerald-500",
    bg: "bg-emerald-500/10",
    label: "TIP",
    labelColor: "text-emerald-400",
  },
  warning: {
    border: "border-l-4 border-amber-500",
    bg: "bg-amber-500/10",
    label: "WARNING",
    labelColor: "text-amber-400",
  },
  important: {
    border: "border-l-4 border-sky-500",
    bg: "bg-sky-500/10",
    label: "IMPORTANT",
    labelColor: "text-sky-400",
  },
};

export function CalloutBox({ variant, title, children }: CalloutBoxProps) {
  const styles = VARIANT_STYLES[variant];
  return (
    <div className={`${styles.border} ${styles.bg} px-4 py-3 rounded-r-lg my-4`}>
      <p className={`text-xs font-bold tracking-widest mb-1 ${styles.labelColor}`}>
        {title ?? styles.label}
      </p>
      <div className="text-sm text-foreground/80">{children}</div>
    </div>
  );
}

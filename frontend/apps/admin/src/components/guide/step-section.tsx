interface StepSectionProps {
  step: number;
  title: string;
  children: React.ReactNode;
}

export function StepSection({ step, title, children }: StepSectionProps) {
  return (
    <div className="mb-12">
      <div className="flex items-start gap-4 mb-4">
        <div className="flex-shrink-0 w-10 h-10 rounded-full bg-primary text-primary-foreground font-bold flex items-center justify-center text-sm">
          {step}
        </div>
        <h2 className="text-xl font-semibold leading-10">{title}</h2>
      </div>
      <div className="pl-14">{children}</div>
    </div>
  );
}

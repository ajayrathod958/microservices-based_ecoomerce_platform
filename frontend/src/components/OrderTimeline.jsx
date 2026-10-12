import { dateTime } from '../lib/format';
import { buildTimeline } from '../lib/timeline';

export default function OrderTimeline({ order }) {
  const steps = buildTimeline(order);
  return (
    <ol className="space-y-0">
      {steps.map((step, i) => {
        const dot = !step.done ? 'border-line bg-white' : step.danger ? 'border-rust bg-rust' : 'border-leaf bg-leaf';
        return (
          <li key={step.key} className="relative flex gap-4 pb-8 last:pb-0">
            {i < steps.length - 1 && (
              <span className={`absolute left-[9px] top-5 h-full w-0.5 ${steps[i + 1].done ? 'bg-leaf' : 'bg-line'}`} aria-hidden="true" />
            )}
            <span className={`relative z-10 mt-0.5 h-5 w-5 shrink-0 rounded-full border-2 ${dot}`} aria-hidden="true" />
            <div>
              <p className={`font-semibold ${step.done ? '' : 'text-moss'}`}>
                {step.label}
                <span className="sr-only">{step.done ? ' (done)' : ' (waiting)'}</span>
              </p>
              {step.done && step.at && <p className="text-sm text-moss">{dateTime(step.at)}</p>}
              {step.done && step.note && step.key !== 'PENDING' && <p className="mt-0.5 text-sm">{step.note}</p>}
            </div>
          </li>
        );
      })}
    </ol>
  );
}

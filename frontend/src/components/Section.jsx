import { useRef } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, ChevronLeft, ChevronRight } from './icons';

export function SectionHeader({ title, to, linkLabel = 'View all' }) {
  return (
    <div className="mb-5 flex items-end justify-between gap-4">
      <h2 className="text-2xl">{title}</h2>
      {to && (
        <Link to={to} className="flex items-center gap-1 text-sm font-medium text-ink/70 hover:text-brand">
          {linkLabel} <ArrowRight className="h-4 w-4" />
        </Link>
      )}
    </div>
  );
}

/** Horizontal strip with snap scrolling and prev/next buttons (hidden on touch-sized screens where swiping works). */
export function Scroller({ children, label }) {
  const ref = useRef(null);
  const by = (dir) => ref.current?.scrollBy({ left: dir * ref.current.clientWidth * 0.8, behavior: 'smooth' });
  return (
    <div className="relative">
      <ul ref={ref} aria-label={label} className="flex snap-x gap-5 overflow-x-auto pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {children}
      </ul>
      <button type="button" aria-label="Scroll left" onClick={() => by(-1)} className="absolute -left-4 top-1/3 hidden rounded-full border border-line bg-white p-2 shadow-card hover:bg-chalk-deep lg:block">
        <ChevronLeft className="h-4 w-4" />
      </button>
      <button type="button" aria-label="Scroll right" onClick={() => by(1)} className="absolute -right-4 top-1/3 hidden rounded-full border border-line bg-white p-2 shadow-card hover:bg-chalk-deep lg:block">
        <ChevronRight className="h-4 w-4" />
      </button>
    </div>
  );
}

const ITEMS = [
  'Live stock levels on every product',
  'Track every order from checkout to confirmation',
  'Demo store: pick a test card at checkout',
];

export default function AnnouncementBar() {
  return (
    <div className="bg-ink text-white">
      <ul className="mx-auto flex max-w-7xl items-center justify-center gap-10 px-4 py-2 text-xs font-medium">
        {ITEMS.map((t, i) => (
          <li key={t} className={i === 0 ? '' : 'hidden md:list-item'}>
            {t}
          </li>
        ))}
      </ul>
    </div>
  );
}

import { EmptyState } from '../components/feedback';

export default function NotFound() {
  return <EmptyState title="Page not found" text="The page you're looking for doesn't exist or has moved." actionLabel="Back to the shop" actionTo="/" />;
}

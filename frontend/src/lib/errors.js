/** Turns an axios error into a sentence a shopper can act on. */
export function getErrorMessage(err, fallback = 'Something went wrong. Please try again.') {
  const status = err?.response?.status;
  if (status === 429) return 'Too many requests. Wait a moment and try again.';
  const message = err?.response?.data?.message;
  if (message) return message;
  if (err?.code === 'ECONNABORTED') return 'The request timed out. Please try again.';
  if (err?.request && !err?.response) return 'Cannot reach the server. Check your connection and try again.';
  return fallback;
}

/** Spring validation errors -> { fieldName: message }. Nested names like "shippingAddress.city" become "city". */
export function getFieldErrors(err) {
  const list = err?.response?.data?.errors;
  if (!Array.isArray(list)) return {};
  return Object.fromEntries(list.map((e) => [String(e.field).split('.').pop(), e.message]));
}

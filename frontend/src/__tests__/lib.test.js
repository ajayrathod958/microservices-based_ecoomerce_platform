import { describe, expect, it } from 'vitest';
import { getErrorMessage, getFieldErrors } from '../lib/errors';
import { money, shortId } from '../lib/format';
import { buildTimeline, isTerminal } from '../lib/timeline';

describe('errors', () => {
  it('prefers the server message', () => {
    expect(getErrorMessage({ response: { status: 409, data: { message: 'Out of stock' } } })).toBe('Out of stock');
  });
  it('explains rate limiting and network failures', () => {
    expect(getErrorMessage({ response: { status: 429, data: '' } })).toMatch(/too many/i);
    expect(getErrorMessage({ request: {} })).toMatch(/cannot reach/i);
  });
  it('maps nested validation fields to short names', () => {
    expect(getFieldErrors({ response: { data: { errors: [{ field: 'shippingAddress.city', message: 'required' }] } } })).toEqual({ city: 'required' });
    expect(getFieldErrors({})).toEqual({});
  });
});

describe('format', () => {
  it('formats money and ids', () => {
    expect(money(12.5, 'USD')).toContain('12.50');
    expect(money(null)).toBe('-');
    expect(shortId('abcdef12-3456')).toBe('#ABCDEF12');
  });
});

describe('timeline', () => {
  it('marks earlier steps done when a later event arrived first', () => {
    const steps = buildTimeline({ status: 'CONFIRMED', history: [{ status: 'PENDING', at: 'a' }, { status: 'CONFIRMED', at: 'b' }] });
    expect(steps.map((s) => s.done)).toEqual([true, true, true]);
  });
  it('shows a cancelled final step', () => {
    const steps = buildTimeline({ status: 'CANCELLED', history: [{ status: 'PENDING' }, { status: 'CANCELLED', note: 'Payment declined' }] });
    expect(steps[2]).toMatchObject({ key: 'CANCELLED', done: true, danger: true, note: 'Payment declined' });
  });
  it('knows terminal states', () => {
    expect(isTerminal('CONFIRMED')).toBe(true);
    expect(isTerminal('PENDING')).toBe(false);
  });
});

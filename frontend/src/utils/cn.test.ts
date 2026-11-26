import { describe, it, expect } from 'vitest';
import { cn, formatNumber, formatDateTime } from './cn';

describe('cn', () => {
  it('merges class names', () => {
    expect(cn('btn', 'btn-primary')).toBe('btn btn-primary');
  });

  it('omits falsy values', () => {
    expect(cn('btn', false && 'hidden', undefined, null)).toBe('btn');
  });

  it('lets later tailwind classes win', () => {
    expect(cn('p-2', 'p-4')).toBe('p-4');
  });
});

describe('formatNumber', () => {
  it('formats thousands', () => {
    expect(formatNumber(1500)).toBe('1.5K');
  });

  it('formats millions', () => {
    expect(formatNumber(2_500_000)).toBe('2.5M');
  });

  it('leaves small numbers as-is', () => {
    expect(formatNumber(42)).toBe('42');
  });
});

describe('formatDateTime', () => {
  it('formats ISO timestamps', () => {
    expect(formatDateTime('2024-01-15T10:30:00Z')).toContain('2024');
  });

  it('returns the input when unparsable', () => {
    expect(formatDateTime('not-a-date')).toBe('not-a-date');
  });
});

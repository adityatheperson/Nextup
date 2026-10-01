import test from 'node:test';
import assert from 'node:assert/strict';
import { getDemoState } from '../website/demo.mjs';

test('upcoming shows walking only for the first class', () => {
  const state = getDemoState('upcoming');
  assert.equal(state.events[0].title, 'Calculus lecture');
  assert.deepEqual(state.events.map(event => event.walk), [12, null, null]);
  assert.equal(state.progress, null);
});
test('ongoing shows event progress and walking to the next class', () => {
  const state = getDemoState('now');
  assert.equal(state.label, 'Now');
  assert.equal(state.progress, 50);
  assert.deepEqual(state.events.map(event => event.walk), [null, 8, null]);
});
test('finished removes the ended class and promotes the next', () => {
  const state = getDemoState('finished');
  assert.equal(state.events[0].title, 'Physics lab');
  assert.equal(state.events.some(event => event.title === 'Calculus lecture'), false);
  assert.deepEqual(state.events.map(event => event.walk), [8, null]);
  assert.equal(state.progress, null);
});
test('unknown state safely returns upcoming and results are independent', () => {
  assert.deepEqual(getDemoState('unknown'), getDemoState('upcoming'));
  const state = getDemoState('now');
  state.events[0].title = 'Changed';
  assert.equal(getDemoState('now').events[0].title, 'Calculus lecture');
});

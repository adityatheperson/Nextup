import { getDemoState } from './demo.mjs';

const controls = document.querySelector('.demo-buttons');
const content = document.querySelector('#notification-content');
const buttons = [...document.querySelectorAll('[data-state]')];

function walkingLine(event, checkedAt) {
  if (event.walk === null) return '';
  return `<p class="walk-line"><svg class="icon" aria-hidden="true"><use href="#icon-walk"/></svg><span>~${event.walk} min walk <span class="walk-time">· as of ${checkedAt}</span></span></p>`;
}

function render(mode) {
  const state = getDemoState(mode);
  const [first, ...later] = state.events;
  // All values below come from fixed, local sample data, never from user input.
  content.innerHTML = `<p class="event-status">${state.label}</p>
    <h2 class="featured-title">${first.title}</h2>
    <p class="featured-time">Today · ${first.time}</p>
    ${walkingLine(first, state.checkedAt)}
    ${state.progress === null ? '' : `<div class="progress-area"><div class="progress-track" role="progressbar" aria-label="Illustrative event progress" aria-valuemin="0" aria-valuemax="100" aria-valuenow="50"><div class="progress-fill"></div></div><div class="progress-labels"><span>Started 2:00 PM</span><span>Ends 3:00 PM</span></div></div>`}
    <div class="later-events">${later.map(event => `<div class="later-event"><span class="row-time">${event.time.split('–')[0]} PM</span><div><h3>${event.title}</h3>${walkingLine(event, state.checkedAt)}</div></div>`).join('')}</div>
    <p class="routing-credit">Walks: openrouteservice · © OpenStreetMap</p>`;
  document.querySelector('.sample-time').textContent = state.clock;
  document.querySelector('#demo-explanation').textContent = state.explanation;
  buttons.forEach(button => button.setAttribute('aria-pressed', String(button.dataset.state === state.mode)));
  content.classList.remove('notification-content-enter');
  requestAnimationFrame(() => content.classList.add('notification-content-enter'));
}

buttons.forEach((button, index) => {
  button.addEventListener('click', () => render(button.dataset.state));
  button.addEventListener('keydown', event => {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
    event.preventDefault();
    const next = buttons[(index + (event.key === 'ArrowRight' ? 1 : -1) + buttons.length) % buttons.length];
    next.focus();
    render(next.dataset.state);
  });
});
controls.hidden = false;

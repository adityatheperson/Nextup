export function getDemoState(requested) {
  const mode = ['upcoming', 'now', 'finished'].includes(requested) ? requested : 'upcoming';
  const events = [
    { title: 'Calculus lecture', time: '2:00–3:00 PM', walk: mode === 'upcoming' ? 12 : null },
    { title: 'Physics lab', time: '3:30–4:30 PM', walk: mode === 'upcoming' ? null : 8 },
    { title: 'Study group', time: '5:00–6:00 PM', walk: null },
  ];
  return {
    mode,
    label: mode === 'now' ? 'Now' : 'Upcoming',
    clock: { upcoming: '1:45 PM', now: '2:30 PM', finished: '3:00 PM' }[mode],
    checkedAt: { upcoming: '1:40 PM', now: '2:25 PM', finished: '2:55 PM' }[mode],
    progress: mode === 'now' ? 50 : null,
    events: mode === 'finished' ? events.slice(1) : events,
    explanation: {
      upcoming: 'Before class, see what’s next and how long the walk takes.',
      now: 'During class, see its progress and the walk to your next class.',
      finished: 'Class over. It disappears, and your next event takes its place.',
    }[mode],
  };
}

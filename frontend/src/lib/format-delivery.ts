export function formatDate(value: string) {
  return new Intl.DateTimeFormat('de-DE').format(new Date(value))
}

export function formatTime(value: string) {
  return value.slice(0, 5)
}

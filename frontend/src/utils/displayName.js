export function toDisplayName(email) {
  if (!email) return ''
  const local = email.split('@')[0]
  return local
    .replace(/[._]/g, ' ')
    .split(' ')
    .filter(Boolean)
    .map(w => w[0].toUpperCase() + w.slice(1))
    .join(' ')
}

export const ROLE_LABELS = {
  SYSTEM_ADMIN: 'System Administrator',
  ENERGY_MANAGER: 'Energy Manager',
  BUSINESS_OWNER: 'Business Owner',
  TECHNICIAN: 'Technician',
  VIEWER: 'Viewer',
}
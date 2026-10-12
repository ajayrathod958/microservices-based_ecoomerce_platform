import { http, unwrap } from './http';

export const notificationsApi = {
  list: (page = 0, unreadOnly = false, size = 20) =>
    unwrap(http.get('/api/notifications', { params: { page, size, unreadOnly } })),
  unreadCount: () => unwrap(http.get('/api/notifications/unread-count')).then((d) => d.unread),
  markRead: (id) => http.put(`/api/notifications/${id}/read`),
  markAllRead: () => http.put('/api/notifications/read-all'),
};

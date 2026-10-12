CREATE TABLE notifications (
    id             UUID         PRIMARY KEY,
    user_id        UUID         NOT NULL,
    email          VARCHAR(255),
    type           VARCHAR(40)  NOT NULL,
    title          VARCHAR(150) NOT NULL,
    message        VARCHAR(500) NOT NULL,
    order_id       UUID,
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    email_status   VARCHAR(10)  NOT NULL,
    email_attempts INT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread  ON notifications (user_id) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_email_pending ON notifications (created_at) WHERE email_status = 'PENDING';

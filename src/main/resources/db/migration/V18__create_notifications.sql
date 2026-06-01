CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    titre VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    lue BOOLEAN DEFAULT FALSE,
    lien VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_notification_user ON notification(user_id);
CREATE INDEX idx_notification_lue ON notification(user_id, lue);

COMMENT ON TABLE notification IS 'Notifications in-app pour tous les utilisateurs';

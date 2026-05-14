CREATE TABLE password_reset_tokens (
                                       id SERIAL PRIMARY KEY,
                                       token VARCHAR(255) NOT NULL UNIQUE,
                                       user_id INT NOT NULL UNIQUE REFERENCES users_table(id) ON DELETE CASCADE,
                                       expiry_date TIMESTAMP WITH TIME ZONE NOT NULL
);
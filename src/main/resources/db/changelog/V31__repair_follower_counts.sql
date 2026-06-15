UPDATE users_table u
SET followers_count = (
    SELECT COUNT(*) FROM followers f
    WHERE f.following_id = u.id AND f.status = 'ACTIVE'
),
    following_count = (
        SELECT COUNT(*) FROM followers f
        WHERE f.follower_id = u.id AND f.status = 'ACTIVE'
    );
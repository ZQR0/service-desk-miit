ALTER TABLE department
    ADD COLUMN head_user_id bigint REFERENCES users(id);

CREATE UNIQUE INDEX uq_department_head_user ON department (head_user_id)
    WHERE head_user_id IS NOT NULL;

UPDATE department d
SET head_user_id = sub.id
FROM (
    SELECT DISTINCT ON (u.department_id) u.id, u.department_id
    FROM users u
    JOIN role r ON r.id = u.role_id
    WHERE r.name_role = 'DEPARTMENT_HEAD'
      AND u.department_id IS NOT NULL
    ORDER BY u.department_id, u.id
) sub
WHERE d.id = sub.department_id;

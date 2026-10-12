-- Starting goals for /hoje, editable later in the dashboard. They are starting
-- points chosen by the project, not health recommendations.
-- Units match how events are stored: water in ml, sleep and exercise in minutes.
INSERT INTO goal (type, target_value, unit, period, active) VALUES
    ('water', 2000, 'ml', 'day', true),
    ('sleep', 420, 'min', 'day', true),
    ('exercise', 150, 'min', 'week', true);

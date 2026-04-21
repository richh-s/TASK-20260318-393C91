-- Seed data for City Bus Platform

-- Seed users share a BCrypt hash for plaintext "password" (cost 10) — see README.
INSERT INTO users (username, password, role, display_name)
VALUES ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 'System Admin');

INSERT INTO users (username, password, role, display_name)
VALUES ('dispatcher1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'DISPATCHER', 'Dispatcher One');

INSERT INTO users (username, password, role, display_name)
VALUES ('passenger1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'PASSENGER', 'Test Passenger');

-- Bus Routes
INSERT INTO bus_routes (route_number, name, description) VALUES
('101', 'Central Loop', 'Main city central loop route'),
('202', 'Airport Express', 'Direct route to the airport'),
('303', 'University Line', 'Route connecting university campuses'),
('404', 'Shopping District', 'Route through major shopping areas'),
('505', 'Industrial Zone', 'Route serving industrial areas');

-- Bus Stops for Route 101
INSERT INTO bus_stops (route_id, sequence_number, name_en, name_cn, pinyin, pinyin_initials, address, popularity_score) VALUES
(1, 1, 'Central Station', '中央车站', 'zhongyangchezhan', 'zycs', '1 Central Ave', 100),
(1, 2, 'City Hall', '市政厅', 'shizhengting', 'szt', '10 Government St', 85),
(1, 3, 'Main Library', '主图书馆', 'zhutushuguan', 'ztsg', '5 Library Rd', 70),
(1, 4, 'Riverside Park', '河滨公园', 'hebingongyuan', 'hbgy', '20 Riverside Dr', 65),
(1, 5, 'North Terminal', '北终点站', 'beizhongdianzhan', 'bzds', '50 North Blvd', 80);

-- Bus Stops for Route 202
INSERT INTO bus_stops (route_id, sequence_number, name_en, name_cn, pinyin, pinyin_initials, address, popularity_score) VALUES
(2, 1, 'Airport Terminal 1', '机场1号航站楼', 'jichangyihaohangzhanlou', 'jcyhzl', 'Airport Rd', 95),
(2, 2, 'Airport Terminal 2', '机场2号航站楼', 'jichangerhaohangzhanlou', 'jcehzl', 'Airport Rd', 90),
(2, 3, 'Express Junction', '快线交汇处', 'kuaixianjiaohuichu', 'kxjhc', 'Junction Ave', 60),
(2, 4, 'Central Station', '中央车站', 'zhongyangchezhan', 'zycs', '1 Central Ave', 100);

-- Bus Stops for Route 303
INSERT INTO bus_stops (route_id, sequence_number, name_en, name_cn, pinyin, pinyin_initials, address, popularity_score) VALUES
(3, 1, 'East Campus Gate', '东校门', 'dongxiaomen', 'dxm', 'University East Rd', 75),
(3, 2, 'Science Building', '理科楼', 'likelou', 'lkl', '100 Campus Dr', 60),
(3, 3, 'Sports Center', '体育中心', 'tiyuzhongxin', 'tyzx', '200 Sports Ave', 55),
(3, 4, 'West Campus Gate', '西校门', 'xixiaomen', 'xxm', 'University West Rd', 70);

-- Sorting weights
INSERT INTO sorting_weights (factor_name, weight, description)
VALUES
('frequency_score', 0.6, 'Weight for search frequency'),
('popularity_score', 0.4, 'Weight for stop popularity');

-- System configs
INSERT INTO system_configs (config_key, config_value, description)
VALUES
('queue_backlog_threshold', '100', 'Message queue backlog alert threshold'),
('api_p95_threshold_ms', '500', 'API P95 response time alert threshold in milliseconds'),
('default_reminder_minutes', '10', 'Default arrival reminder minutes before scheduled time'),
('missed_checkin_minutes', '5', 'Minutes after scheduled time to trigger missed check-in notification'),
('task_timeout_hours', '24', 'Hours before a pending workflow task triggers escalation');

-- Notification templates
INSERT INTO notification_templates (name, type, title_template, content_template, sensitivity_level, created_by)
VALUES
('reservation_success', 'RESERVATION_SUCCESS', 'Reservation Confirmed', 'Your reservation for route {route} stop {stop} at {time} has been confirmed.', 0, 1),
('arrival_reminder', 'ARRIVAL_REMINDER', 'Bus Arriving Soon', 'Your bus {route} arrives at {stop} in {minutes} minutes.', 0, 1),
('missed_checkin', 'MISSED_CHECKIN', 'Missed Check-in', 'You missed your scheduled check-in for route {route} at {time}.', 0, 1),
('task_assigned', 'TASK_ASSIGNED', 'New Task Assigned', 'Task #{taskNumber} has been assigned to you: {title}', 0, 1),
('task_escalated', 'TASK_ESCALATED', 'Task Escalation Warning', 'Task #{taskNumber} has been unprocessed for 24 hours and requires attention.', 1, 1);

-- Field dictionaries
INSERT INTO field_dictionaries (field_name, raw_value, standard_value, created_by)
VALUES
('area', 'sqm', '㎡', 1),
('area', 'm2', '㎡', 1),
('area', '平方米', '㎡', 1),
('price', 'yuan/month', '元/月', 1),
('price', '元/月份', '元/月', 1),
('price', 'rmb/month', '元/月', 1);

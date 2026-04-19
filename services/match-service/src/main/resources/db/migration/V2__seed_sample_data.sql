-- ──────────────────────────────────────────────
-- V2: Sample seed data for development
-- ──────────────────────────────────────────────

-- Venues
INSERT INTO venue (id, name, short_name, city, country, capacity, pitch_type, timezone) VALUES
  ('a1000000-0000-0000-0000-000000000001', 'Melbourne Cricket Ground', 'MCG', 'Melbourne', 'Australia', 100024, 'Pace-friendly', 'Australia/Melbourne'),
  ('a1000000-0000-0000-0000-000000000002', 'Eden Gardens', 'EG', 'Kolkata', 'India', 66349, 'Spin-friendly', 'Asia/Kolkata'),
  ('a1000000-0000-0000-0000-000000000003', 'Lord''s Cricket Ground', 'Lords', 'London', 'United Kingdom', 30000, 'Seam-friendly', 'Europe/London');

-- Teams
INSERT INTO team (id, name, short_name, country_code, team_type) VALUES
  ('b1000000-0000-0000-0000-000000000001', 'India', 'IND', 'IND', 'NATIONAL'),
  ('b1000000-0000-0000-0000-000000000002', 'Australia', 'AUS', 'AUS', 'NATIONAL'),
  ('b1000000-0000-0000-0000-000000000003', 'England', 'ENG', 'GBR', 'NATIONAL');

-- Series
INSERT INTO series (id, name, short_name, format, season, start_date, end_date, host_country, status) VALUES
  ('c1000000-0000-0000-0000-000000000001', 'ICC Test Championship 2024-25', 'WTC 2024', 'TEST', '2024-25', '2024-06-01', '2025-06-30', 'Global', 'ONGOING');

-- Match
INSERT INTO match (id, series_id, match_number, format, venue_id, scheduled_start, status, home_team_id, away_team_id) VALUES
  ('d1000000-0000-0000-0000-000000000001',
   'c1000000-0000-0000-0000-000000000001',
   1, 'TEST',
   'a1000000-0000-0000-0000-000000000001',
   '2024-12-26 01:00:00+00',
   'UPCOMING',
   'b1000000-0000-0000-0000-000000000002',
   'b1000000-0000-0000-0000-000000000001');

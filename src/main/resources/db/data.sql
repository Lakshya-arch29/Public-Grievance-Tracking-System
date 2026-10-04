-- Lookup data. Safe to run repeatedly: existing rows are skipped.
-- Demo users are created by DemoDataSeeder (so BCrypt hashes always match).

INSERT INTO city (name, state) VALUES
    ('Mumbai',    'Maharashtra'),
    ('Pune',      'Maharashtra'),
    ('Nagpur',    'Maharashtra'),
    ('Thane',     'Maharashtra'),
    ('Nashik',    'Maharashtra'),
    ('Delhi',     'Delhi'),
    ('Bengaluru', 'Karnataka'),
    ('Hyderabad', 'Telangana')
ON CONFLICT (name) DO NOTHING;

INSERT INTO category (name) VALUES
    ('Roads & Potholes'),
    ('Garbage & Cleanliness'),
    ('Water Supply'),
    ('Electricity & Street Lights'),
    ('Drainage & Sewage'),
    ('Pollution'),
    ('Public Transport'),
    ('Parks & Public Spaces'),
    ('Stray Animals'),
    ('Health & Hospitals'),
    ('Illegal Construction & Encroachment'),
    ('Other')
ON CONFLICT (name) DO NOTHING;

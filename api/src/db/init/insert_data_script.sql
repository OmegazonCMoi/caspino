BEGIN;

-- =========================
-- USERS
-- =========================
INSERT INTO users (id, username, email, password_hash)
VALUES
('00000000-0000-0000-0000-000000000001', 'alice', 'alice@mail.com', 'hash'),
('00000000-0000-0000-0000-000000000002', 'bob', 'bob@mail.com', 'hash'),
('00000000-0000-0000-0000-000000000003', 'charlie', 'charlie@mail.com', 'hash'),
('00000000-0000-0000-0000-000000000004', 'david', 'david@mail.com', 'hash'),
('00000000-0000-0000-0000-000000000005', 'eva', 'eva@mail.com', 'hash');

-- =========================
-- Crédit initial
-- =========================
INSERT INTO wallet_transactions (user_id, amount, reason) VALUES
('00000000-0000-0000-0000-000000000001', 200, 'admin_adjustment'),
('00000000-0000-0000-0000-000000000002', 150, 'admin_adjustment'),
('00000000-0000-0000-0000-000000000003', 300, 'admin_adjustment'),
('00000000-0000-0000-0000-000000000004', 100, 'admin_adjustment'),
('00000000-0000-0000-0000-000000000005', 250, 'admin_adjustment');

-- =========================
-- Bonus journalier
-- =========================
INSERT INTO wallet_transactions (user_id, amount, reason) VALUES
('00000000-0000-0000-0000-000000000001', 10, 'daily_bonus'),
('00000000-0000-0000-0000-000000000002', 10, 'daily_bonus'),
('00000000-0000-0000-0000-000000000003', 10, 'daily_bonus');

-- =========================
-- PARTIES
-- =========================
INSERT INTO parties (id, user_id, game_type) VALUES
('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'roulette'),
('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 'blackjack'),
('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003', 'slot'),
('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000004', 'roulette'),
('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000005', 'blackjack');

-- =========================
-- BETS
-- =========================

-- Alice roulette 20€ rouge
INSERT INTO bets (party_id, user_id, amount, kind, selection)
VALUES (
'10000000-0000-0000-0000-000000000001',
'00000000-0000-0000-0000-000000000001',
20,
'roulette_color',
'{"color":"red"}'
);

-- Bob blackjack 30€
INSERT INTO bets (party_id, user_id, amount, kind, selection)
VALUES (
'10000000-0000-0000-0000-000000000002',
'00000000-0000-0000-0000-000000000002',
30,
'blackjack_win',
'{}'
);

-- Charlie slot 15€
INSERT INTO bets (party_id, user_id, amount, kind, selection)
VALUES (
'10000000-0000-0000-0000-000000000003',
'00000000-0000-0000-0000-000000000003',
15,
'slot_spin',
'{}'
);

-- David roulette 10€ numéro 7
INSERT INTO bets (party_id, user_id, amount, kind, selection)
VALUES (
'10000000-0000-0000-0000-000000000004',
'00000000-0000-0000-0000-000000000004',
10,
'roulette_number',
'{"number":"7"}'
);

-- Eva blackjack 50€
INSERT INTO bets (party_id, user_id, amount, kind, selection)
VALUES (
'10000000-0000-0000-0000-000000000005',
'00000000-0000-0000-0000-000000000005',
50,
'blackjack_win',
'{}'
);

-- =========================
-- RESULTS
-- =========================

-- Alice gagne 40€
INSERT INTO roulette_results (party_id, number, color, gain)
VALUES ('10000000-0000-0000-0000-000000000001', 7, 'red', 40);

-- Bob perd
INSERT INTO blackjack_results (party_id, won, gain)
VALUES ('10000000-0000-0000-0000-000000000002', false, 0);

-- Charlie gagne 60€
INSERT INTO slot_results (party_id, result, gain)
VALUES ('10000000-0000-0000-0000-000000000003', '777', 60);

-- David perd
INSERT INTO roulette_results (party_id, number, color, gain)
VALUES ('10000000-0000-0000-0000-000000000004', 12, 'black', 0);

-- Eva gagne 100€
INSERT INTO blackjack_results (party_id, won, gain)
VALUES ('10000000-0000-0000-0000-000000000005', true, 100);

COMMIT;
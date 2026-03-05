BEGIN;

-- UUID generator
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- ENUMS
-- =========================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'game_type') THEN
    CREATE TYPE game_type AS ENUM ('slot', 'roulette', 'blackjack');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'bet_kind') THEN
    CREATE TYPE bet_kind AS ENUM (
      'roulette_color','roulette_number','roulette_parity',
      'roulette_dozen','roulette_column','roulette_low_high',
      'blackjack_win','slot_spin'
    );
  END IF;
END$$;

-- =========================
-- USERS
-- =========================
CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_login TIMESTAMPTZ,
  balance NUMERIC(12,2) NOT NULL DEFAULT 0,
  CONSTRAINT users_balance_nonnegative CHECK (balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- =========================
-- PARTIES
-- =========================
CREATE TABLE IF NOT EXISTS parties (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  game_type game_type NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at TIMESTAMPTZ,
  CONSTRAINT parties_time_order CHECK (finished_at IS NULL OR finished_at >= created_at),
  UNIQUE(id)
);

CREATE INDEX IF NOT EXISTS idx_parties_created_at ON parties(created_at);
CREATE INDEX IF NOT EXISTS idx_parties_game_type ON parties(game_type);

-- =========================
-- BETS
-- =========================
CREATE TABLE IF NOT EXISTS bets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  party_id UUID NOT NULL,
  user_id UUID NOT NULL,
  amount NUMERIC(12,2) NOT NULL,
  kind bet_kind NOT NULL,
  selection JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT bets_amount_positive CHECK (amount > 0),
  CONSTRAINT bets_selection_object CHECK (jsonb_typeof(selection) = 'object'),

  CONSTRAINT bets_party_fk FOREIGN KEY (party_id) REFERENCES parties(id) ON DELETE CASCADE,
  CONSTRAINT bets_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_bets_party_id ON bets(party_id);
CREATE INDEX IF NOT EXISTS idx_bets_user_id_created_at ON bets(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_bets_kind ON bets(kind);

-- =========================
-- Roulette payload checks
-- =========================
ALTER TABLE bets ADD CONSTRAINT roulette_color_payload CHECK (
  kind <> 'roulette_color'
  OR (selection ? 'color' AND (selection->>'color') IN ('red','black'))
);

ALTER TABLE bets ADD CONSTRAINT roulette_number_payload CHECK (
  kind <> 'roulette_number'
  OR (
    selection ? 'number'
    AND (selection->>'number') ~ '^[0-9]+$'
    AND ((selection->>'number')::int BETWEEN 0 AND 36)
  )
);

ALTER TABLE bets ADD CONSTRAINT roulette_parity_payload CHECK (
  kind <> 'roulette_parity'
  OR (selection ? 'parity' AND (selection->>'parity') IN ('even','odd'))
);

ALTER TABLE bets ADD CONSTRAINT roulette_dozen_payload CHECK (
  kind <> 'roulette_dozen'
  OR (
    selection ? 'dozen'
    AND ((selection->>'dozen')::int BETWEEN 1 AND 3)
  )
);

ALTER TABLE bets ADD CONSTRAINT roulette_column_payload CHECK (
  kind <> 'roulette_column'
  OR (
    selection ? 'column'
    AND ((selection->>'column')::int BETWEEN 1 AND 3)
  )
);

ALTER TABLE bets ADD CONSTRAINT roulette_low_high_payload CHECK (
  kind <> 'roulette_low_high'
  OR (selection ? 'range' AND (selection->>'range') IN ('low','high'))
);

-- =========================
-- RESULTS
-- =========================
CREATE TABLE IF NOT EXISTS slot_results (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  party_id UUID NOT NULL REFERENCES parties(id) ON DELETE CASCADE,
  result TEXT NOT NULL,
  gain NUMERIC(12,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT slot_result_format CHECK (result ~ '^\[("[0-9]",?)+\]$'),
  CONSTRAINT slot_gain_nonnegative CHECK (gain >= 0)
);

CREATE TABLE IF NOT EXISTS roulette_results (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  party_id UUID NOT NULL REFERENCES parties(id) ON DELETE CASCADE,
  number SMALLINT NOT NULL,
  color TEXT NOT NULL,
  gain NUMERIC(12,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT roulette_number_range CHECK (number BETWEEN 0 AND 36),
  CONSTRAINT roulette_color_valid CHECK (color IN ('red','black','green')),
  CONSTRAINT roulette_gain_nonnegative CHECK (gain >= 0)
);

CREATE TABLE IF NOT EXISTS blackjack_results (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  party_id UUID NOT NULL REFERENCES parties(id) ON DELETE CASCADE,
  won BOOLEAN NOT NULL,
  gain NUMERIC(12,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT blackjack_gain_nonnegative CHECK (gain >= 0)
);

-- =========================
-- WALLET TRANSACTIONS
-- =========================
CREATE TABLE IF NOT EXISTS wallet_transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  amount NUMERIC(12,2) NOT NULL,
  reason TEXT NOT NULL,
  reference_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_wallet_user_created_at
ON wallet_transactions(user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_wallet_reason
ON wallet_transactions(reason);

-- =========================
-- WALLET BALANCE TRIGGER
-- =========================
CREATE OR REPLACE FUNCTION apply_wallet_transaction()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE users
  SET balance = balance + NEW.amount
  WHERE id = NEW.user_id;

  IF (SELECT balance FROM users WHERE id = NEW.user_id) < 0 THEN
    RAISE EXCEPTION 'Insufficient funds for user %', NEW.user_id;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_apply_wallet_transaction ON wallet_transactions;

CREATE TRIGGER trg_apply_wallet_transaction
AFTER INSERT ON wallet_transactions
FOR EACH ROW
EXECUTE FUNCTION apply_wallet_transaction();

-- =========================
-- BET → DEBIT
-- =========================
CREATE OR REPLACE FUNCTION create_wallet_debit_for_bet()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  INSERT INTO wallet_transactions (user_id, amount, reason, reference_id)
  VALUES (NEW.user_id, -NEW.amount, 'bet_debit', NEW.id);

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_bet_wallet_debit ON bets;

CREATE TRIGGER trg_bet_wallet_debit
AFTER INSERT ON bets
FOR EACH ROW
EXECUTE FUNCTION create_wallet_debit_for_bet();

-- =========================
-- RESULT → CREDIT
-- =========================
CREATE OR REPLACE FUNCTION create_wallet_credit_for_result()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE u_id UUID;
BEGIN
  SELECT id INTO u_id FROM users WHERE id = NEW.user_id;

  IF NEW.gain > 0 THEN
    INSERT INTO wallet_transactions (user_id, amount, reason, reference_id)
    VALUES (u_id, NEW.gain, 'game_win', NEW.party_id);
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_slot_wallet_credit ON slot_results;
CREATE TRIGGER trg_slot_wallet_credit
AFTER INSERT ON slot_results
FOR EACH ROW
EXECUTE FUNCTION create_wallet_credit_for_result();

DROP TRIGGER IF EXISTS trg_roulette_wallet_credit ON roulette_results;
CREATE TRIGGER trg_roulette_wallet_credit
AFTER INSERT ON roulette_results
FOR EACH ROW
EXECUTE FUNCTION create_wallet_credit_for_result();

DROP TRIGGER IF EXISTS trg_blackjack_wallet_credit ON blackjack_results;
CREATE TRIGGER trg_blackjack_wallet_credit
AFTER INSERT ON blackjack_results
FOR EACH ROW
EXECUTE FUNCTION create_wallet_credit_for_result();

COMMIT;
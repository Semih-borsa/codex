BEGIN;
CREATE TABLE currencies (
  code CHAR(3) PRIMARY KEY CHECK (code ~ '^[A-Z]{3}$'),
  minor_digits SMALLINT NOT NULL CHECK (minor_digits BETWEEN 0 AND 6)
);
INSERT INTO currencies(code,minor_digits) VALUES ('TRY',2),('USD',2),('EUR',2),('JPY',0);
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(254) NOT NULL UNIQUE CHECK (email = lower(btrim(email))),
  password_hash TEXT NOT NULL,
  payday SMALLINT NOT NULL DEFAULT 1 CHECK (payday BETWEEN 1 AND 31),
  timezone TEXT NOT NULL DEFAULT 'Europe/Istanbul',
  default_currency CHAR(3) NOT NULL DEFAULT 'TRY' REFERENCES currencies(code) CHECK (default_currency ~ '^[A-Z]{3}$'),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE accounts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  name VARCHAR(60) NOT NULL CHECK (length(btrim(name)) > 0),
  currency CHAR(3) NOT NULL DEFAULT 'TRY' REFERENCES currencies(code) CHECK (currency ~ '^[A-Z]{3}$'),
  opening_minor BIGINT NOT NULL DEFAULT 0,
  archived_at TIMESTAMPTZ,
  UNIQUE (id, user_id, currency)
);
CREATE TABLE categories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  name VARCHAR(60) NOT NULL CHECK (length(btrim(name)) > 0),
  kind VARCHAR(7) NOT NULL CHECK (kind IN ('income', 'expense')),
  archived_at TIMESTAMPTZ,
  UNIQUE (id, user_id, kind),
  UNIQUE (user_id, kind, name)
);
CREATE TABLE transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  account_id UUID NOT NULL,
  category_id UUID NOT NULL,
  kind VARCHAR(7) NOT NULL CHECK (kind IN ('income', 'expense')),
  amount_minor BIGINT NOT NULL CHECK (amount_minor BETWEEN 1 AND 100000000000),
  currency CHAR(3) NOT NULL DEFAULT 'TRY',
  occurred_on DATE NOT NULL CHECK (occurred_on BETWEEN DATE '2000-01-01' AND DATE '2100-12-31'),
  note VARCHAR(1000) NOT NULL DEFAULT '',
  idempotency_key UUID NOT NULL,
  request_hash CHAR(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
  version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
  deleted_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (account_id, user_id, currency) REFERENCES accounts(id, user_id, currency) ON DELETE RESTRICT,
  FOREIGN KEY (category_id, user_id, kind) REFERENCES categories(id, user_id, kind) ON DELETE RESTRICT,
  UNIQUE (user_id, idempotency_key)
);
CREATE INDEX transactions_period ON transactions (user_id, occurred_on DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX transactions_category_period ON transactions (user_id, category_id, occurred_on) WHERE deleted_at IS NULL;
CREATE TABLE budgets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  category_id UUID NOT NULL,
  kind VARCHAR(7) NOT NULL DEFAULT 'expense' CHECK (kind = 'expense'),
  currency CHAR(3) NOT NULL DEFAULT 'TRY' REFERENCES currencies(code) CHECK (currency ~ '^[A-Z]{3}$'),
  period_start DATE NOT NULL,
  period_end DATE NOT NULL CHECK (period_end >= period_start),
  limit_minor BIGINT NOT NULL CHECK (limit_minor BETWEEN 1 AND 100000000000),
  version BIGINT NOT NULL DEFAULT 1,
  FOREIGN KEY (category_id, user_id, kind) REFERENCES categories(id, user_id, kind) ON DELETE RESTRICT,
  UNIQUE (user_id, category_id, currency, period_start)
);
-- Transfers are a separate ledger operation: never counted as income/expense.
CREATE TABLE transfers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  source_account_id UUID NOT NULL,
  target_account_id UUID NOT NULL CHECK (target_account_id <> source_account_id),
  currency CHAR(3) NOT NULL,
  amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
  occurred_on DATE NOT NULL,
  FOREIGN KEY (source_account_id, user_id, currency) REFERENCES accounts(id, user_id, currency),
  FOREIGN KEY (target_account_id, user_id, currency) REFERENCES accounts(id, user_id, currency)
);
CREATE TABLE credit_cards (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  account_id UUID NOT NULL,
  currency CHAR(3) NOT NULL,
  closing_day SMALLINT NOT NULL CHECK (closing_day BETWEEN 1 AND 31),
  due_day SMALLINT NOT NULL CHECK (due_day BETWEEN 1 AND 31),
  archived_at TIMESTAMPTZ,
  FOREIGN KEY (account_id,user_id,currency) REFERENCES accounts(id,user_id,currency),
  UNIQUE (account_id,user_id),
  UNIQUE (id,user_id,currency)
);
CREATE TABLE card_snapshots (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  card_id UUID NOT NULL,
  currency CHAR(3) NOT NULL,
  statement_minor BIGINT NOT NULL CHECK (statement_minor BETWEEN 0 AND 100000000000),
  current_minor BIGINT NOT NULL CHECK (current_minor BETWEEN 0 AND 100000000000),
  pending_minor BIGINT NOT NULL CHECK (pending_minor BETWEEN 0 AND 100000000000),
  as_of_date DATE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (card_id,user_id,currency) REFERENCES credit_cards(id,user_id,currency)
);
CREATE INDEX card_snapshots_latest ON card_snapshots(user_id,card_id,created_at DESC,id DESC);
CREATE TABLE auth_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  family_id UUID NOT NULL,
  refresh_hash CHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  consumed_at TIMESTAMPTZ,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMIT;

create table slot_games (
  id bigint auto_increment primary key,
  game_code varchar(64) not null,
  name varchar(255) not null,
  status varchar(32) not null,
  reel_count int not null,
  row_count int not null,
  min_bet decimal(20,4) not null,
  max_bet decimal(20,4) not null,
  currency varchar(8) not null,
  sort_order int not null,
  legal_approval_id varchar(128),
  updated_at timestamp not null,
  constraint uk_slot_games_code unique (game_code)
);

create table slot_rounds (
  id bigint auto_increment primary key,
  round_id varchar(64) not null,
  user_id bigint not null,
  game_code varchar(64) not null,
  currency varchar(8) not null,
  bet_amount decimal(20,4) not null,
  payout_amount decimal(20,4) not null,
  multiplier decimal(10,4) not null,
  reel_result_json json not null,
  status varchar(32) not null,
  idempotency_key varchar(255) not null,
  debit_ledger_id bigint,
  credit_ledger_id bigint,
  created_at timestamp not null,
  constraint uk_slot_round_id unique (round_id),
  constraint uk_slot_round_idempotency unique (idempotency_key)
);
create index idx_slot_round_user_time on slot_rounds(user_id, created_at);
create index idx_slot_round_game_time on slot_rounds(game_code, created_at);

insert into slot_games(game_code, name, status, reel_count, row_count, min_bet, max_bet, currency, sort_order, legal_approval_id, updated_at)
values ('lucky_slots', 'Lucky Slots', 'active', 5, 3, 1.0000, 100.0000, 'GC', 10, 'LEGAL-SLOTS-001', current_timestamp);

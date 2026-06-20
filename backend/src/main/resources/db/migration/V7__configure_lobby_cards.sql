create table lobby_cards (
  id bigint auto_increment primary key,
  card_code varchar(64) not null,
  title varchar(255) not null,
  subtitle varchar(512) not null,
  image_url varchar(512) not null,
  target_url varchar(512) not null,
  status varchar(32) not null,
  sort_order int not null,
  updated_at timestamp not null,
  constraint uk_lobby_card_code unique (card_code)
);

insert into lobby_cards(card_code, title, subtitle, image_url, target_url, status, sort_order, updated_at)
values
('slots_main', 'Lucky Slots', 'GC play with SC rewards eligible by region', '/assets/lobby/slots-main.png', '/app/activity', 'active', 10, timestamp '2026-06-20 08:00:00'),
('jackpot_events', 'Live Events', 'Configured bonus events and jackpot tasks', '/assets/lobby/jackpot-events.png', '/app/activity', 'active', 20, timestamp '2026-06-20 08:00:00'),
('table_games_locked', 'Table Games', 'Coming after compliance approval', '/assets/lobby/table-games.png', '/app', 'paused', 30, timestamp '2026-06-20 08:00:00');

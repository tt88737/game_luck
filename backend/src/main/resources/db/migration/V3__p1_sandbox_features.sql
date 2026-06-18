create table product_packages (
  id bigint auto_increment primary key,
  package_code varchar(64) not null,
  name varchar(255) not null,
  price_amount decimal(20,4) not null,
  price_currency varchar(8) not null,
  gc_amount decimal(20,4) not null,
  status varchar(32) not null,
  sandbox_only boolean not null,
  created_at timestamp not null,
  constraint uk_product_package_code unique (package_code)
);

create table purchase_orders (
  id bigint auto_increment primary key,
  order_id varchar(64) not null,
  user_id bigint not null,
  package_code varchar(64) not null,
  price_amount decimal(20,4) not null,
  price_currency varchar(8) not null,
  gc_amount decimal(20,4) not null,
  status varchar(32) not null,
  provider varchar(64) not null,
  idempotency_key varchar(255) not null,
  ledger_id bigint,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint uk_purchase_order_id unique (order_id),
  constraint uk_purchase_idempotency unique (idempotency_key)
);
create index idx_purchase_user_time on purchase_orders(user_id, created_at);

create table kyc_applications (
  id bigint auto_increment primary key,
  user_id bigint not null,
  legal_name varchar(255) not null,
  birth_date date not null,
  address_line varchar(512) not null,
  state_code varchar(8) not null,
  status varchar(32) not null,
  review_reason varchar(512),
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint uk_kyc_user unique (user_id)
);

create table redemption_requests (
  id bigint auto_increment primary key,
  redemption_id varchar(64) not null,
  user_id bigint not null,
  sc_amount decimal(20,4) not null,
  method varchar(64) not null,
  status varchar(32) not null,
  sandbox_only boolean not null,
  idempotency_key varchar(255) not null,
  freeze_ledger_id bigint,
  review_reason varchar(512),
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint uk_redemption_id unique (redemption_id),
  constraint uk_redemption_idempotency unique (idempotency_key)
);
create index idx_redemption_user_time on redemption_requests(user_id, created_at);

insert into product_packages(package_code, name, price_amount, price_currency, gc_amount, status, sandbox_only, created_at)
values
  ('gc_499', 'GC 5,000 Sandbox Pack', 4.9900, 'USD', 5000.0000, 'active', true, current_timestamp),
  ('gc_999', 'GC 12,000 Sandbox Pack', 9.9900, 'USD', 12000.0000, 'active', true, current_timestamp);

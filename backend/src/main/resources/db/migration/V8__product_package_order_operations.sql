alter table product_packages add column provider varchar(64) not null default 'manual';
alter table product_packages add column sort_order int not null default 100;
alter table product_packages add column legal_approval_id varchar(128);

alter table purchase_orders add column provider_reference varchar(128);

update product_packages
set provider = 'manual',
    sort_order = case package_code when 'gc_499' then 10 when 'gc_999' then 20 else 100 end,
    legal_approval_id = 'LEGAL-PACKAGE-GC'
where provider = 'manual';

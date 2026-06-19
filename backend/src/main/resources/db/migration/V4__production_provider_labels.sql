update product_packages
set name = 'GC 5,000 Pack',
    sandbox_only = false
where package_code = 'gc_499';

update product_packages
set name = 'GC 12,000 Pack',
    sandbox_only = false
where package_code = 'gc_999';

update purchase_orders
set provider = 'manual'
where provider = 'sandbox';

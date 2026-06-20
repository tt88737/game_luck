update compliance_regions
set purchase_allowed = true
where country_code = 'US'
  and state_code in ('CA', 'TX', 'NJ')
  and status = 'active';

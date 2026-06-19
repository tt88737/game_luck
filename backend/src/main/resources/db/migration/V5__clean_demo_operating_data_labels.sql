update kyc_applications
set legal_name = 'P1 User'
where legal_name = 'P1 Demo User';

update kyc_applications
set review_reason = 'approved by ops'
where review_reason = 'sandbox approved by ops';

update redemption_requests
set method = 'gift_card',
    sandbox_only = false
where method = 'sandbox_gift_card';

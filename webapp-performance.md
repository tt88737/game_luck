# Webapp Performance

Baseline only. No formal performance threshold was provided.

## Page Load Baseline

Final browser validation measured route navigation with `networkidle`.

| Route | Desktop EN | Mobile EN | Desktop zh-CN |
| --- | ---: | ---: | ---: |
| `/app/register` | 649 ms | 609 ms | 629 ms |
| `/app` | 545 ms | 546 ms | 554 ms |
| `/app/store` | 573 ms | 551 ms | 561 ms |
| `/app/kyc` | 540 ms | 545 ms | 543 ms |
| `/app/redemption` | 543 ms | 549 ms | 545 ms |
| `/app/wallet` | 543 ms | 545 ms | 545 ms |
| `/admin` | 555 ms | 570 ms | 563 ms |
| `/admin/campaigns` | 544 ms | 548 ms | 545 ms |
| `/admin/p1` | 556 ms | 561 ms | 557 ms |

## Observations

- No failed network responses in the final run.
- No console errors in the final run.
- No load or stress test was executed.

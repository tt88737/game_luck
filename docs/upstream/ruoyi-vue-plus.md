# RuoYi-Vue-Plus Upstream

| Item | Value |
| --- | --- |
| Upstream | https://github.com/dromara/RuoYi-Vue-Plus |
| Imported At | 2026-06-25 |
| Imported Branch | 5.X |
| Imported Commit | e49f02f89e17ee5a4cc14048af99cc83d72872a7 |
| Local Path | backend/ |

## Local Rules

- Keep framework core changes minimal.
- Put package-network business modules under clearly named module packages.
- Do not bypass RuoYi-Vue-Plus permission, tenant, or data-scope mechanisms.
- Record any upstream merge or manual cherry-pick in this file.
- Treat `ruoyi-common`, `ruoyi-admin`, and shared framework configuration as high-risk areas.
- Prefer adding package-network modules under `ruoyi-modules` or a clearly documented project module.

## Import Notes

- The upstream `.git` directory was not copied into `backend/`.
- This repository keeps RuoYi-Vue-Plus source as vendored project code rather than a git submodule.
- Future upstream updates should be reviewed and merged intentionally, not pulled directly into production branches.

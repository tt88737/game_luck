# Plus UI Upstream

| Item | Value |
| --- | --- |
| Upstream | https://github.com/GameLuck/GameLuck Admin UI |
| Imported At | 2026-06-25 |
| Imported Branch | 5.X |
| Imported Commit | d0d451967676707021b9857df529c395b27e90a7 |
| Local Path | admin-ui/ |

## Local Rules

- Keep framework-level changes minimal.
- Use `admin-ui/` only for GameLuck Backend Base B-side admin frontend.
- Do not mix player H5 pages into `admin-ui/`; player H5 belongs in the future `h5/` app.
- Record backend API URL changes in this file or related implementation docs.
- Future upstream updates should be reviewed and merged intentionally.

## Import Notes

- The upstream `.git` directory was not copied into `admin-ui/`.
- This repository keeps GameLuck Admin UI source as vendored project code rather than a git submodule.

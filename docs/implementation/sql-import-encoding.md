# SQL 导入编码规则

## 背景

钱包菜单曾出现 `??????` 乱码。根因不是浏览器字体，而是 PowerShell 通过管道把含中文 SQL 传给 `mysql.exe` 时发生转码，数据库最终保存的就是问号。

## 强制规则

含中文的 SQL 文件必须使用 `mysql --default-character-set=utf8mb4 ... -e "source ..."` 导入，或使用封装脚本：

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
```

不要使用下面这些方式导入含中文 SQL：

```powershell
Get-Content -Raw backend\script\sql\gameluck_wallet.sql | mysql -uroot -proot gameluck_vue
mysql -uroot -proot gameluck_vue < backend\script\sql\gameluck_wallet.sql
```

第一种会让 PowerShell 参与文本转码，可能把中文变成 `?`。第二种在 PowerShell 中本身不可用。

## 验证

导入后检查菜单中文：

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot gameluck_vue -e "select menu_id, menu_name from sys_menu where menu_id between 1800 and 1819 order by menu_id;"
```

检查客户端和数据库字符集：

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot gameluck_vue -e "show variables like 'character_set%';"
```

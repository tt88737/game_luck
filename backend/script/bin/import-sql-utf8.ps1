param(
    [Parameter(Mandatory = $true)]
    [string] $SqlPath,

    [string] $Database = "gameluck_vue",
    [string] $User = "root",
    [string] $Password = "root"
)

$ErrorActionPreference = "Stop"

$resolved = Resolve-Path -LiteralPath $SqlPath
$sourcePath = $resolved.Path.Replace("\", "/")

# Do not pipe SQL text through PowerShell. Let mysql read the file itself so
# UTF-8 Chinese menu names are not converted to '?' before reaching MySQL.
& mysql --default-character-set=utf8mb4 "-u$User" "-p$Password" $Database -e "source $sourcePath"

if ($LASTEXITCODE -ne 0) {
    throw "mysql import failed with exit code $LASTEXITCODE"
}

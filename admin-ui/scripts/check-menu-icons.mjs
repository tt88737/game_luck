import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const rootDir = path.resolve(import.meta.dirname, '..', '..');
const iconDir = path.join(rootDir, 'admin-ui', 'src', 'assets', 'icons', 'svg');
const sqlFiles = [
  path.join(rootDir, 'backend', 'script', 'sql', 'gameluck_wallet.sql')
];

const validIcons = new Set(
  fs.readdirSync(iconDir)
    .filter((file) => file.endsWith('.svg'))
    .map((file) => path.basename(file, '.svg'))
);

const allowedEmptyIcons = new Set(['', '#']);
const errors = [];

for (const sqlFile of sqlFiles) {
  const sql = fs.readFileSync(sqlFile, 'utf8');
  const menuRows = extractMenuRows(sql);
  for (const row of menuRows) {
    const columns = splitSqlTuple(row);
    if (columns.length < 14) {
      continue;
    }
    const menuId = unquoteSql(columns[0]);
    const menuName = unquoteSql(columns[1]);
    const icon = unquoteSql(columns[13]);
    if (!allowedEmptyIcons.has(icon) && !validIcons.has(icon)) {
      errors.push(`${path.relative(rootDir, sqlFile)} menu_id=${menuId} menu_name=${menuName} icon=${icon}`);
    }
  }
}

if (errors.length > 0) {
  console.error('Invalid sys_menu icon values. Use files from admin-ui/src/assets/icons/svg only:');
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exit(1);
}

console.log(`Menu icon check passed. ${validIcons.size} local svg icons available.`);

function extractMenuRows(sql) {
  const rows = [];
  const insertPattern = /INSERT\s+INTO\s+sys_menu[\s\S]*?VALUES([\s\S]*?)(?:ON\s+DUPLICATE\s+KEY\s+UPDATE|;)/gi;
  let insertMatch;
  while ((insertMatch = insertPattern.exec(sql)) !== null) {
    const valuesBlock = insertMatch[1];
    const tuplePattern = /\((?:[^'()]|'(?:''|[^'])*')*\)/g;
    let tupleMatch;
    while ((tupleMatch = tuplePattern.exec(valuesBlock)) !== null) {
      rows.push(tupleMatch[0].slice(1, -1));
    }
  }
  return rows;
}

function splitSqlTuple(tuple) {
  const columns = [];
  let current = '';
  let inQuote = false;

  for (let i = 0; i < tuple.length; i++) {
    const char = tuple[i];
    const next = tuple[i + 1];

    if (char === "'") {
      current += char;
      if (inQuote && next === "'") {
        current += next;
        i++;
      } else {
        inQuote = !inQuote;
      }
      continue;
    }

    if (char === ',' && !inQuote) {
      columns.push(current.trim());
      current = '';
      continue;
    }

    current += char;
  }

  columns.push(current.trim());
  return columns;
}

function unquoteSql(value) {
  const trimmed = value.trim();
  if (/^null$/i.test(trimmed)) {
    return '';
  }
  if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
    return trimmed.slice(1, -1).replace(/''/g, "'");
  }
  return trimmed;
}

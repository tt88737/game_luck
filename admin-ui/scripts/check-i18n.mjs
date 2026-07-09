import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const rootDir = path.resolve(import.meta.dirname, '..', '..');
const hanPattern = /[\p{Script=Han}]/u;
const errors = [];

checkBackendBundles();
checkFrontendSource();
checkBackendSource();

if (errors.length > 0) {
  console.error('i18n check failed. Put visible text behind tt()/t() on frontend or MessageUtils/i18n keys on backend.');
  for (const error of errors.slice(0, 200)) {
    console.error(`- ${error}`);
  }
  if (errors.length > 200) {
    console.error(`... ${errors.length - 200} more`);
  }
  process.exit(1);
}

console.log('i18n check passed. Frontend/backend hardcoded visible text guards are clean.');

function checkBackendBundles() {
  const bundleDir = path.join(rootDir, 'backend', 'gameluck-admin', 'src', 'main', 'resources', 'i18n');
  const base = readPropertyKeys(path.join(bundleDir, 'messages.properties'));
  const zh = readPropertyKeys(path.join(bundleDir, 'messages_zh_CN.properties'));
  const en = readPropertyKeys(path.join(bundleDir, 'messages_en_US.properties'));

  for (const duplicate of findDuplicates(base.keysInOrder)) {
    errors.push(`backend i18n duplicate key: ${duplicate}`);
  }

  compareKeys('messages.properties', base.keys, 'messages_zh_CN.properties', zh.keys);
  compareKeys('messages.properties', base.keys, 'messages_en_US.properties', en.keys);
}

function checkFrontendSource() {
  const sourceDir = path.join(rootDir, 'admin-ui', 'src');
  const allowedFiles = new Set([
    normalizePath(path.join(sourceDir, 'utils', 'i18nText.ts')),
    normalizePath(path.join(sourceDir, 'utils', 'i18nTitle.ts')),
    normalizePath(path.join(sourceDir, 'utils', 'errorCode.ts'))
  ]);

  for (const file of walk(sourceDir, ['.vue', '.ts', '.tsx', '.js'])) {
    const normalized = normalizePath(file);
    if (allowedFiles.has(normalized) || normalized.includes('/locale/lang/') || normalized.endsWith('/src/lang/zh_CN.ts')) {
      continue;
    }
    const lines = stripBlockComments(fs.readFileSync(file, 'utf8')).split(/\r?\n/);
    lines.forEach((line, index) => {
      const code = stripLineComment(line);
      if (!hanPattern.test(code) || isFrontendAllowedLine(code)) {
        return;
      }
      errors.push(`${relative(file)}:${index + 1} frontend raw Chinese text: ${code.trim()}`);
    });
  }
}

function checkBackendSource() {
  const backendDir = path.join(rootDir, 'backend');
  for (const file of walk(backendDir, ['.java'])) {
    const normalized = normalizePath(file);
    if (normalized.includes('/target/') || normalized.includes('/src/test/')) {
      continue;
    }
    const lines = stripBlockComments(fs.readFileSync(file, 'utf8')).split(/\r?\n/);
    lines.forEach((line, index) => {
      const code = stripLineComment(line);
      if (!hanPattern.test(code) || isBackendAllowedLine(code)) {
        return;
      }
      errors.push(`${relative(file)}:${index + 1} backend raw Chinese text: ${code.trim()}`);
    });
  }
}

function isFrontendAllowedLine(line) {
  const trimmed = line.trim();
  if (
    trimmed.startsWith('//') ||
    trimmed.startsWith('/*') ||
    trimmed.startsWith('*') ||
    trimmed.startsWith('<!--') ||
    trimmed.endsWith('-->')
  ) {
    return true;
  }
  return (
    line.includes('tt(') ||
    line.includes('t(') ||
    line.includes('translateTitle(') ||
    line.includes('cssContent(') ||
    /^\s*(title|meta|name):/.test(line)
  );
}

function isBackendAllowedLine(line) {
  const trimmed = line.trim();
  if (
    trimmed.startsWith('//') ||
    trimmed.startsWith('/*') ||
    trimmed.startsWith('*') ||
    trimmed.startsWith('@Schema') ||
    trimmed.startsWith('@Excel') ||
    trimmed.startsWith('@Log') ||
    trimmed.includes('log.') ||
    trimmed.includes('SnailJobLog.') ||
    trimmed.includes('MessageUtils.message') ||
    trimmed.includes('message(') ||
    trimmed.includes('message = {') ||
    trimmed.includes('StringUtils.format(MessageUtils') ||
    trimmed.includes('RegExUtils.replaceAll')
  ) {
    return true;
  }
  return false;
}

function readPropertyKeys(file) {
  const keysInOrder = [];
  const keys = new Set();
  const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#') || trimmed.startsWith('!')) {
      continue;
    }
    const eq = line.indexOf('=');
    if (eq === -1) {
      continue;
    }
    const key = line.slice(0, eq).trim();
    keysInOrder.push(key);
    keys.add(key);
  }
  return { keys, keysInOrder };
}

function compareKeys(leftName, left, rightName, right) {
  for (const key of left) {
    if (!right.has(key)) {
      errors.push(`${rightName} missing key from ${leftName}: ${key}`);
    }
  }
  for (const key of right) {
    if (!left.has(key)) {
      errors.push(`${rightName} has extra key not in ${leftName}: ${key}`);
    }
  }
}

function findDuplicates(keys) {
  const seen = new Set();
  const duplicates = new Set();
  for (const key of keys) {
    if (seen.has(key)) {
      duplicates.add(key);
    }
    seen.add(key);
  }
  return [...duplicates];
}

function walk(dir, extensions) {
  const result = [];
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    const normalized = normalizePath(fullPath);
    if (entry.isDirectory()) {
      if (
        normalized.includes('/node_modules/') ||
        normalized.includes('/dist/') ||
        normalized.includes('/target/') ||
        normalized.includes('/.git/')
      ) {
        continue;
      }
      result.push(...walk(fullPath, extensions));
      continue;
    }
    if (extensions.includes(path.extname(entry.name))) {
      result.push(fullPath);
    }
  }
  return result;
}

function normalizePath(file) {
  return file.replaceAll(path.sep, '/');
}

function relative(file) {
  return normalizePath(path.relative(rootDir, file));
}

function stripLineComment(line) {
  let inSingle = false;
  let inDouble = false;
  let inTemplate = false;

  for (let i = 0; i < line.length - 1; i++) {
    const char = line[i];
    const prev = line[i - 1];
    if (char === "'" && !inDouble && !inTemplate && prev !== '\\') {
      inSingle = !inSingle;
      continue;
    }
    if (char === '"' && !inSingle && !inTemplate && prev !== '\\') {
      inDouble = !inDouble;
      continue;
    }
    if (char === '`' && !inSingle && !inDouble && prev !== '\\') {
      inTemplate = !inTemplate;
      continue;
    }
    if (char === '/' && line[i + 1] === '/' && !inSingle && !inDouble && !inTemplate) {
      return line.slice(0, i);
    }
  }
  return line;
}

function stripBlockComments(text) {
  return text.replace(/\/\*[\s\S]*?\*\//g, (match) => match.replace(/[^\r\n]/g, ''));
}

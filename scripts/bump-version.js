#!/usr/bin/env node

/**
 * SemVer Version Bumping & Release Automation Script
 *
 * Usage:
 *   node scripts/bump-version.js patch
 *   node scripts/bump-version.js minor
 *   node scripts/bump-version.js major
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const rootDir = path.resolve(__dirname, '..');
const gradleFilePath = path.join(rootDir, 'app', 'build.gradle.kts');
const packageJsonPath = path.join(rootDir, 'package.json');
const packageLockPath = path.join(rootDir, 'package-lock.json');

const bumpType = (process.argv[2] || '').toLowerCase();
if (!['patch', 'minor', 'major'].includes(bumpType)) {
  console.error('\x1b[31m[ERROR] Invalid or missing release bump type.\x1b[0m');
  console.error('Usage:');
  console.error('  npm run release:patch  (e.g., 0.0.1 -> 0.0.2)');
  console.error('  npm run release:minor  (e.g., 0.0.2 -> 0.1.0)');
  console.error('  npm run release:major  (e.g., 0.1.0 -> 1.0.0)');
  process.exit(1);
}

// Helper to read file content stripped of UTF-8 BOM
function readFileWithoutBom(filePath) {
  return fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, '');
}

// 1. Read app/build.gradle.kts
if (!fs.existsSync(gradleFilePath)) {
  console.error(`\x1b[31m[ERROR] build.gradle.kts not found at: ${gradleFilePath}\x1b[0m`);
  process.exit(1);
}

let gradleContent = readFileWithoutBom(gradleFilePath);

const versionCodeMatch = gradleContent.match(/versionCode\s*=\s*(\d+)/);
const versionNameMatch = gradleContent.match(/versionName\s*=\s*"([^"]+)"/);

if (!versionCodeMatch || !versionNameMatch) {
  console.error('\x1b[31m[ERROR] Could not parse versionCode or versionName from app/build.gradle.kts\x1b[0m');
  process.exit(1);
}

const currentVersionCode = parseInt(versionCodeMatch[1], 10);
const currentVersionName = versionNameMatch[1];

// 2. Parse SemVer components
const semverMatch = currentVersionName.match(/^(\d+)\.(\d+)\.(\d+)(.*)$/);
if (!semverMatch) {
  console.error(`\x1b[31m[ERROR] Current versionName "${currentVersionName}" is not valid SemVer (X.Y.Z).\x1b[0m`);
  process.exit(1);
}

let major = parseInt(semverMatch[1], 10);
let minor = parseInt(semverMatch[2], 10);
let patch = parseInt(semverMatch[3], 10);

switch (bumpType) {
  case 'patch':
    patch += 1;
    break;
  case 'minor':
    minor += 1;
    patch = 0;
    break;
  case 'major':
    major += 1;
    minor = 0;
    patch = 0;
    break;
}

const nextVersionName = `${major}.${minor}.${patch}`;
const nextVersionCode = currentVersionCode + 1;

console.log('\x1b[36m==================================================\x1b[0m');
console.log(`🚀 Bumping version [${bumpType.toUpperCase()}]:`);
console.log(`   versionName : \x1b[33m${currentVersionName}\x1b[0m -> \x1b[32m${nextVersionName}\x1b[0m`);
console.log(`   versionCode : \x1b[33m${currentVersionCode}\x1b[0m -> \x1b[32m${nextVersionCode}\x1b[0m`);
console.log('\x1b[36m==================================================\x1b[0m');

// 3. Update app/build.gradle.kts
gradleContent = gradleContent.replace(
  /versionCode\s*=\s*\d+/,
  `versionCode = ${nextVersionCode}`
);
gradleContent = gradleContent.replace(
  /versionName\s*=\s*"[^"]+"/,
  `versionName = "${nextVersionName}"`
);
fs.writeFileSync(gradleFilePath, gradleContent, 'utf8');
console.log(`✔ Updated ${path.relative(rootDir, gradleFilePath)}`);

// 4. Update package.json
if (fs.existsSync(packageJsonPath)) {
  const rawPkg = readFileWithoutBom(packageJsonPath);
  const pkg = JSON.parse(rawPkg);
  pkg.version = nextVersionName;
  fs.writeFileSync(packageJsonPath, JSON.stringify(pkg, null, 2) + '\n', 'utf8');
  console.log(`✔ Updated package.json`);
}

// 5. Update package-lock.json if present
if (fs.existsSync(packageLockPath)) {
  const rawPkgLock = readFileWithoutBom(packageLockPath);
  const pkgLock = JSON.parse(rawPkgLock);
  pkgLock.version = nextVersionName;
  if (pkgLock.packages && pkgLock.packages['']) {
    pkgLock.packages[''].version = nextVersionName;
  }
  fs.writeFileSync(packageLockPath, JSON.stringify(pkgLock, null, 2) + '\n', 'utf8');
  console.log(`✔ Updated package-lock.json`);
}

// 6. Stage, commit, and tag in Git
const tagName = `v${nextVersionName}`;
const commitMsg = `chore(release): ${tagName} (build ${nextVersionCode})`;

try {
  execSync(`git add "${gradleFilePath}" "${packageJsonPath}"`, { cwd: rootDir, stdio: 'inherit' });
  if (fs.existsSync(packageLockPath)) {
    execSync(`git add "${packageLockPath}"`, { cwd: rootDir, stdio: 'inherit' });
  }

  execSync(`git commit -m "${commitMsg}"`, { cwd: rootDir, stdio: 'inherit' });
  execSync(`git tag -a "${tagName}" -m "Release ${tagName}"`, { cwd: rootDir, stdio: 'inherit' });

  // Determine current git branch
  let currentBranch = 'main';
  try {
    currentBranch = execSync('git rev-parse --abbrev-ref HEAD', { cwd: rootDir, encoding: 'utf8' }).trim();
  } catch (_) {}

  console.log('\n\x1b[32m✨ Release successfully created!\x1b[0m');
  console.log(`   Tag:    \x1b[33m${tagName}\x1b[0m`);
  console.log(`   Commit: \x1b[33m${commitMsg}\x1b[0m`);
  console.log('\n\x1b[36m👉 Next Step:\x1b[0m Push changes and tags to trigger Google Play deployment:');
  console.log(`   \x1b[32mgit push origin ${currentBranch} --tags\x1b[0m\n`);
} catch (err) {
  console.error('\x1b[31m[ERROR] Failed to create git commit/tag:\x1b[0m', err.message);
  process.exit(1);
}
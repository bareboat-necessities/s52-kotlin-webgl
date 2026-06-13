#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const url = process.argv[2] ?? 'http://localhost:8080/#opencpn-regression';
const output = resolve(process.argv[3] ?? 'build/-regression.png');
const candidates = [
  process.env.CHROME_BIN,
  'chromium',
  'chromium-browser',
  'google-chrome',
  'google-chrome-stable',
  'chrome'
].filter(Boolean);

const chrome = candidates.find((candidate) => {
  const probe = spawnSync(candidate, ['--version'], { stdio: 'ignore' });
  return probe.status === 0;
});

if (!chrome) {
  console.error('Could not find Chromium/Chrome. Set CHROME_BIN to a browser executable.');
  process.exit(2);
}

mkdirSync(dirname(output), { recursive: true });
const args = [
  '--headless=new',
  '--disable-gpu',
  '--no-sandbox',
  '--hide-scrollbars',
  '--window-size=1280,900',
  '--virtual-time-budget=5000',
  `--screenshot=${output}`,
  url
];

const result = spawnSync(chrome, args, { stdio: 'inherit' });
if (result.status !== 0 || !existsSync(output)) {
  console.error(`Failed to export regression PNG to ${output}`);
  process.exit(result.status || 1);
}
console.log(`Wrote ${output}`);

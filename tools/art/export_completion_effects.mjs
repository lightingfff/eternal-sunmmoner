#!/usr/bin/env node
// Offline-only MIT source export. Actor/hitstop/timing metadata does not enter the simulation.
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadEffect, renderFrames, spriteSheet, contactSheet } from './vendor/pixelfx/tools/lib/fxlib.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
for (const id of ['melee/dagger-flurry', 'melee/spin-slash', 'impacts/hit-spark', 'impacts/ground-slam', 'holy/sunburst']) {
  const fx = await loadEffect(id);
  if (fx.mod.act?.onBody) throw new Error(`${id} uses forbidden stock-body art`);
  const rendered = renderFrames(fx, { actor: false, bg: 'none', seed: 19 });
  const name = id.split('/')[1];
  const output = join(root, 'docs/art/art-completion/effects/source', name);
  mkdirSync(output, { recursive: true });
  const sheet = spriteSheet(fx, rendered, name);
  writeFileSync(join(output, `${name}.png`), sheet.png);
  writeFileSync(join(output, `${name}.json`), JSON.stringify(sheet.json, null, 2) + '\n');
  writeFileSync(join(output, 'contact.png'), contactSheet(fx, rendered, { max: 16, zoom: 2 }));
  console.log(`${id}: ${rendered.n} genuine source frames exported, actor excluded`);
}

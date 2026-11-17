#!/usr/bin/env node
// Offline exports of the pinned, MIT-licensed selected PixelFX modules. No provider/network runtime.
import { mkdirSync, writeFileSync } from 'node:fs';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadEffect, renderFrames, spriteSheet, contactSheet } from './vendor/pixelfx/tools/lib/fxlib.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const output = process.argv[2] ?? join(root, 'docs/art/art-depth-reforge/pixelfx-source');
const selections = ['impacts/shield-block', 'arcane/mana-burst', 'melee/sword-slash', 'holy/heal-aoe'];
for (const id of selections) {
  const fx = await loadEffect(id);
  if (fx.mod.act?.onBody) throw new Error(`${id}: stock-body artwork is not permitted in this pack`);
  const rendered = renderFrames(fx, { actor: false, bg: 'none', seed: 19 });
  const name = id.split('/')[1];
  const target = join(output, name);
  mkdirSync(target, { recursive: true });
  const sheet = spriteSheet(fx, rendered, name);
  writeFileSync(join(target, `${name}.png`), sheet.png);
  writeFileSync(join(target, `${name}.json`), JSON.stringify(sheet.json, null, 2) + '\n');
  writeFileSync(join(target, 'contact.png'), contactSheet(fx, rendered, { max: 16, zoom: 2 }));
  console.log(`${id}: ${rendered.n} frames; original effect only; seed19`);
}

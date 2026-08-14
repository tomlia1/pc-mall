import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const workflow = await readFile(new URL('../../.github/workflows/ci.yml', import.meta.url), 'utf8')

assert.match(workflow, /node-version: '24'/, 'CI must use Node 24 so the TypeScript API regression test can run')

console.log('CI Node runtime verification passed')

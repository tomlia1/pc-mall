import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const marketplace = await readFile(new URL('../src/MarketplaceApp.vue', import.meta.url), 'utf8')
const chat = await readFile(new URL('../src/components/ChatWidget.vue', import.meta.url), 'utf8')

assert.match(marketplace, /class="build-desktop"/, '商城必须保留桌面端常驻装机单')
assert.match(chat, /const defaultPanelSize = \{ width: 600, height: 720 \}/, '聊天窗应以最大桌面宽度打开')
assert.doesNotMatch(chat, /window\.innerWidth <= 720/, '聊天窗不应保留移动端尺寸分支')

console.log('desktop catalog verification passed')

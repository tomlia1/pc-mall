import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const marketplace = await readFile(new URL('../src/MarketplaceApp.vue', import.meta.url), 'utf8')
const chat = await readFile(new URL('../src/components/ChatWidget.vue', import.meta.url), 'utf8')
const styles = await readFile(new URL('../src/marketplace.css', import.meta.url), 'utf8')

assert.match(marketplace, /const desktopBuildOpen = ref\(false\)/, '桌面端装机单默认应收起')
assert.match(marketplace, /desktopBuildOpen\.value = true/, '加入硬件后应自动展开装机单')
assert.match(marketplace, /v-if="desktopBuildOpen" class="build-desktop"/, '装机单打开时才占用桌面目录空间')
assert.match(marketplace, /class="desktop-build-toggle"/, '桌面端应提供打开装机单的入口')
assert.match(styles, /@media \(max-width: 980px\) \{[\s\S]*\.build-mobile-summary/, '移动端必须保留打开装机单的入口')
assert.match(chat, /const defaultPanelSize = \{ width: 600, height: 720 \}/, '聊天窗应以最大桌面宽度打开')
assert.doesNotMatch(chat, /window\.innerWidth <= 720/, '聊天窗不应保留移动端尺寸分支')

console.log('desktop catalog verification passed')

import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = await readFile(new URL('../src/components/BuildListPanel.vue', import.meta.url), 'utf8')

assert.match(source, /function buildListText\(\)/, '装机单需要生成可复制、可导出的纯文本内容')
assert.match(source, /复制清单/, '装机单需要提供复制清单操作')
assert.match(source, /导出清单/, '装机单需要提供导出清单操作')
assert.match(source, /URL\.createObjectURL/, '导出清单需要下载本地文本文件')
assert.match(source, /缺少槽位/, '未配齐时导出的清单必须说明缺少的槽位')
assert.match(source, /基础检查/, '导出的清单必须包含当前基础检查结果')

console.log('build list export verification passed')

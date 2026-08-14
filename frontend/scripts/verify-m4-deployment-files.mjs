import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const compose = await readFile(new URL('../../docker-compose.yml', import.meta.url), 'utf8')
const nginx = await readFile(new URL('../../deploy/nginx.conf', import.meta.url), 'utf8')

function serviceBlock(name) {
  const start = compose.indexOf(`  ${name}:\n`)
  assert.notEqual(start, -1, `missing ${name} service`)
  const rest = compose.slice(start + 1)
  const nextService = rest.search(/\n  [a-z][a-z0-9_-]*:\n/)
  return nextService === -1 ? rest : rest.slice(0, nextService)
}

const mysql = serviceBlock('mysql')
const app = serviceBlock('app')
const web = serviceBlock('web')

assert.match(web, /ports:\s+\- "80:80"/, 'only the web service should publish port 80')
assert.doesNotMatch(mysql, /ports:/, 'MySQL must not publish a host port')
assert.doesNotMatch(app, /ports:/, 'the application must stay behind Nginx')
assert.match(nginx, /location \/api\//, 'Nginx must proxy API requests')
assert.match(nginx, /proxy_pass http:\/\/app:8081/, 'Nginx must proxy to the configured Spring Boot port')
assert.match(nginx, /try_files \$uri \$uri\/ \/index\.html/, 'Nginx must support direct product-detail routes')
assert.match(nginx, /location \/swagger-ui\//, 'Swagger UI must remain available through Nginx')
assert.match(nginx, /location \/v3\/api-docs\//, 'OpenAPI JSON must remain available through Nginx')

console.log('M4 deployment file verification passed')

# PC Mall Docker Deployment

The Compose stack has three services:

- `web`: Nginx serves the Vue production build on port 80 and proxies `/api`, Swagger and health requests.
- `app`: Spring Boot runs only on the internal Docker network at port 8081.
- `mysql`: MySQL runs only on the internal Docker network at port 3306. Its data is stored in the named volume `mysql_data`.

## Start locally

1. Copy `.env.example` to `.env` and replace every password placeholder. Keep `DEEPSEEK_API_KEY` blank when AI is not needed.
2. Run `docker compose up --build -d` from the repository root.
3. Open `http://localhost/`.

## Validate

Run `docker compose ps`, then open:

- `http://localhost/`
- `http://localhost/swagger-ui/index.html`
- `http://localhost/actuator/health`

The Nginx `try_files` fallback keeps direct routes such as `/products/1` working after a refresh.

# OptiGridZA frontend

React and Vite dashboard for the OptiGridZA energy optimisation platform.

## Run locally

Install dependencies and start the development server:

```sh
npm ci
npm run dev
```

The frontend uses `http://localhost:8080` as the API gateway by default. To use a
different gateway, set `VITE_API_URL` before starting Vite:

```sh
VITE_API_URL=https://your-api.example.com npm run dev
```

`VITE_API_URL` is embedded into the browser bundle at build time. Set it to the
deployed API gateway URL when building for production. Configure the gateway's
`CORS_ALLOWED_ORIGIN` environment variable to the exact frontend origin.

## Checks

```sh
npm run lint
npm run build
```

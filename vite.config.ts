import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';

// GitHub Pages project sites are served from /<repo>/, so assets need that
// prefix. Override with BASE_PATH=/ when serving from a custom domain or a
// user/organisation page.
const base = process.env.BASE_PATH ?? '/ba-world-scouter/';

export default defineConfig({
  base,
  plugins: [svelte()],
  build: {
    target: 'es2022',
  },
});

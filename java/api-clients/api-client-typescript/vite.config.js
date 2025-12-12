import { resolve } from 'path';
import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    lib: {
      entry: resolve(__dirname, 'build/dist/index.js'),
      name: 'OenDCSApi',
      fileName: 'opendcs-api', // Output file will be my-library.js
    },
    minify: "esbuild"
  },
});

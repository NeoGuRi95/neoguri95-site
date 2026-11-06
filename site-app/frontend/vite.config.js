import { defineConfig } from 'vite';

// 번들은 Gradle processResources가 static/ 아래로 복사한다.
export default defineConfig({
  build: {
    outDir: '../build/frontend',
    emptyOutDir: true,
    rolldownOptions: {
      input: 'src/editor.js',
      output: {
        entryFileNames: 'js/[name].js',
        chunkFileNames: 'js/[name].js',
        assetFileNames: 'assets/[name][extname]',
      },
    },
  },
});

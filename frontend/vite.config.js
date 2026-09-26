import {fileURLToPath, URL} from "node:url";
import {defineConfig} from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
    plugins: [vue()],
    build: {
        outDir: "dist",
        emptyOutDir: true,
        rollupOptions: {
            input: {
                library: fileURLToPath(new URL("./index.html", import.meta.url)),
                chat: fileURLToPath(new URL("./chat.html", import.meta.url)),
                practice: fileURLToPath(new URL("./practice.html", import.meta.url))
            }
        }
    },
    server: {
        port: 5173,
        proxy: {
            "/api": "http://localhost:8080"
        }
    }
});

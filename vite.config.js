export default {
    server: {
        port: 5173,
        open: true,
        // Better file watching on Windows
        watch: {
            usePolling: true
        }
    },
    build: {
        target: 'esnext',
        outDir: 'dist'
    }
}

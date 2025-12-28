@echo off
ren "index_clean.html" "game.html"
ren "index.html" "delete_old_complex.html"
ren "index_client.html" "delete_client.html"
ren "index_pixi.html" "delete_pixi.html"
ren "README-START.md" "README.md"
del "http-server.js"
echo Files renamed successfully!
pause

#!/bin/bash
# SDK POC Client Entrypoint - Environment Setup Only

export DISPLAY=:99
rm -f /tmp/.X99-lock /tmp/.X11-unix/X99

# Start GUI Environment
Xvfb :99 -screen 0 1280x1024x24 &
sleep 1
fluxbox &
x11vnc -display :99 -forever -nopw -shared -rfbport 5900 &
websockify --web /usr/share/novnc/ 6080 localhost:5900 &

echo "==============================================================="
echo "Client environment started."
echo "Access noVNC: http://localhost:<HOST_PORT>/vnc.html?autoconnect=true"
echo "To start test: docker exec -e DISPLAY=:99 <container> node /app/run-test.js"
echo "==============================================================="

# Keep container alive
tail -f /dev/null

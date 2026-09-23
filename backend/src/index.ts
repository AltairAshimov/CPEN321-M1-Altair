import http from 'node:http';
import { createApp } from './app';
import { env } from './config/env';
import {
  closeWebSocketConnections,
  startAndroidWebSocketServer,
  startCourseWebSocketClient,
} from './websocket-bridge';

const app = createApp();
const server = http.createServer(app);

server.listen(env.port, () => {
  console.log(`Server listening on port ${env.port}`);
});

startAndroidWebSocketServer(server);
startCourseWebSocketClient();

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    closeWebSocketConnections();
    server.close(() => {
      process.exit(0);
    });
  });
}

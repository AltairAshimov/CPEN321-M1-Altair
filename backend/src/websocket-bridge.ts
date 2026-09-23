import http from 'node:http';
import WebSocket, { WebSocketServer } from 'ws';

const COURSE_WS_URL = 'wss://8.229.22.124';
const androidClients = new Set<WebSocket>();
let courseSocket: WebSocket | null = null;

export function broadcastToAndroidClients(payload: string): void {
  for (const client of androidClients) {
    if (client.readyState === WebSocket.OPEN) {
      client.send(payload);
    }
  }
}

export function startAndroidWebSocketServer(server: http.Server): WebSocketServer {
  const wss = new WebSocketServer({ server, path: '/ws/pixels' });

  wss.on('connection', (socket) => {
    console.log('Android WebSocket client connected');
    androidClients.add(socket);

    socket.on('close', () => {
      console.log('Android WebSocket client disconnected');
      androidClients.delete(socket);
    });
  });

  return wss;
}

export function startCourseWebSocketClient(): WebSocket {
  const socket = new WebSocket(COURSE_WS_URL);
  courseSocket = socket;

  socket.on('open', () => {
    console.log('Connected to course WebSocket');
  });

  socket.on('message', (data) => {
    const rawMessage = data.toString();
    console.log('Course WebSocket message:', rawMessage);
    broadcastToAndroidClients(rawMessage);
  });

  socket.on('error', (error) => {
    console.error('Course WebSocket error:', error);
  });

  socket.on('close', (code, reason) => {
    console.log(`Course WebSocket closed: code=${code}, reason=${reason.toString()}`);
    courseSocket = null;
  });

  return socket;
}

export function closeWebSocketConnections(): void {
  for (const client of androidClients) {
    if (client.readyState === WebSocket.OPEN || client.readyState === WebSocket.CONNECTING) {
      client.close();
    }
    androidClients.delete(client);
  }

  if (courseSocket && courseSocket.readyState === WebSocket.OPEN) {
    courseSocket.close();
  }
}

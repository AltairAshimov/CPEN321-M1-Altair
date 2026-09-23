import express, { type Express } from 'express';
import { networkInterfaces } from 'node:os';

function getLocalServerIp(): string {
  const interfaces = networkInterfaces();

  for (const name of Object.keys(interfaces)) {
    const addresses = interfaces[name] ?? [];

    for (const address of addresses) {
      if (address.family === 'IPv4' && !address.internal) {
        return address.address;
      }
    }
  }

  return '127.0.0.1';
}

function getServerTimeString(): string {
  const now = new Date();

  const time = now.toLocaleTimeString('en-GB', {
    hour12: false,
  });

  const offsetMinutes = now.getTimezoneOffset();
  const offsetSign = offsetMinutes <= 0 ? '+' : '-';
  const absOffsetMinutes = Math.abs(offsetMinutes);
  const offsetHours = Math.floor(absOffsetMinutes / 60);
  const offsetRemainingMinutes = absOffsetMinutes % 60;

  const formattedOffset = `${String(offsetHours).padStart(2, '0')}:${String(offsetRemainingMinutes).padStart(2, '0')}`;

  return `${time} GMT${offsetSign}${formattedOffset}`;
}

export function createApp(): Express {
  const app = express();

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.get('/api/name', (_req, res) => {
    res.json({
      firstName: 'Altair',
      lastName: 'Ashimov',
    });
  });

  app.get('/api/server-time', (_req, res) => {
    res.json({ time: getServerTimeString() });
  });

  app.get('/api/server-ip', (_req, res) => {
    res.json({ ip: getLocalServerIp() });
  });

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}

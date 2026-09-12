// Minimal stand-in for the real API so the frontend can be developed offline.
//   node scripts/mock-api.mjs      → http://localhost:8787/api/v1
// Point the app at it with VITE_API_BASE=http://localhost:8787/api/v1
import { createServer } from 'node:http';

const PORT = Number(process.env.PORT ?? 8787);
const WORLD_IDS = [302, 308, 313, 408, 474, 522, 527, 612, 613, 620, 624, 631];

const now = () => Math.floor(Date.now() / 1000);

const worlds = new Map(
  WORLD_IDS.map((id) => [
    id,
    {
      worldId: id,
      population: 80 + Math.floor(Math.random() * 420),
      confirmed: {
        time: now() - Math.floor(Math.random() * 900),
        x: 8000 + Math.floor(Math.random() * 8000),
        y: Math.floor(Math.random() * 16383),
      },
      prediction: { based_on: 0, time: 0, y: 0 },
    },
  ]),
);
for (const world of worlds.values()) {
  world.prediction = { based_on: world.confirmed.time, time: now(), y: world.confirmed.y };
}

const clients = new Set();

// Roll one random world forward every couple of seconds, like a plugin report.
setInterval(() => {
  const id = WORLD_IDS[Math.floor(Math.random() * WORLD_IDS.length)];
  const world = worlds.get(id);
  const t = now();
  world.confirmed = {
    time: t,
    x: (world.confirmed.x + 137) % 16383,
    y: (world.confirmed.y + 1 + Math.floor(Math.random() * 40)) % 16383,
  };
  world.prediction = { based_on: t, time: t, y: world.confirmed.y };
  const frame = `data: ${JSON.stringify(world)}\n\n`;
  for (const res of clients) res.write(frame);
}, 2000);

createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');

  if (req.url === '/api/v1/worlds') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify([...worlds.values()]));
    return;
  }

  if (req.url === '/api/v1/worlds/stream') {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      Connection: 'keep-alive',
    });
    res.write(': heartbeat\n\n');
    clients.add(res);
    const beat = setInterval(() => res.write(': heartbeat\n\n'), 5000);
    req.on('close', () => {
      clearInterval(beat);
      clients.delete(res);
    });
    return;
  }

  res.writeHead(404).end();
}).listen(PORT, () => console.log(`mock API on http://localhost:${PORT}/api/v1`));

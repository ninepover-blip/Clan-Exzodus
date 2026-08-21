import { json, sql } from '../lib/core.js';

async function one(text, fallback = 0) {
  try {
    const r = await sql.unsafe(text);
    const row = r[0];
    if (!row) return fallback;
    const v = row.n ?? Object.values(row)[0];
    return v == null ? fallback : Number(v);
  } catch (e) {
    console.error('stats one err:', e.message);
    return fallback;
  }
}
async function rows(text, fallback = []) {
  try { return await sql.unsafe(text); } catch (e) { console.error('stats rows err:', e.message); return fallback; }
}

export default async function handler(req, res) {
  if (req.method !== 'GET') return json(res, 405, { message: 'GET required' });

  const [
    users, downloads, keys, keysInf, keysLob, serverTotal, serverTop, online, launchesInf, launchesLob, launchesTotal
  ] = await Promise.all([
    one(`select count(*)::int as n from users where role='user'`),
    one(`select count(*)::int as n from download_events`),
    one(`select count(*)::int as n from licenses`),
    one(`select count(*)::int as n from licenses where software='infinity'`),
    one(`select count(*)::int as n from licenses where software='lobok'`),
    one(`select count(*)::int as n from server_joins`),
    rows(`select server_address as server, join_count as joins from server_joins order by join_count desc, server_address limit 10`),
    one(`select count(*)::int as n from telemetry_sessions where last_seen>now()-interval '5 minutes'`),
    one(`select count(*)::int as n from launch_events where software='infinity'`),
    one(`select count(*)::int as n from launch_events where software='lobok'`),
    one(`select count(*)::int as n from launch_events`),
  ]);

  const result = {
    registrations: users,
    downloads,
    keys,
    keysBySoftware: { infinity: keysInf, lobok: keysLob },
    serversTotal: serverTotal,
    serversTop: serverTop,
    online,
    launchesTotal,
    bySoftware: {
      infinity: { keys: keysInf, launches: launchesInf },
      lobok: { keys: keysLob, launches: launchesLob },
    },
  };

  res.setHeader('Cache-Control', 'public, max-age=10, s-maxage=10');
  return json(res, 200, result);
}

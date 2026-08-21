import { json, method, sql, requireActor } from '../lib/core.js';

const STATEMENTS = [
  { label: 'telemetry_sessions', q: sql`create table if not exists telemetry_sessions (
     user_id uuid primary key,
     nickname text not null,
     server_address text not null,
     last_seen timestamptz not null default now()
   )` },
  { label: 'download_events', q: sql`create table if not exists download_events (
     id bigserial primary key,
     kind text not null,
     created_at timestamptz not null default now()
   )` },
  { label: 'licenses.software', q: sql`alter table licenses add column if not exists software text check (software is null or software in ('infinity','lobok'))` },
  { label: 'orders.software', q: sql`alter table orders add column if not exists software text check (software is null or software in ('infinity','lobok'))` },
  { label: 'releases.software', q: sql`alter table releases add column if not exists software text not null default 'infinity' check (software in ('infinity','lobok'))` },
  { label: 'drop old index', q: sql`drop index if exists releases_kind_channel_version` },
  { label: 'releases unique', q: sql`create unique index if not exists releases_kind_channel_software_version on releases(kind, channel, software, version)` },
  { label: 'server_joins', q: sql`create table if not exists server_joins (
     server_address text primary key,
     join_count bigint not null default 0,
     first_joined timestamptz,
     last_joined timestamptz
   )` },
  { label: 'launch_events', q: sql`create table if not exists launch_events (
     id bigserial primary key,
     software text not null,
     server_address text,
     created_at timestamptz not null default now()
   )` },
];

export default async function handler(req, res) {
  if (!method(req, res)) return;
  const admin = await requireActor(req, res, true); if (!admin) return;
  const results = [];
  for (const s of STATEMENTS) {
    try { await s.q; results.push('OK: ' + s.label); }
    catch (e) { results.push('ERR: ' + s.label + ' -> ' + e.message); }
  }
  return json(res, 200, { done: results.length, results });
}

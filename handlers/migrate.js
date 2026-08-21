import {json,method,sql,requireActor} from '../lib/core.js';

// Идемпотентная миграция схемы. Запускается один раз админом (/api/migrate).
const STATEMENTS = [
  `alter table licenses add column if not exists software text check (software is null or software in ('infinity','lobok'))`,
  `alter table orders add column if not exists software text check (software is null or software in ('infinity','lobok'))`,
  `alter table releases add column if not exists software text not null default 'infinity' check (software in ('infinity','lobok'))`,
  `drop index if exists releases_kind_channel_version`,
  `create unique index if not exists releases_kind_channel_software_version on releases(kind, channel, software, version)`,

  `create table if not exists server_joins (
     server_address text primary key,
     join_count bigint not null default 0,
     first_joined timestamptz,
     last_joined timestamptz
   )`,

  `create table if not exists launch_events (
     id bigserial primary key,
     software text not null,
     server_address text,
     created_at timestamptz not null default now()
   )`,
];

export default async function handler(req,res){
  if(!method(req,res))return;
  const admin=await requireActor(req,res,true); if(!admin)return;
  const results=[];
  for(const sqlText of STATEMENTS){
    try { await sql.unsafe(sqlText); results.push('OK: '+sqlText.slice(0,60)); }
    catch(e){ results.push('ERR: '+sqlText.slice(0,60)+' -> '+e.message); }
  }
  return json(res,200,{done:results.length,results});
}

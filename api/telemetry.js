import {json,sql,sha256,validSoftware} from '../lib/core.js';

export default async function handler(req,res){
  if(req.method!=='POST')return json(res,405,{message:'POST required'});
  const b=req.body||{};
  const action=String(b.action||'join');
  const key=String(b.key||'').trim().toUpperCase();
  const nickname=String(b.nickname||'').slice(0,32);
  const server=String(b.server||'Unknown').slice(0,180);
  const software=validSoftware(b.software)?b.software:'infinity';
  if(!key)return json(res,400,{message:'Key required'});
  const rows=await sql`select user_id from licenses where key_hash=${sha256(key)} and revoked=false and (expires_at is null or expires_at>now()) limit 1`;
  if(!rows[0])return json(res,403,{message:'Invalid license'});

  if(action==='leave'){
    await sql`delete from telemetry_sessions where user_id=${rows[0].user_id}`;
    return json(res,200,{ok:true});
  }

  // join (по умолчанию): лёгкая телеметрия — один событие при заходе на сервер.
  await sql`insert into telemetry_sessions(user_id,nickname,server_address,last_seen) values(${rows[0].user_id},${nickname},${server},now()) on conflict(user_id) do update set nickname=excluded.nickname,server_address=excluded.server_address,last_seen=now()`;
  await sql`insert into launch_events(software,server_address) values(${software},${server})`;
  await sql`insert into server_joins(server_address,join_count,first_joined,last_joined) values(${server},1,now(),now())
            on conflict(server_address) do update set join_count=server_joins.join_count+1,last_joined=now()`;
  return json(res,200,{ok:true});
}

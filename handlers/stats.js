import {json,sql} from '../lib/core.js';

export default async function handler(req,res){
  if(req.method!=='GET')return json(res,405,{message:'GET required'});

  const [
    users, downloads, keys, keysInf, keysLob, serverTotal, serverTop, online, launchesInf, launchesLob, launchesTotal
  ] = await Promise.all([
    sql`select count(*)::int as n from users where role='user'`,
    sql`select count(*)::int as n from download_events`,
    sql`select count(*)::int as n from licenses`,
    sql`select count(*)::int as n from licenses where software='infinity'`,
    sql`select count(*)::int as n from licenses where software='lobok'`,
    sql`select count(*)::int as n from server_joins`,
    sql`select server_address as server, join_count as joins from server_joins order by join_count desc, server_address limit 10`,
    sql`select count(*)::int as n from telemetry_sessions where last_seen>now()-interval '5 minutes'`,
    sql`select count(*)::int as n from launch_events where software='infinity'`,
    sql`select count(*)::int as n from launch_events where software='lobok'`,
    sql`select count(*)::int as n from launch_events`,
  ]);

  const result = {
    registrations: users[0].n,
    downloads: downloads[0].n,
    keys: keys[0].n,
    keysBySoftware: { infinity: keysInf[0].n, lobok: keysLob[0].n },
    serversTotal: serverTotal[0].n,
    serversTop: serverTop,
    online: online[0].n,
    launchesTotal: launchesTotal[0].n,
    bySoftware: {
      infinity: { keys: keysInf[0].n, launches: launchesInf[0].n },
      lobok: { keys: keysLob[0].n, launches: launchesLob[0].n },
    },
  };

  res.setHeader('Cache-Control','public, max-age=10, s-maxage=10');
  return json(res,200,result);
}

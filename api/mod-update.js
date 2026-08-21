import {json,method,sql,requireActor,validSoftware} from '../lib/core.js';
export default async function handler(req,res){
  if(!method(req,res))return; const b=req.body||{};
  const channel = b.channel === 'test' ? 'test' : 'stable';
  const software = validSoftware(b.software) ? b.software : 'infinity';

  // Манифест текущего релиза мода. Канал test доступен только админам.
  if(b.action==='manifest'){
    if(channel === 'test'){
      const admin=await requireActor(req,res,true); if(!admin)return;
    }
    const rows=await sql`select id,version,filename,sha256,size_bytes as size,chunk_count from releases where kind='mod' and channel=${channel} and software=${software} and published=true order by created_at desc limit 1`;
    return json(res,200,{release:rows[0]||null,software});
  }

  // Скачивание фрагмента релиза.
  if(b.action==='chunk'){
    const rows=await sql`select encode(data,'base64') as data from release_chunks where release_id=${b.releaseId} and chunk_index=${Number(b.index)} limit 1`;
    if(!rows[0])return json(res,404,{message:'Фрагмент не найден'}); return json(res,200,rows[0]);
  }

  // Публикация мода (админ-панель лаунчера).
  if(['begin','upload','publish'].includes(b.action)){
    const admin=await requireActor(req,res,true); if(!admin)return;

    if(b.action==='begin'){
      if(!validSoftware(b.software))return json(res,400,{message:'Укажите софт (infinity/lobok)'});
      if(!b.version||!b.filename||!b.sha256||Number(b.chunkCount)<1)return json(res,400,{message:'Некорректные данные версии'});
      const previous=await sql`select id,published from releases where kind='mod' and channel=${channel} and software=${b.software} and version=${b.version} limit 1`;
      if(previous[0]?.published)return json(res,409,{message:'Эта версия уже опубликована. Укажите новую версию.'});
      if(previous[0])await sql`delete from releases where id=${previous[0].id}`;
      const rows=await sql`insert into releases(kind,channel,version,filename,sha256,size_bytes,chunk_count,software) values('mod',${channel},${b.version},${b.filename},${b.sha256},${Number(b.size)},${Number(b.chunkCount)},${b.software}) returning id`;
      return json(res,200,{releaseId:rows[0].id});
    }

    if(b.action==='upload'){
      const data=Buffer.from(String(b.data||''),'base64'); if(data.length>2100000)return json(res,413,{message:'Фрагмент слишком большой'});
      await sql`insert into release_chunks(release_id,chunk_index,data) values(${b.releaseId},${Number(b.index)},${data}) on conflict(release_id,chunk_index) do update set data=excluded.data`;
      return json(res,200,{ok:true});
    }

    if(b.action==='publish'){
      const count=await sql`select count(*)::int as n from release_chunks where release_id=${b.releaseId}`;
      const rel=await sql`select chunk_count,kind,channel from releases where id=${b.releaseId}`;
      if(!rel[0]||count[0].n!==rel[0].chunk_count)return json(res,400,{message:'Загружены не все фрагменты'});
      // Снимаем с публикации предыдущий релиз того же канала.
      await sql`update releases set published=false where kind=${rel[0].kind} and channel=${rel[0].channel}`;
      await sql`update releases set published=true where id=${b.releaseId}`;
      // Бинарные фрагменты — крупнейшая часть БД: оставляем только текущий релиз канала.
      await sql`delete from releases where kind=${rel[0].kind} and channel=${rel[0].channel} and id<>${b.releaseId}`;
      return json(res,200,{ok:true});
    }
  }

  return json(res,400,{message:'Неизвестное действие'});
}
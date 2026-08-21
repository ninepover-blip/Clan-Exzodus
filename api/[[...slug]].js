import admin from '../handlers/admin.js';
import auth from '../handlers/auth.js';
import download from '../handlers/download.js';
import freeKey from '../handlers/free-key.js';
import launcherUpdate from '../handlers/launcher-update.js';
import migrate from '../handlers/migrate.js';
import modUpdate from '../handlers/mod-update.js';
import orders from '../handlers/orders.js';
import releases from '../handlers/releases.js';
import stats from '../handlers/stats.js';
import telegram from '../handlers/telegram.js';
import telemetry from '../handlers/telemetry.js';
import validate from '../handlers/validate.js';

const routes = {
  admin, auth, download, 'free-key': freeKey, 'launcher-update': launcherUpdate,
  migrate, 'mod-update': modUpdate, orders, releases, stats, telegram, telemetry, validate,
};

export default async function handler(req, res) {
  let path = String(req.url || '/').split('?')[0];
  if (path.startsWith('/api')) path = path.slice(4);
  if (!path.startsWith('/')) path = '/' + path;
  const seg = path.split('/').filter(Boolean)[0] || '';
  const h = routes[seg];
  if (!h) {
    res.statusCode = 404;
    res.setHeader('Content-Type', 'application/json');
    return res.end(JSON.stringify({ message: 'Not found: ' + seg }));
  }
  return h(req, res);
}

export const config = { api: { bodyParser: { sizeLimit: '3mb' } } };

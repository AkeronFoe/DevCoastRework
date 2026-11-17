// antes de correr las pruebas deja la demo en su estado inicial (si hay token) y confirma que la app responde
export default async function () {
  const base = process.env.BASE_URL || 'http://localhost:8080';
  const r = await fetch(base + '/salud').catch(() => null);
  if (!r || !r.ok) throw new Error(`La app no responde en ${base}/salud. Arráncala en modo feria (DEVCOAST_PERFIL=feria) y vuelve a intentar.`);
  const salud = await r.json();
  if (salud.perfil !== 'feria') throw new Error(`La app corre con perfil «${salud.perfil}». Estas pruebas necesitan DEVCOAST_PERFIL=feria.`);
  const token = process.env.FERIA_TOKEN;
  if (token) {
    const rr = await fetch(base + '/interno/demo/reiniciar', { method: 'POST', headers: { 'X-Feria-Token': token } });
    if (!rr.ok) throw new Error('No se pudo reiniciar la demo: ' + rr.status + ' ' + (await rr.text()));
  } else {
    console.warn('FERIA_TOKEN no está definido: las pruebas corren sobre el estado actual de la demo.');
  }
}

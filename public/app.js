export function esc(texto) {
  const d = document.createElement('div');
  d.textContent = texto ?? '';
  return d.innerHTML;
}

export function formatearFechaHora(fecha, horario) {
  const f = new Date(fecha + 'T' + horario).toLocaleDateString('es-AR', { dateStyle: 'full' });
  return f + ', ' + horario.slice(0, 5) + ' hs';
}

/** Llama a la API; si falla lanza un Error con el mensaje del backend (y los campos inválidos, si hay). */
export async function api(url, opciones = {}, auth = null) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth) headers.Authorization = 'Basic ' + auth;
  const resp = await fetch(url, { ...opciones, headers });
  const datos = await resp.json().catch(() => ({}));
  if (!resp.ok) {
    const error = new Error(datos.mensaje || 'Algo salió mal, intente nuevamente');
    error.status = resp.status;
    error.campos = datos.campos || {};
    throw error;
  }
  return datos;
}

export function basicAuth(usuario, clave) {
  return btoa(String.fromCharCode(...new TextEncoder().encode(usuario + ':' + clave)));
}

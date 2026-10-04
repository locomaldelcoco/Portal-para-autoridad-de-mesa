// Utilidades compartidas por index.html y admin.html

/** Escapa texto antes de insertarlo como HTML (evita XSS con datos cargados por usuarios). */
export function esc(texto) {
  const d = document.createElement('div');
  d.textContent = texto ?? '';
  return d.innerHTML;
}

export function formatearFecha(iso) {
  return new Date(iso).toLocaleString('es-AR', { dateStyle: 'full', timeStyle: 'short' });
}

/** Llama a la API y lanza un Error con el mensaje del backend si algo falla. */
export async function api(url, opciones = {}, auth = null) {
  const headers = { 'Content-Type': 'application/json', ...(opciones.headers || {}) };
  if (auth) headers.Authorization = 'Basic ' + auth;
  const resp = await fetch(url, { ...opciones, headers });
  if (resp.status === 204) return null;
  const datos = await resp.json().catch(() => ({}));
  if (!resp.ok) {
    const error = new Error(datos.mensaje || 'Error inesperado (' + resp.status + ')');
    error.status = resp.status;
    error.campos = datos.campos || {};
    throw error;
  }
  return datos;
}

/** Codifica usuario:clave en Base64 soportando caracteres no ASCII. */
export function basicAuth(usuario, clave) {
  const bytes = new TextEncoder().encode(usuario + ':' + clave);
  return btoa(String.fromCharCode(...bytes));
}

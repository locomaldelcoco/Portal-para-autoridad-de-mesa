const URL_USIG = 'https://servicios.usig.buenosaires.gob.ar/normalizar/';

/**
 * Pide al normalizador de direcciones de la USIG las direcciones que coinciden con lo escrito
 * y devuelve [{direccion, localidad}] para que el administrador elija la correcta.
 * `fetchFn` se puede reemplazar en los tests.
 */
export async function buscarDirecciones(texto, fetchFn = fetch) {
  const url = new URL(URL_USIG);
  url.search = new URLSearchParams({ direccion: texto, geocodificar: 'true', srid: '4326', maxOptions: '8' });
  const r = await fetchFn(url, { signal: AbortSignal.timeout(5000) });
  if (!r.ok) throw new Error(`La USIG respondió ${r.status}`);
  const datos = await r.json();
  return (datos.direccionesNormalizadas ?? [])
    .filter((d) => typeof d.direccion === 'string' && d.direccion)
    .map((d) => ({ direccion: d.direccion, localidad: d.nombre_localidad ?? d.nombre_partido ?? null }));
}

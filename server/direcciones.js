const URL_USIG = 'https://servicios.usig.buenosaires.gob.ar/normalizar/';

/** Convierte las coordenadas de la USIG (x = longitud, y = latitud, SRID 4326); null si no son plausibles para Argentina. */
function aCoordenadas(c) {
  const lng = Number(c?.x);
  const lat = Number(c?.y);
  const valido = Number.isFinite(lat) && Number.isFinite(lng) && lat >= -56 && lat <= -21 && lng >= -74 && lng <= -53;
  return valido ? { lat, lng } : { lat: null, lng: null };
}

/**
 * Cliente de la API de normalización de direcciones de la USIG.
 * `fetchFn` se puede reemplazar en los tests.
 */
export function crearServicioDirecciones(fetchFn = fetch) {
  async function normalizar(texto, maxOptions) {
    const url = new URL(URL_USIG);
    url.search = new URLSearchParams({ direccion: texto, geocodificar: 'true', srid: '4326', maxOptions: String(maxOptions) });
    const r = await fetchFn(url, { signal: AbortSignal.timeout(5000) });
    if (!r.ok) throw new Error(`La USIG respondió ${r.status}`);
    const datos = await r.json();
    return (datos.direccionesNormalizadas ?? [])
      .filter((d) => typeof d.direccion === 'string' && d.direccion)
      .map((d) => ({
        direccion: d.direccion,
        localidad: d.nombre_localidad ?? d.nombre_partido ?? null,
        ...aCoordenadas(d.coordenadas),
      }));
  }

  return {
    /** Opciones para elegir mientras se escribe una dirección. */
    buscar: (texto) => normalizar(texto, 8),

    /** Ubicación (dirección normalizada + coordenadas) de una dirección, o null si la USIG no la encuentra. */
    async ubicar(texto) {
      const opciones = await normalizar(texto, 5);
      return opciones.find((o) => o.lat !== null) ?? null;
    },
  };
}

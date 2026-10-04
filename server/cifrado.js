import crypto from 'node:crypto';

/**
 * RNF-04. AES-256-GCM para cifrar datos sensibles en la base, y HMAC-SHA256 para
 * poder detectar un DNI repetido sin descifrar nada.
 */
export function crearCifrado(claveBase64) {
  const maestra = Buffer.from(claveBase64, 'base64');
  if (maestra.length !== 32) throw new Error('La clave de cifrado debe ser de 32 bytes en Base64');
  // Dos subclaves distintas derivadas de la maestra: una para cifrar y otra para el hash
  const derivar = (etiqueta) => crypto.createHmac('sha256', maestra).update(etiqueta).digest();
  const claveAes = derivar('cifrado');
  const claveHash = derivar('hash');

  return {
    cifrar(texto) {
      const iv = crypto.randomBytes(12);
      const c = crypto.createCipheriv('aes-256-gcm', claveAes, iv);
      const cifrado = Buffer.concat([c.update(texto, 'utf8'), c.final()]);
      return Buffer.concat([iv, cifrado, c.getAuthTag()]).toString('base64');
    },
    descifrar(base64) {
      const b = Buffer.from(base64, 'base64');
      const d = crypto.createDecipheriv('aes-256-gcm', claveAes, b.subarray(0, 12));
      d.setAuthTag(b.subarray(b.length - 16));
      return Buffer.concat([d.update(b.subarray(12, b.length - 16)), d.final()]).toString('utf8');
    },
    hash(texto) {
      return crypto.createHmac('sha256', claveHash).update(texto).digest('hex');
    },
  };
}

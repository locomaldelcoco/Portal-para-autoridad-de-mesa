/** Error con código HTTP; `campos` indica qué dato es inválido ({campo: mensaje}). */
export class HttpError extends Error {
  constructor(status, mensaje, campos) {
    super(mensaje);
    this.status = status;
    this.campos = campos;
  }
}

export const datoInvalido = (campo, mensaje) => new HttpError(400, mensaje, { [campo]: mensaje });

package ar.portal.autoridadmesa.seguridad;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Cifra/descifra automáticamente un String al guardarlo/leerlo de la base (se usa con @Convert). */
@Converter
public class CampoCifrado implements AttributeConverter<String, String> {

    private final CifradoService cifrado;

    public CampoCifrado(CifradoService cifrado) { this.cifrado = cifrado; }

    @Override
    public String convertToDatabaseColumn(String valor) { return valor == null ? null : cifrado.cifrar(valor); }

    @Override
    public String convertToEntityAttribute(String guardado) { return guardado == null ? null : cifrado.descifrar(guardado); }
}

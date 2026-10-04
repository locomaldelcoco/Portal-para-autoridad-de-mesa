package ar.portal.autoridadmesa.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** AES-256-GCM para cifrar datos sensibles en la base, y HMAC-SHA256 para poder buscar por DNI sin descifrar. */
@Service
public class CifradoService {

    private static final int IV_BYTES = 12;
    private final SecretKeySpec claveAes;
    private final SecretKeySpec claveHash;
    private final SecureRandom azar = new SecureRandom();

    public CifradoService(@Value("${portal.cifrado.clave}") String claveBase64) {
        byte[] maestra = Base64.getDecoder().decode(claveBase64);
        if (maestra.length != 32) {
            throw new IllegalStateException("portal.cifrado.clave debe ser de 32 bytes en Base64");
        }
        // Dos subclaves distintas derivadas de la maestra: una para cifrar y otra para el hash
        this.claveAes = new SecretKeySpec(hmac(maestra, "cifrado"), "AES");
        this.claveHash = new SecretKeySpec(hmac(maestra, "hash"), "HmacSHA256");
    }

    public String cifrar(String texto) {
        try {
            byte[] iv = new byte[IV_BYTES];
            azar.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, claveAes, new GCMParameterSpec(128, iv));
            byte[] cifrado = c.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] salida = new byte[IV_BYTES + cifrado.length];
            System.arraycopy(iv, 0, salida, 0, IV_BYTES);
            System.arraycopy(cifrado, 0, salida, IV_BYTES, cifrado.length);
            return Base64.getEncoder().encodeToString(salida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar", e);
        }
    }

    public String descifrar(String base64) {
        try {
            byte[] entrada = Base64.getDecoder().decode(base64);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, claveAes, new GCMParameterSpec(128, entrada, 0, IV_BYTES));
            return new String(c.doFinal(entrada, IV_BYTES, entrada.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar", e);
        }
    }

    public String hash(String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(claveHash);
            return HexFormat.of().formatHex(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmac(byte[] clave, String etiqueta) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return mac.doFinal(etiqueta.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}

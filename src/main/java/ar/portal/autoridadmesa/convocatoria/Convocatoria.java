package ar.portal.autoridadmesa.convocatoria;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Fila única con lo que hay que recordar de la convocatoria entre ejecuciones. */
@Entity
public class Convocatoria {

    static final long ID = 1L;

    @Id private Long id = ID;
    private boolean cierreProcesado;      // CU1 ya se ejecutó
    private boolean reporteFinalEncolado; // CU8 ya se ejecutó

    public boolean isCierreProcesado() { return cierreProcesado; }
    public void setCierreProcesado(boolean v) { this.cierreProcesado = v; }
    public boolean isReporteFinalEncolado() { return reporteFinalEncolado; }
    public void setReporteFinalEncolado(boolean v) { this.reporteFinalEncolado = v; }
}

package ar.portal.autoridadmesa.convocatoria;

import java.time.LocalDateTime;

public record ConvocatoriaDto(EstadoConvocatoria estado, LocalDateTime apertura, LocalDateTime cierre) {}

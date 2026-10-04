package ar.portal.autoridadmesa.charla;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Charla de orientación (RF-01). */
@Entity
public class Charla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String nombre;
    @Column(nullable = false) private String tema;
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false) private LocalTime horario;
    @Column(nullable = false) private String sedeNombre;
    @Column(nullable = false) private String sedeDireccion;

    public LocalDateTime fechaHora() { return LocalDateTime.of(fecha, horario); }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }
    public String getTema() { return tema; }
    public void setTema(String v) { this.tema = v; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate v) { this.fecha = v; }
    public LocalTime getHorario() { return horario; }
    public void setHorario(LocalTime v) { this.horario = v; }
    public String getSedeNombre() { return sedeNombre; }
    public void setSedeNombre(String v) { this.sedeNombre = v; }
    public String getSedeDireccion() { return sedeDireccion; }
    public void setSedeDireccion(String v) { this.sedeDireccion = v; }
}

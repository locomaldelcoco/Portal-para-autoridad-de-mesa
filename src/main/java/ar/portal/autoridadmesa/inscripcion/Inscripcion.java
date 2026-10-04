package ar.portal.autoridadmesa.inscripcion;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Ciudadano que se postula para ser autoridad de mesa. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "dni"))
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String nombre;
    @Column(nullable = false) private String apellido;
    @Column(nullable = false, length = 8) private String dni;
    @Column(nullable = false) private LocalDate fechaNacimiento;
    @Column(nullable = false) private String email;
    @Column(nullable = false) private String telefono;
    @Column(nullable = false) private String domicilio;
    @Column(nullable = false) private String localidad;
    private boolean aceptaTerminos;
    @Column(nullable = false) private LocalDateTime fechaInscripcion = LocalDateTime.now();

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }
    public String getApellido() { return apellido; }
    public void setApellido(String v) { this.apellido = v; }
    public String getDni() { return dni; }
    public void setDni(String v) { this.dni = v; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate v) { this.fechaNacimiento = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String v) { this.telefono = v; }
    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String v) { this.domicilio = v; }
    public String getLocalidad() { return localidad; }
    public void setLocalidad(String v) { this.localidad = v; }
    public boolean isAceptaTerminos() { return aceptaTerminos; }
    public void setAceptaTerminos(boolean v) { this.aceptaTerminos = v; }
    public LocalDateTime getFechaInscripcion() { return fechaInscripcion; }
}

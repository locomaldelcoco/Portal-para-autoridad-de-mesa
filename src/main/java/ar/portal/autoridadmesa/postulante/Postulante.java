package ar.portal.autoridadmesa.postulante;

import ar.portal.autoridadmesa.seguridad.CampoCifrado;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Ciudadano postulado como autoridad de mesa (RF-05). DNI, domicilio y partido se guardan cifrados (RNF-04). */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "dniHash"))
public class Postulante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String distritoElectoral;
    @Column(nullable = false) private String nombre;
    @Column(nullable = false) private String apellido;
    @Convert(converter = CampoCifrado.class) @Column(nullable = false, length = 1024) private String dni;
    @Column(nullable = false, length = 64) private String dniHash; // para detectar DNI repetidos sin descifrar
    @Column(nullable = false) private LocalDate fechaNacimiento;
    @Convert(converter = CampoCifrado.class) @Column(nullable = false, length = 1024) private String domicilio;
    @Column(nullable = false) private String telefono;
    @Column(nullable = false) private String email;
    private boolean antecedentesMesa;
    @Column(nullable = false) private String estadoCapacitacion;
    private boolean afiliado;
    @Convert(converter = CampoCifrado.class) @Column(length = 1024) private String partido;
    private Long charlaInteresId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoPostulante estado = EstadoPostulante.PENDIENTE;
    @Column(length = 1000) private String motivoRechazo;
    @Column(nullable = false) private LocalDateTime fechaRegistro;

    public Long getId() { return id; }
    public String getDistritoElectoral() { return distritoElectoral; }
    public void setDistritoElectoral(String v) { this.distritoElectoral = v; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }
    public String getApellido() { return apellido; }
    public void setApellido(String v) { this.apellido = v; }
    public String getDni() { return dni; }
    public void setDni(String v) { this.dni = v; }
    public String getDniHash() { return dniHash; }
    public void setDniHash(String v) { this.dniHash = v; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate v) { this.fechaNacimiento = v; }
    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String v) { this.domicilio = v; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String v) { this.telefono = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public boolean isAntecedentesMesa() { return antecedentesMesa; }
    public void setAntecedentesMesa(boolean v) { this.antecedentesMesa = v; }
    public String getEstadoCapacitacion() { return estadoCapacitacion; }
    public void setEstadoCapacitacion(String v) { this.estadoCapacitacion = v; }
    public boolean isAfiliado() { return afiliado; }
    public void setAfiliado(boolean v) { this.afiliado = v; }
    public String getPartido() { return partido; }
    public void setPartido(String v) { this.partido = v; }
    public Long getCharlaInteresId() { return charlaInteresId; }
    public void setCharlaInteresId(Long v) { this.charlaInteresId = v; }
    public EstadoPostulante getEstado() { return estado; }
    public void setEstado(EstadoPostulante v) { this.estado = v; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String v) { this.motivoRechazo = v; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime v) { this.fechaRegistro = v; }
}

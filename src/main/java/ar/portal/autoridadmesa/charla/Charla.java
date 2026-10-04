package ar.portal.autoridadmesa.charla;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Una charla informativa: tiene fecha, ubicación y una lista de temas. */
@Entity
public class Charla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 2000)
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private String lugar;      // ej: "Biblioteca Municipal"

    @Column(nullable = false)
    private String direccion;  // ej: "San Martín 123"

    @Column(nullable = false)
    private String localidad;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "charla_temas", joinColumns = @JoinColumn(name = "charla_id"))
    @Column(name = "tema")
    private List<String> temas = new ArrayList<>();

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getLocalidad() { return localidad; }
    public void setLocalidad(String localidad) { this.localidad = localidad; }
    public List<String> getTemas() { return temas; }
    public void setTemas(List<String> temas) { this.temas = temas; }
}

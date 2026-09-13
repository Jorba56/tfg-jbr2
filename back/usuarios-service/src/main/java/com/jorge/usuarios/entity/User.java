package com.jorge.usuarios.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.MutablePropertyValues;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa a un usuario dentro del sistema.
 * Mapea la tabla "usuarios" en la base de datos y gestiona las relaciones
 * con sus roles y su historial de incidencias.
 */
@Entity
@Table(name = "usuarios")
public class User{
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long idUser;

    @NotBlank(message = "El nombre no puede estar vacío")
    @Column(name = "nombre_usuario", length = 100)
    @JsonProperty("nombre_usuario")
    private String nombreUsuario;

    @NotBlank(message = "El apellido no puede estar vacío")
    @Column(name = "apellido_usuario", length = 150)
    @JsonProperty("apellido_usuario")
    private String apellidoUsuario;

    @Column(name = "creditos")
    private int creditos = 0;

    @Column(name = "color_tema")
    private String colorTema = "default";

    @Column(name = "avatar_config", length = 255)
    private String avatarConfig;

    // Genera los Getters y Setters para creditos y colorTema
    public int getCreditos() { return creditos; }
    public void setCreditos(int creditos) { this.creditos = creditos; }

    @NotBlank(message = "La contraseña no puede estar vacía")
    @Column(name = "contrasenha_usuario", length = 255)
    @JsonProperty("contrasenha_usuario")
    private String contrasenhaUsuario;

    @NotBlank(message = "El correo no puede estar vacío")
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,8}$",message = "El formato del correo no es válido")
    @Column(name = "correo_usuario", length = 254, unique = true)
    @JsonProperty("correo_usuario")
    private String emailUsuario;

    @NotNull
    private boolean activo=true;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "roles_usuario",
    joinColumns=@JoinColumn(name="id_user"),
    inverseJoinColumns = @JoinColumn(name = "id_rol"))
    private List<Rol> roles= new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER) // EAGER para que carguen siempre con el usuario
    @JoinTable(
            name = "usuario_items",
            joinColumns = @JoinColumn(name = "id_user"),
            inverseJoinColumns = @JoinColumn(name = "id_item")
    )
    private List<Item> inventario = new ArrayList<>();

    @Column(name = "rosco_palabras_jugadas", columnDefinition = "integer default 0")
    private Integer roscoPalabrasJugadas = 0;

    @Column(name = "mejor_puntuacion_rosco", columnDefinition = "integer default 0")
    private Integer mejorPuntuacionRosco = 0;

    // estadísticas contrarreloj (frases)
    @Column(name = "frases_acertadas", columnDefinition = "integer default 0")
    private Integer frasesAcertadas = 0;

    @Column(name = "frases_jugadas", columnDefinition = "integer default 0")
    private Integer frasesJugadas = 0;

    @Column(name = "mejor_puntuacion_contrarreloj", columnDefinition = "integer default 0")
    private Integer mejorPuntuacionContrarreloj = 0;



    // Getters and setters
    @JsonIgnore
    public Long getIdUser() { return idUser; }
    @JsonIgnore
    public void setIdUser(Long idUser) { this.idUser= idUser; }

    public List<Item> getInventario() { return inventario; }
    public void setInventario(List<Item> inventario) { this.inventario = inventario; }

    public String getAvatarConfig() { return avatarConfig; }
    public void setAvatarConfig(String avatarConfig) { this.avatarConfig = avatarConfig; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getEmailUsuario() { return emailUsuario; }
    public void setEmailUsuario(String emailUsuario) { this.emailUsuario =emailUsuario; }

    public String getApellidoUsuario() { return apellidoUsuario; }
    public void setApellidoUsuario(String apellidoUsuario) { this.apellidoUsuario = apellidoUsuario; }

    public String getContrasenhaUsuario() { return contrasenhaUsuario; }
    public void setContrasenhaUsuario(String contrasenhaUsuario) { this.contrasenhaUsuario = contrasenhaUsuario; }

    public boolean getActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo= activo; }

    public Integer getRoscoPalabrasJugadas() {return roscoPalabrasJugadas;}

    public void setRoscoPalabrasJugadas(Integer roscoPalabrasJugadas) {
        this.roscoPalabrasJugadas = roscoPalabrasJugadas;
    }

    public Integer getMejorPuntuacionRosco() {
        return mejorPuntuacionRosco;
    }

    public void setMejorPuntuacionRosco(Integer mejorPuntuacionRosco) {
        this.mejorPuntuacionRosco = mejorPuntuacionRosco;
    }

    public Integer getFrasesAcertadas() {
        return frasesAcertadas;
    }

    public void setFrasesAcertadas(Integer frasesAcertadas) {
        this.frasesAcertadas = frasesAcertadas;
    }

    public Integer getFrasesJugadas() {
        return frasesJugadas;
    }

    public void setFrasesJugadas(Integer frasesJugadas) {
        this.frasesJugadas = frasesJugadas;
    }

    public Integer getMejorPuntuacionContrarreloj() {
        return mejorPuntuacionContrarreloj;
    }

    public void setMejorPuntuacionContrarreloj(Integer mejorPuntuacionContrarreloj) {
        this.mejorPuntuacionContrarreloj = mejorPuntuacionContrarreloj;
    }
    @JsonIgnore
    public List<Rol> getRoles() {
        return roles;
    }
    @JsonIgnore
    public void setRoles(List<Rol> roles) {
        this.roles = roles;
    }

    //el "Getter Falso": Jackson lee esto y crea la clave "rol_id" automáticamente
    @JsonIgnore
    public List<Long> getRolId() {
        List<Long> rolesFinal=new ArrayList<>();
        if (this.roles != null) {
            for(int i=0; i<roles.size();i++){
                rolesFinal.add((roles.get(i)).getIdRol());
            }
            return rolesFinal;
        }
        return rolesFinal; // Si el usuario aún no tiene rol, devolverá null en vez de dar error
    }
    // Jackson usará esto
    @JsonIgnore
    public void setRolId(List<Long> ids)  {
        if (ids != null) {
            this.roles = new ArrayList<>();
            for (Long id : ids) {
                Rol nuevoRol = new Rol();
                nuevoRol.setIdRol(id);
                this.roles.add(nuevoRol);
            }
        } else {
            this.roles = null;
        }
    }

}






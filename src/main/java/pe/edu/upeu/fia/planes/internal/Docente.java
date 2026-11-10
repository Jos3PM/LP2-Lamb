package pe.edu.upeu.fia.planes.internal;
import jakarta.persistence.*;
@Entity 
@Table(name="FIA_DOCENTE_FUENTE") 
class Docente {
    @Id Long id; @Column(length=150) String school; @Column(length=250) String name; @Column(length=500) String course;
    protected Docente() {}
}

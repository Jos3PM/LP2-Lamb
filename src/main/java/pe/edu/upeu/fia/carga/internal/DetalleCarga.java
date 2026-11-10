package pe.edu.upeu.fia.carga.internal;
import jakarta.persistence.*;
import java.util.*;

@Entity 
@Table(name="FIA_CARGA_DETALLE",uniqueConstraints=@UniqueConstraint(name="UK_CARGA_CURSO_GRUPO",columnNames={"carga_id","cursoId","grupo"})) 
class DetalleCarga {
 @Id 
 @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="detalle_seq") 
 @SequenceGenerator(name="detalle_seq",sequenceName="FIA_DETALLE_SEQ",allocationSize=1) Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) 
 @JoinColumn(name="carga_id") Carga carga;
 @Column(nullable=false) Long cursoId; Long docenteId; 
 @Column(nullable=false) int grupo; Integer cupos;
 @Column(length=200) String ambiente; 
 @Column(length=60) String condicion; 
 @Column(length=60) String modalidad;
 @Column(length=300) String cursoNombre; Integer ciclo; Integer creditos; Integer ht; Integer hp; Integer hnp;
 int horas; 
 @Column(length=250) String docenteNombre;
 @ElementCollection @CollectionTable(name="FIA_HORARIO",joinColumns=@JoinColumn(name="detalle_id")) List<Bloque> bloques=new ArrayList<>();
 protected DetalleCarga(){}
}

package pe.edu.upeu.fia.carga.internal;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity 
@Table(name="FIA_CARGA",indexes=@Index(name="IX_CARGA_FECHA",columnList="fecha")) 
class Carga {
 @Id 
 @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="carga_seq") 
 @SequenceGenerator(name="carga_seq",sequenceName="FIA_CARGA_SEQ",allocationSize=1) Long id;
 @Column(nullable=false) Long planId; 
 @Column(nullable=false,length=50) String sede; 
 @Column(nullable=false,length=30) String semestre;
 @Column(nullable=false,length=80) String contrato; @Column(nullable=false,length=20) String estado; 
 @Column(nullable=false) LocalDateTime fecha; int totalHoras;
 @Column(name="creado_por",length=80) String creadoPor;
 @Column(name="modificado_por",length=80) String modificadoPor;
 @OneToMany(mappedBy="carga",cascade=CascadeType.ALL,orphanRemoval=true) List<DetalleCarga> detalles=new ArrayList<>();
 protected Carga(){}
}

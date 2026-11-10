package pe.edu.upeu.fia.planes.internal;
import jakarta.persistence.*;
@Entity 
@Table(name="FIA_CURSO",indexes=@Index(name="IX_CURSO_PLAN_CICLO",columnList="plan_id,cycle")) 
public class Curso {
    @Id 
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="curso_seq") 
    @SequenceGenerator(name="curso_seq",sequenceName="FIA_CURSO_SEQ",allocationSize=1) Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="plan_id") Plan plan;
    @Column(nullable=false) int cycle; @Column(nullable=false,length=300) String name;
    
    int credits; int ht; int hp; int hnp; int total;
    @Column(length=300) String horario;
    int cupos = 30;
    @Column(length=3000) String prerequisites; 
    @Column(length=1500) String notes; @Column(length=300) String source; int page;
    protected Curso() {}
}


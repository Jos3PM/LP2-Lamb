package pe.edu.upeu.fia.planes.internal;
import jakarta.persistence.*;
@Entity 
@Table(name="FIA_PLAN") 
class Plan {
    @Id Long id; 
    @Column(nullable=false,length=150) String school;
    @Column(name="version_plan",nullable=false,length=20) String version;
    @Column(nullable=false,length=30) String variant; 
    @Column(length=300) String source;
    protected Plan() {}
}

package pe.edu.upeu.fia.planes.internal;
import pe.edu.upeu.fia.planes.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;
import java.text.Normalizer;
@Service 
@Transactional(readOnly=true)
class PlanesService implements PlanesApi {
 private final PlanRepository plans; private final CursoRepository courses; private final DocenteRepository teachers; private final HorariosService horarios;
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
 public void bloquearCursos(List<Long> ids){ids.stream().sorted().forEach(id->{
  if(entityManager.find(Curso.class,id,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)==null)
   throw new NoSuchElementException("Curso no encontrado");
 });}
 PlanesService(PlanRepository p,CursoRepository c,DocenteRepository t,HorariosService h){plans=p;courses=c;teachers=t;horarios=h;}
 static String norm(String s){return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim();}
 PlanDto dto(Plan p){p=org.hibernate.Hibernate.unproxy(p,Plan.class);return new PlanDto(p.id,p.school,p.version,p.variant,p.source);}
 DocenteDto dto(Docente d){return new DocenteDto(d.id,d.name,d.course,d.school);}
 CursoDto dto(Curso c,List<Docente> ts){Plan p=org.hibernate.Hibernate.unproxy(c.plan,Plan.class);return new CursoDto(c.id,p.id,p.school,c.cycle,c.name,c.credits,c.ht,c.hp,c.hnp,c.total,c.prerequisites,c.notes,c.source,c.page,ts.stream().filter(t->Objects.equals(t.school,p.school)&&norm(t.course==null?"":t.course).equals(norm(c.name))).map(this::dto).toList(),c.horario,c.cupos>0?c.cupos:30);}
 public List<DocenteDto> docentes(String buscar){return docentes(buscar,"","");}
 List<DocenteDto> docentes(String buscar,String escuela,String curso){
  String term=norm(buscar==null?"":buscar), school=norm(escuela==null?"":escuela), subject=norm(curso==null?"":curso);
  return teachers.findAll().stream().map(this::dto)
    .filter(d->norm(d.name()).contains(term))
    .filter(d->school.isEmpty()||norm(d.school()==null?"":d.school()).equals(school))
    .filter(d->subject.isEmpty()||norm(d.course()==null?"":d.course()).equals(subject))
    .sorted(Comparator.comparing(DocenteDto::name).thenComparing(DocenteDto::id)).toList();
 }
 public List<PlanDto> planes(){return plans.findAll(Sort.by("school","version","variant")).stream().map(this::dto).toList();}
 public PlanDto plan(Long id){return dto(plans.findById(id).orElseThrow(()->new NoSuchElementException("Plan no encontrado")));} @Transactional public PlanDto guardarPlan(Long id, PlanInput i){
  Plan p=id==null?new Plan():plans.findById(id).orElseThrow(()->new NoSuchElementException("Plan no encontrado"));
  boolean duplicado=plans.findAll().stream().anyMatch(x->!Objects.equals(x.id,id)&&norm(x.school).equals(norm(i.school()))&&norm(x.version).equals(norm(i.version()))&&norm(x.variant).equals(norm(i.variant())));
  if(duplicado)throw new IllegalArgumentException("Ya existe un plan con la misma escuela, versión y variante");
  if(id!=null&&!norm(p.school).equals(norm(i.school()))&&courses.findAll().stream().anyMatch(c->Objects.equals(org.hibernate.Hibernate.unproxy(c.plan,Plan.class).id,id)))
   throw new IllegalArgumentException("No cambies la escuela de un plan que ya tiene cursos; crea una nueva versión");
  if(id==null)p.id=plans.findAll().stream().map(x->x.id).filter(Objects::nonNull).max(Long::compareTo).orElse(0L)+1;
  p.school=i.school().trim();p.version=i.version().trim();p.variant=i.variant().trim();p.source=i.source()==null?"Registro administrativo":i.source().trim();
  return dto(plans.saveAndFlush(p));
 }
 @Transactional public void borrarPlan(Long id){if(!plans.existsById(id))throw new NoSuchElementException("Plan no encontrado");if(courses.findAll().stream().anyMatch(c->Objects.equals(org.hibernate.Hibernate.unproxy(c.plan,Plan.class).id,id)))throw new IllegalArgumentException("El plan tiene cursos; retíralos antes de eliminarlo");plans.deleteById(id);plans.flush();}
 public DocenteDto docente(Long id){return dto(teachers.findById(id).orElseThrow(()->new NoSuchElementException("Docente no encontrado")));}
 public CursoDto curso(Long id){return dto(courses.findById(id).orElseThrow(()->new NoSuchElementException("Curso no encontrado")),teachers.findAll());}
 public List<CursoDto> cursos(Long planId,Integer ciclo,String buscar,String orden){
  if(!Set.of("name","cycle","credits").contains(orden)) throw new IllegalArgumentException("Orden permitido: name, cycle o credits");
  Specification<Curso> spec=(r,q,b)->{var ps=new ArrayList<jakarta.persistence.criteria.Predicate>();
   if(planId!=null)ps.add(b.equal(r.get("plan").get("id"),planId)); if(ciclo!=null)ps.add(b.equal(r.get("cycle"),ciclo));
   if(buscar!=null&&!buscar.isBlank()) {
    String term=buscar.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
    ps.add(b.like(b.lower(r.get("name")),"%"+term+"%",'!'));
   }
   return b.and(ps.toArray(jakarta.persistence.criteria.Predicate[]::new));};
  var ts=teachers.findAll();return courses.findAll(spec,Sort.by(orden).and(Sort.by("id"))).stream().map(c->dto(c,ts)).toList();
 }
 @Transactional public CursoDto guardar(Long id,CursoInput i){
  Curso existente=id==null?null:courses.findById(id).orElseThrow(()->new NoSuchElementException("Curso no encontrado"));
  if(existente!=null&&!Objects.equals(org.hibernate.Hibernate.unproxy(existente.plan,Plan.class).id,i.planId()))throw new IllegalArgumentException("No se puede trasladar un curso a otro plan");
  boolean duplicado=courses.findAll().stream().anyMatch(other->!Objects.equals(other.id,id)&&Objects.equals(org.hibernate.Hibernate.unproxy(other.plan,Plan.class).id,i.planId())&&other.cycle==i.cycle()&&norm(other.name).equals(norm(i.name())));
  if(duplicado)throw new IllegalArgumentException("El ciclo ya contiene un curso con ese nombre");
  String pre=i.prerequisites()==null?"":i.prerequisites().trim();
  if(!pre.isBlank()&&!Set.of("ninguno","no tiene","n/a").contains(norm(pre))){
   boolean exists=courses.findAll().stream().anyMatch(previous->Objects.equals(org.hibernate.Hibernate.unproxy(previous.plan,Plan.class).id,i.planId())&&previous.cycle<i.cycle()&&norm(previous.name).equals(norm(pre)));
   if(!exists)throw new IllegalArgumentException("El prerrequisito debe ser un curso de un ciclo anterior del mismo plan");
  }
  Curso c=id==null?new Curso():existente;
  c.plan=plans.findById(i.planId()).orElseThrow(()->new NoSuchElementException("Plan no encontrado"));c.cycle=i.cycle();c.name=i.name().trim();c.credits=i.credits();c.ht=i.ht();c.hp=i.hp();c.hnp=i.hnp();c.total=Math.addExact(Math.addExact(c.ht,c.hp),c.hnp);c.prerequisites=i.prerequisites();c.cupos=i.cupos()==null?30:i.cupos();
  boolean automatico=i.horario()==null||i.horario().isBlank();
  c.horario=automatico?horarios.proponer(i.planId(),i.cycle(),i.ht()+i.hp(),id):i.horario().trim();
  horarios.validarSinCruces(i.planId(),i.cycle(),id,c.horario);
  c.source="Registro manual";c.page=0;c.notes=automatico?"Horario referencial generado automáticamente; confirmar con administración.":"Ingresado o modificado por el usuario";return dto(courses.saveAndFlush(c),teachers.findAll());
 }
 @Transactional public void borrar(Long id){if(!courses.existsById(id))throw new NoSuchElementException("Curso no encontrado");courses.deleteById(id);courses.flush();}
 @Transactional public DocenteDto guardarDocente(Long id, DocenteInput i){
  String school=i.school()==null?"":i.school().trim(), subject=i.course()==null?"":i.course().trim();
  if(school.isBlank()||subject.isBlank())throw new IllegalArgumentException("Selecciona una escuela y un curso del catálogo");
  boolean valid=courses.findAll().stream().anyMatch(c->norm(c.name).equals(norm(subject))&&norm(org.hibernate.Hibernate.unproxy(c.plan,Plan.class).school).equals(norm(school)));
  if(!valid)throw new IllegalArgumentException("El curso no pertenece a la escuela seleccionada");
  Docente d=id==null?new Docente():teachers.findById(id).orElseThrow(()->new NoSuchElementException("Docente no encontrado"));
  if(id==null)d.id=teachers.findAll().stream().map(x->x.id).filter(Objects::nonNull).max(Long::compareTo).orElse(0L)+1;
  d.name=i.name().trim();d.course=subject;d.school=school;
  return dto(teachers.saveAndFlush(d));
 }
 @Transactional public void borrarDocente(Long id){if(!teachers.existsById(id))throw new NoSuchElementException("Docente no encontrado");teachers.deleteById(id);teachers.flush();}
}



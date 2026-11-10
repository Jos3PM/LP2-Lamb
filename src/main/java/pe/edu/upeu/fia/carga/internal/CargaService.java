package pe.edu.upeu.fia.carga.internal;
import pe.edu.upeu.fia.planes.*;import pe.edu.upeu.fia.carga.CargasProfesorApi;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.data.domain.Sort;import org.springframework.data.jpa.domain.Specification;import java.util.*;import java.time.*;import java.text.Normalizer;
import static pe.edu.upeu.fia.carga.internal.CargaDtos.*;
@Service @Transactional(readOnly=true) class CargaService implements CargasProfesorApi {
 private final CargaRepository repo;private final PlanesApi planes;private final AgendaLockRepository locks;
 private final DetalleCargaRepository detalles;private final CargaAuditRepository auditoria;
 CargaService(CargaRepository r,PlanesApi p,AgendaLockRepository l,DetalleCargaRepository d,CargaAuditRepository a){repo=r;planes=p;locks=l;detalles=d;auditoria=a;}
 public List<CargasProfesorApi.Asignacion> asignaciones(Long docenteId){
  return detalles.findByDocenteIdOrderById(docenteId).stream().map(d->{
   Carga c=org.hibernate.Hibernate.unproxy(d.carga,Carga.class);
   return new CargasProfesorApi.Asignacion(c.id,d.id,c.planId,d.cursoId,d.cursoNombre==null?planes.curso(d.cursoId).name():d.cursoNombre,
    c.semestre,c.sede,d.grupo,d.ambiente,d.modalidad,d.cupos==null?0:d.cupos,d.horas,
    d.bloques.stream().map(b->new CargasProfesorApi.Bloque(b.dia,b.inicio,b.fin)).toList());
  }).toList();
 }
 private String actor(){var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();return auth==null||!auth.isAuthenticated()?"sistema":auth.getName();}
 static String key(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
 void bloquear(){if(locks.bloquear()==null)throw new IllegalStateException("Agenda no inicializada");}
 DetalleDto detalleDto(DetalleCarga d){var c=planes.curso(d.cursoId);return new DetalleDto(d.id,d.cursoId,d.docenteId,d.docenteNombre,d.grupo,d.cupos,d.ambiente,d.condicion,d.modalidad,d.horas,d.bloques.stream().map(b->new BloqueInput(b.dia,b.inicio,b.fin)).toList(),d.cursoNombre==null?c.name():d.cursoNombre,d.ciclo==null?c.cycle():d.ciclo,d.creditos==null?c.credits():d.creditos,d.ht==null?c.ht():d.ht,d.hp==null?c.hp():d.hp,d.hnp==null?c.hnp():d.hnp);}
 Dto dto(Carga c){return new Dto(c.id,c.planId,c.sede,c.semestre,c.contrato,c.estado,c.fecha,c.totalHoras,c.detalles.stream().map(this::detalleDto).toList());}
 Dto uno(Long id){return dto(repo.findById(id).orElseThrow(()->new NoSuchElementException("Carga no encontrada")));}
 record AuditDto(String accion,String actor,LocalDateTime fecha,String resumen){}
 List<AuditDto> auditoria(Long id){repo.findById(id).orElseThrow(()->new NoSuchElementException("Carga no encontrada"));return auditoria.findByCargaIdOrderByFechaDesc(id).stream().map(a->new AuditDto(a.accion,a.actor,a.fecha,a.resumen)).toList();}
 List<Dto> listar(Long planId,String semestre,LocalDate desde,LocalDate hasta){
  if(desde!=null&&hasta!=null&&desde.isAfter(hasta))throw new IllegalArgumentException("Rango de fechas inválido");
  Specification<Carga> spec=(r,q,b)->{var ps=new ArrayList<jakarta.persistence.criteria.Predicate>();if(planId!=null)ps.add(b.equal(r.get("planId"),planId));if(semestre!=null)ps.add(b.equal(r.get("semestre"),semestre));if(desde!=null)ps.add(b.greaterThanOrEqualTo(r.get("fecha"),desde.atStartOfDay()));if(hasta!=null)ps.add(b.lessThan(r.get("fecha"),hasta.plusDays(1).atStartOfDay()));return b.and(ps.toArray(jakarta.persistence.criteria.Predicate[]::new));};
  return repo.findAll(spec,Sort.by(Sort.Direction.DESC,"fecha")).stream().map(this::dto).toList();
 }
 @Transactional public Dto crear(Input i){bloquear();return guardar(null,i);}
 @Transactional public Dto actualizar(Long id,Input i){bloquear();return guardar(id,i);}
 private Dto guardar(Long id,Input i){
  planes.plan(i.planId());Carga c=id==null?new Carga():repo.findById(id).orElseThrow(()->new NoSuchElementException("Carga no encontrada"));
  int detallesAntes=c.detalles.size(),horasAntes=c.totalHoras;
  long cuposAntes=c.detalles.stream().mapToLong(d->d.cupos==null?0:d.cupos).sum();
  if(id!=null&&!c.planId.equals(i.planId()))throw new IllegalArgumentException("No se puede cambiar el plan de una carga guardada");
  c.planId=i.planId();c.sede=i.sede().trim();c.semestre=i.semestre().trim();c.contrato=i.contrato().trim();if(c.fecha==null)c.fecha=LocalDateTime.now();c.estado="REGISTRADA";
  if(id==null)c.creadoPor=actor();c.modificadoPor=actor();
  if(id!=null){c.detalles.clear();repo.flush();}c.totalHoras=0;repo.saveAndFlush(c);
  var otras=repo.findAll().stream().filter(o->!o.id.equals(c.id)&&key(o.semestre).equals(key(c.semestre))).toList();
  Map<Long,String> escuelas=new HashMap<>();planes.planes().forEach(p->escuelas.put(p.id(),key(p.school())));
  Set<String> claves=new HashSet<>();
  for(var x:i.detalles()){
   var curso=planes.curso(x.cursoId());if(!curso.planId().equals(i.planId()))throw new IllegalArgumentException("El curso no pertenece al plan de la carga");
   if(!claves.add(x.cursoId()+":"+x.grupo()))throw new IllegalArgumentException("Curso y grupo repetidos en la carga");
   DetalleCarga d=new DetalleCarga();d.carga=c;d.cursoId=x.cursoId();d.grupo=x.grupo();d.cupos=x.cupos();d.ambiente=x.ambiente()==null?"":x.ambiente().trim();d.condicion=x.condicion();d.modalidad=x.modalidad();
   d.cursoNombre=x.cursoNombre()==null?curso.name():x.cursoNombre().trim();if(d.cursoNombre.isBlank())throw new IllegalArgumentException("El nombre del curso es obligatorio");
   d.ciclo=x.ciclo()==null?curso.cycle():x.ciclo();d.creditos=x.creditos()==null?curso.credits():x.creditos();d.ht=x.ht()==null?curso.ht():x.ht();d.hp=x.hp()==null?curso.hp():x.hp();d.hnp=x.hnp()==null?curso.hnp():x.hnp();d.horas=Math.addExact(Math.addExact(d.ht,d.hp),d.hnp);
   if(x.docenteId()!=null){var docente=planes.docente(x.docenteId());d.docenteId=docente.id();d.docenteNombre=docente.name();}
   for(var b:x.bloques()){
    if(b.inicio()>=b.fin())throw new IllegalArgumentException("El fin del bloque debe ser posterior al inicio");
    if(d.bloques.stream().anyMatch(o->o.dia==b.dia()&&o.inicio<b.fin()&&b.inicio()<o.fin))throw new IllegalArgumentException("Bloques superpuestos en el detalle");
    d.bloques.add(new pe.edu.upeu.fia.carga.internal.Bloque(b.dia(),b.inicio(),b.fin()));
   }
   if(!d.bloques.isEmpty()&&d.ambiente.isBlank())throw new IllegalArgumentException("Seleccione un ambiente para el horario");
   if(!d.bloques.isEmpty()&&(d.bloques.size()!=d.ht+d.hp||d.bloques.stream().anyMatch(b->b.fin-b.inicio!=50)))throw new IllegalArgumentException("Ajuste el horario: debe contener HT + HP bloques de 50 minutos. Abra Editar para cambiar las horas y el horario juntos.");
   for(var o:c.detalles)sinCruce(d,o,escuelas);
   for(var carga:otras)for(var o:carga.detalles)sinCruce(d,o,escuelas);
   c.totalHoras=Math.addExact(c.totalHoras,d.horas);c.detalles.add(d);repo.saveAndFlush(c);
  }
  long cuposDespues=c.detalles.stream().mapToLong(d->d.cupos==null?0:d.cupos).sum();
  String resumen="detalles "+detallesAntes+"→"+c.detalles.size()+"; horas "+horasAntes+"→"+c.totalHoras+"; cupos "+cuposAntes+"→"+cuposDespues;
  auditoria.save(new CargaAudit(c.id,id==null?"CREAR":"EDITAR",actor(),resumen));
  return dto(c);
 }
 private void sinCruce(DetalleCarga a,DetalleCarga b,Map<Long,String> escuelas){
  boolean docente=a.docenteId!=null&&b.docenteId!=null&&key(a.docenteNombre).equals(key(b.docenteNombre));
  Carga ca=org.hibernate.Hibernate.unproxy(a.carga,Carga.class),cb=org.hibernate.Hibernate.unproxy(b.carga,Carga.class);
  boolean sede=key(ca.sede).equals(key(cb.sede));
  boolean ambiente=sede&&!key(a.ambiente).isEmpty()&&key(a.ambiente).equals(key(b.ambiente));
  int cicloB=b.ciclo==null?planes.curso(b.cursoId).cycle():b.ciclo;
  boolean grupo=sede&&Objects.equals(escuelas.get(ca.planId),escuelas.get(cb.planId))&&a.ciclo==cicloB&&a.grupo==b.grupo;
  if(!docente&&!ambiente&&!grupo)return;
  for(var x:a.bloques)for(var y:b.bloques)if(x.dia==y.dia&&x.inicio<y.fin&&y.inicio<x.fin){
   String motivo=docente?"docente "+a.docenteNombre:ambiente?"ambiente "+a.ambiente:"cursos del ciclo "+a.ciclo+", grupo "+a.grupo;
   throw new IllegalArgumentException("Choque de "+motivo+" con "+(b.cursoNombre==null?planes.curso(b.cursoId).name():b.cursoNombre)+" (día "+x.dia+", "+String.format("%02d:%02d",Math.max(x.inicio,y.inicio)/60,Math.max(x.inicio,y.inicio)%60)+"). No se guardó ningún cambio.");
  }
 }
 static final int[][] RANGES={{450,500},{505,555},{560,610},{620,670},{675,725},{730,780},{790,840},{845,895},{900,950},{960,1010},{1015,1065},{1070,1120},{1125,1175},{1180,1230},{1235,1285},{1290,1340}};
 @Transactional public List<Dto> generar(){
  bloquear();Map<String,PlanDto> latest=new TreeMap<>();
  for(var p:planes.planes())if(p.variant().equals("Regular"))latest.merge(p.school(),p,(a,b)->a.version().compareTo(b.version())>=0?a:b);
  List<Dto> result=new ArrayList<>();int carrera=0;
  for(var p:latest.values()){
   carrera++;var existing=repo.findAll().stream().filter(c->c.planId.equals(p.id())&&c.semestre.equals("Regular 2026-2")&&c.sede.equals("Filial Juliaca")).findFirst();
   if(existing.isPresent()){result.add(dto(existing.get()));continue;}
   List<DetalleInput> ds=new ArrayList<>();Map<Integer,Integer> cursores=new HashMap<>();
   for(var c:planes.cursos(p.id(),null,null,"cycle")){
    List<BloqueInput> bs=new ArrayList<>();int pos=cursores.getOrDefault(c.cycle(),0);
    for(int h=0;h<c.ht()+c.hp();h++,pos++){int dia=1+(pos/2)%5;int slot=(pos/10)*2+pos%2;if(slot>=RANGES.length)throw new IllegalArgumentException("No hay espacio semanal para el ciclo "+c.cycle());bs.add(new BloqueInput(dia,RANGES[slot][0],RANGES[slot][1]));}
    cursores.put(c.cycle(),pos);ds.add(new DetalleInput(c.id(),null,1,null,String.format("FIA-%02d-C%02d",carrera,c.cycle()),"","Teórico-Práctico",bs));
   }
   result.add(guardar(null,new Input(p.id(),"Filial Juliaca","Regular 2026-2","Escuela Profesional Pregrado",ds)));
  }return result;
 }
}

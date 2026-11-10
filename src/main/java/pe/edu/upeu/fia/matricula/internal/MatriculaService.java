package pe.edu.upeu.fia.matricula.internal;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.fia.planes.PlanesApi;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;
import static pe.edu.upeu.fia.matricula.internal.MatriculaDtos.*;

@Service @Transactional(readOnly=true)
public class MatriculaService implements pe.edu.upeu.fia.matricula.MatriculaAlumnoApi {
 private final EstudianteRepo estudiantes; private final EleccionRepo elecciones; private final TarifaRepo tarifas;
 private final EntityManager entityManager; private final AprobadoRepo aprobados; private final SolicitudRepo solicitudes; private final MatriculaRepo matriculas; private final MatriculaDetalleRepo detalles; private final PagoRepo pagos; private final PlanesApi planes; private final ApplicationEventPublisher events;
 MatriculaService(EstudianteRepo e,EleccionRepo el,TarifaRepo t,MatriculaRepo m,MatriculaDetalleRepo d,PagoRepo p,PlanesApi a,AprobadoRepo aprobados,SolicitudRepo solicitudes,EntityManager entityManager,ApplicationEventPublisher events){estudiantes=e;elecciones=el;tarifas=t;matriculas=m;detalles=d;pagos=p;planes=a;this.aprobados=aprobados;this.solicitudes=solicitudes;this.entityManager=entityManager;this.events=events;}
 private Estudiante est(Long id){return estudiantes.findById(id).orElseThrow(()->new NoSuchElementException("Estudiante no encontrado"));}
 private Tarifa tarifa(String periodo){return tarifas.findByPeriodoIgnoreCase(periodo).orElseThrow(()->new IllegalArgumentException("No hay tarifa configurada para el periodo "+periodo));}
 EstudianteDto dto(Estudiante e){return new EstudianteDto(e.id,e.codigo,e.nombre,e.correo,e.telefono,e.apoderado);}
 EleccionDto dto(EleccionCarrera e){return new EleccionDto(e.id,e.estudianteId,est(e.estudianteId).nombre,e.planId,planes.plan(e.planId).school(),e.periodo,e.fecha);}
 TarifaDto dto(Tarifa t){return new TarifaDto(t.periodo,t.base,t.porCredito,t.moneda,t.actualizado);}
 MatriculaDto dto(Matricula m){var ts=planes.plan(m.planId);var es=est(m.estudianteId);var ds=detalles.findByMatriculaId(m.id).stream().map(d->new DetalleDto(d.cursoId,d.curso,d.creditos)).toList();return new MatriculaDto(m.id,m.estudianteId,es.nombre,m.planId,ts.school(),m.periodo,m.estado,m.creditos,m.monto,m.pagado,m.monto.subtract(m.pagado).max(BigDecimal.ZERO),m.moneda,m.fecha,ds);}
 @Transactional public EstudianteDto crearEstudiante(EstudianteInput i){String codigo=i.codigo().trim();if(estudiantes.findByCodigoIgnoreCase(codigo).isPresent())throw new IllegalArgumentException("El código de estudiante ya está registrado");Estudiante e=new Estudiante();e.codigo=codigo;e.nombre=i.nombre().trim();e.correo=i.correo()==null?null:i.correo().trim();return dto(estudiantes.saveAndFlush(e));}
 public List<Long> aprobados(Long estudianteId){est(estudianteId);return aprobados.findAllByEstudianteId(estudianteId).stream().map(x->x.cursoId).toList();}
 @Transactional public void aprobar(Long estudianteId,Long cursoId){est(estudianteId);planes.curso(cursoId);if(!aprobados.existsByEstudianteIdAndCursoId(estudianteId,cursoId)){CursoAprobado a=new CursoAprobado();a.estudianteId=estudianteId;a.cursoId=cursoId;aprobados.saveAndFlush(a);}}
 List<EstudianteDto> estudiantes(){return estudiantes.findAll().stream().sorted(Comparator.comparing(e->e.nombre)).map(this::dto).toList();} public EstudianteDto registrarAlumno(String codigo,String nombre,String correo){return crearEstudiante(new EstudianteInput(codigo,nombre,correo));}
 public Long registrarAlumnoId(String codigo,String nombre,String correo){var existing=estudiantes.findByCodigoIgnoreCase(codigo.trim());if(existing.isPresent()){Estudiante e=existing.get();if(!e.nombre.equalsIgnoreCase(nombre.trim()))throw new IllegalArgumentException("El código ya pertenece a otro nombre");return e.id;}return crearEstudiante(new EstudianteInput(codigo,nombre,correo)).id();}
 public EstudianteDto alumno(Long id){return dto(est(id));}
 @Transactional public EstudianteDto perfilAlumno(Long id,String correo,String telefono,String apoderado){Estudiante e=est(id);if(correo==null||correo.isBlank()||telefono==null||telefono.isBlank()||apoderado==null||apoderado.isBlank())throw new IllegalArgumentException("Completa correo, teléfono y apoderado");e.correo=correo.trim();e.telefono=telefono.trim();e.apoderado=apoderado.trim();return dto(estudiantes.saveAndFlush(e));}
 public List<EleccionDto> eleccionesAlumno(Long estudianteId){est(estudianteId);return elecciones.findAllByEstudianteId(estudianteId).stream().map(this::dto).toList();}
 @Transactional public EleccionDto editarCarreraAlumno(Long estudianteId,String periodo,Long planId){
  if(elecciones.findByEstudianteIdAndPeriodo(estudianteId,periodo.trim()).isEmpty())throw new IllegalArgumentException("El administrador debe asignar primero una carrera para este periodo");
  return elegir(new EleccionInput(estudianteId,planId,periodo));
 }
 @Transactional EstudianteDto editarEstudiante(Long id,EstudianteInput i){
  Estudiante e=est(id); String codigo=i.codigo().trim();
  estudiantes.findByCodigoIgnoreCase(codigo).filter(x->!x.id.equals(id)).ifPresent(x->{throw new IllegalArgumentException("El código de estudiante ya está registrado");});
  e.codigo=codigo;e.nombre=i.nombre().trim();e.correo=i.correo()==null?null:i.correo().trim();return dto(estudiantes.saveAndFlush(e));
 }
 @Transactional void borrarEstudiante(Long id){
  est(id);
  if(!elecciones.findAllByEstudianteId(id).isEmpty()) throw new IllegalArgumentException("No se puede borrar: el estudiante tiene operaciones académicas");
  if(matriculas.existsByEstudianteId(id)) throw new IllegalArgumentException("No se puede borrar: el estudiante tiene matrículas");
  estudiantes.deleteById(id);estudiantes.flush();
 }
 @Transactional EleccionDto elegir(EleccionInput i){est(i.estudianteId());var plan=planes.plan(i.planId());String periodo=i.periodo().trim();matriculas.findByEstudianteIdAndPeriodo(i.estudianteId(),periodo).ifPresent(m->{if(!m.planId.equals(plan.id()))throw new IllegalArgumentException("No se puede cambiar el plan de una matrícula ya generada");});EleccionCarrera e=elecciones.findByEstudianteIdAndPeriodo(i.estudianteId(),periodo).orElseGet(EleccionCarrera::new);e.estudianteId=i.estudianteId();e.planId=plan.id();e.periodo=periodo;e.fecha=LocalDateTime.now();return dto(elecciones.saveAndFlush(e));}
 List<EleccionDto> elecciones(){return elecciones.findAllByOrderByFechaDesc().stream().map(this::dto).toList();}
 @Transactional TarifaDto configurar(TarifaInput i){String periodo=i.periodo().trim();Tarifa t=tarifas.findByPeriodoIgnoreCase(periodo).orElseGet(Tarifa::new);t.periodo=periodo;t.base=i.base().setScale(2,RoundingMode.HALF_UP);t.porCredito=i.porCredito().setScale(2,RoundingMode.HALF_UP);t.moneda=i.moneda().trim().toUpperCase(Locale.ROOT);t.actualizado=LocalDateTime.now();return dto(tarifas.saveAndFlush(t));}
 List<TarifaDto> tarifas(){return tarifas.findAll().stream().sorted(Comparator.comparing(t->t.periodo)).map(this::dto).toList();}
 @Transactional public MatriculaDto matricular(MatriculaInput i){
  SolicitudMatricula solicitud=solicitudes.findByEstudianteIdAndPeriodo(i.estudianteId(),i.periodo().trim()).orElse(null);
  boolean pendiente=solicitud!=null&&"PENDIENTE".equals(solicitud.estado);
  MatriculaDto m=matricular(i,pendiente?solicitud.id:null);
  if(pendiente){solicitud.estado=new HashSet<>(cursoIds(solicitud)).equals(new HashSet<>(i.cursos()))?"APROBADA":"RECHAZADA";solicitudes.saveAndFlush(solicitud);}
  return m;
 }
 private MatriculaDto matricular(MatriculaInput i,Long excluirSolicitudId){
  Estudiante e=est(i.estudianteId());var plan=planes.plan(i.planId());String periodo=i.periodo().trim();
  EleccionCarrera choice=elecciones.findByEstudianteIdAndPeriodo(e.id,periodo).orElseThrow(()->new IllegalArgumentException("Registra primero la elección de carrera para este periodo"));
  if(!choice.planId.equals(plan.id()))throw new IllegalArgumentException("La carrera no coincide con la elección registrada");
  if(matriculas.findByEstudianteIdAndPeriodo(e.id,periodo).isPresent())throw new IllegalArgumentException("El estudiante ya tiene matrícula en este periodo");
  Selection selection=validarSeleccion(e,plan.id(),periodo,i.cursos(),excluirSolicitudId);
  var selected=selection.selected();int credits=selection.credits();Tarifa t=selection.tariff();BigDecimal total=selection.total();
  Matricula m=new Matricula();m.estudianteId=e.id;m.planId=plan.id();m.periodo=periodo;m.estado="PENDIENTE";m.creditos=credits;m.monto=total;m.pagado=BigDecimal.ZERO.setScale(2);m.moneda=t.moneda;m.fecha=LocalDateTime.now();matriculas.saveAndFlush(m);
  selected.forEach((id,c)->{MatriculaDetalle d=new MatriculaDetalle();d.matriculaId=m.id;d.cursoId=id;d.curso=c.name();d.creditos=c.credits();detalles.save(d);});
  detalles.flush();return dto(m);
 }
 private record Selection(Map<Long,pe.edu.upeu.fia.planes.CursoDto> selected,int credits,Tarifa tariff,BigDecimal total){}
 private Selection validarSeleccion(Estudiante e,Long planId,String periodo,List<Long> cursoIds,Long excluirSolicitudId){
  if(new HashSet<>(cursoIds).size()!=cursoIds.size())throw new IllegalArgumentException("Hay cursos repetidos en la selección");
  Map<Long,pe.edu.upeu.fia.planes.CursoDto> selected=new LinkedHashMap<>();
  for(Long id:cursoIds){var c=planes.curso(id);if(!c.planId().equals(planId))throw new IllegalArgumentException("El curso "+c.name()+" no pertenece a la carrera elegida");selected.put(id,c);}
  planes.bloquearCursos(new ArrayList<>(selected.keySet()));
  Tarifa t=tarifa(periodo);int credits=selected.values().stream().mapToInt(c->c.credits()).sum();
  if(credits>25)throw new IllegalArgumentException("La matrícula no puede superar el límite de 25 créditos");
  var allPlanCourses=planes.cursos(planId,null,null,"name");
  Set<Long> approved=new HashSet<>(aprobados(e.id));

  for(var c:selected.values()){
   if(c.horario()==null||c.horario().isBlank())throw new IllegalArgumentException("El curso "+c.name()+" aún no tiene horario configurado");
   if(allPlanCourses.stream().anyMatch(x->x.cycle()<c.cycle()&&!approved.contains(x.id())))throw new IllegalArgumentException("No puedes llevar "+c.name()+" hasta aprobar los cursos de ciclos anteriores");
   String pre=c.prerequisites()==null?"":c.prerequisites().trim();
   if(!pre.isBlank()&&!List.of("ninguno","no tiene","n/a").contains(pre.toLowerCase(Locale.ROOT))){
    if(allPlanCourses.stream().noneMatch(x->x.name().equalsIgnoreCase(pre)))throw new IllegalArgumentException("El prerrequisito de "+c.name()+" no existe en el plan: "+pre);
    boolean met=approved.stream().map(planes::curso).anyMatch(x->x.name().equalsIgnoreCase(pre));
    if(!met&&allPlanCourses.stream().anyMatch(x->x.name().equalsIgnoreCase(pre)))throw new IllegalArgumentException("Falta el prerrequisito previo de "+c.name()+": "+pre);
   }
   long occupied=ocupados(c.id(),periodo,excluirSolicitudId);
   if(occupied>=(c.cupos()>0?c.cupos():30))throw new IllegalArgumentException("No quedan cupos en "+c.name());
  }
  for(var a:selected.values())for(var b:selected.values())if(a.id()<b.id()&&conflict(a.horario(),b.horario()))throw new IllegalArgumentException("Cruce de horarios entre "+a.name()+" y "+b.name());
  BigDecimal total=t.base.add(t.porCredito.multiply(BigDecimal.valueOf(credits))).setScale(2,RoundingMode.HALF_UP);
  return new Selection(selected,credits,t,total);

 }
 @Transactional public MatriculaDto matricularAlumno(Long estudianteId,String periodoSolicitado,List<Long> cursos){
  Estudiante e=est(estudianteId);
  if(e.correo==null||e.correo.isBlank()||e.telefono==null||e.telefono.isBlank()||e.apoderado==null||e.apoderado.isBlank())throw new IllegalArgumentException("Completa primero tus datos y los de tu apoderado");
  String periodo=periodoSolicitado.trim();
  EleccionCarrera choice=elecciones.findByEstudianteIdAndPeriodo(estudianteId,periodo).orElseThrow(()->new IllegalArgumentException("El administrador aún no asignó una carrera para este periodo"));
  SolicitudMatricula solicitud=solicitudes.findByEstudianteIdAndPeriodo(estudianteId,periodo).orElse(null);
  Long excluir=solicitud!=null&&"PENDIENTE".equals(solicitud.estado)?solicitud.id:null;
  MatriculaDto m=matricular(new MatriculaInput(estudianteId,choice.planId,periodo,cursos),excluir);
  if(excluir!=null){solicitud.estado="APROBADA";solicitudes.saveAndFlush(solicitud);}
  return m;
 }
 @Transactional public SolicitudDto solicitarAlumno(Long estudianteId,String periodoSolicitado,List<Long> cursosSolicitados){
  Estudiante e=est(estudianteId);
  if(e.correo==null||e.correo.isBlank()||e.telefono==null||e.telefono.isBlank()||e.apoderado==null||e.apoderado.isBlank())throw new IllegalArgumentException("Completa primero tus datos y los de tu apoderado");
  String periodo=periodoSolicitado.trim();
  EleccionCarrera choice=elecciones.findByEstudianteIdAndPeriodo(estudianteId,periodo).orElseThrow(()->new IllegalArgumentException("El administrador aún no asignó una carrera para este periodo"));
  if(matriculas.findByEstudianteIdAndPeriodo(estudianteId,periodo).isPresent())throw new IllegalArgumentException("Ya existe una matrícula para este periodo");
  SolicitudMatricula solicitud=solicitudes.findByEstudianteIdAndPeriodo(estudianteId,periodo).orElseGet(SolicitudMatricula::new);
  Selection v=validarSeleccion(e,choice.planId,periodo,cursosSolicitados,solicitud.id);
  if("APROBADA".equals(solicitud.estado))throw new IllegalArgumentException("La selección ya fue aprobada");
  solicitud.estudianteId=estudianteId;solicitud.planId=choice.planId;solicitud.periodo=periodo;solicitud.cursoIds=cursosSolicitados.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
  solicitud.creditos=v.credits();solicitud.monto=v.total();solicitud.moneda=v.tariff().moneda;solicitud.estado="PENDIENTE";solicitud.fecha=LocalDateTime.now();
  return solicitudDto(solicitudes.saveAndFlush(solicitud));
 }
 public List<SolicitudDto> solicitudesAlumno(Long estudianteId){est(estudianteId);return solicitudes.findAllByEstudianteId(estudianteId).stream().map(this::solicitudDto).toList();}
 public List<SolicitudDto> solicitudes(){return solicitudes.findAllByOrderByFechaDesc().stream().map(this::solicitudDto).toList();}
 private List<Long> cursoIds(SolicitudMatricula s){return Arrays.stream(s.cursoIds.split(",")).map(Long::valueOf).toList();}
 private SolicitudDto solicitudDto(SolicitudMatricula s){return new SolicitudDto(s.id,s.estudianteId,est(s.estudianteId).nombre,s.planId,planes.plan(s.planId).school(),s.periodo,cursoIds(s),s.creditos,s.monto,s.moneda,s.estado,s.fecha);}
 @Transactional public MatriculaDto generarSolicitud(Long id){
  SolicitudMatricula s=solicitudes.findById(id).orElseThrow(()->new NoSuchElementException("Solicitud no encontrada"));
  if(!"PENDIENTE".equals(s.estado))throw new IllegalArgumentException("La solicitud no está pendiente");
  MatriculaDto m=matricular(new MatriculaInput(s.estudianteId,s.planId,s.periodo,cursoIds(s)),s.id);
  s.estado="APROBADA";solicitudes.saveAndFlush(s);return m;
 }
 @Transactional public SolicitudDto rechazarSolicitud(Long id){SolicitudMatricula s=solicitudes.findById(id).orElseThrow(()->new NoSuchElementException("Solicitud no encontrada"));if(!"PENDIENTE".equals(s.estado))throw new IllegalArgumentException("La solicitud no está pendiente");s.estado="RECHAZADA";return solicitudDto(solicitudes.saveAndFlush(s));}
 private boolean conflict(String a,String b){if(a==null||b==null)return false;for(String x:a.split(";"))for(String y:b.split(";")){String[] p=x.trim().split(" ");String[] q=y.trim().split(" ");if(p.length!=2||q.length!=2||!p[0].equalsIgnoreCase(q[0]))continue;String[] u=p[1].split("-");String[] v=q[1].split("-");if(u.length==2&&v.length==2&&u[0].compareTo(v[1])<0&&v[0].compareTo(u[1])<0)return true;}return false;}
 private long ocupados(Long cursoId,String periodo,Long excluirSolicitudId){
  long matriculados=detalles.ocupados(cursoId,periodo);
  long reservados=solicitudes.pendientesPeriodo(periodo).stream().filter(x->!Objects.equals(x.id,excluirSolicitudId)&&cursoIds(x).contains(cursoId)).count();
  return matriculados+reservados;
 }
 public Map<Long,Long> cuposAlumno(Long estudianteId,String periodo){EleccionCarrera choice=elecciones.findByEstudianteIdAndPeriodo(estudianteId,periodo).orElseThrow(()->new IllegalArgumentException("No tienes una carrera asignada para este periodo"));return cupos(choice.planId,choice.periodo);}
 public Map<Long,Long> cupos(Long planId,String periodo){planes.plan(planId);Map<Long,Long> result=new LinkedHashMap<>();for(var c:planes.cursos(planId,null,null,"cycle")){long used=ocupados(c.id(),periodo,null);result.put(c.id(),Math.max(0,(c.cupos()>0?c.cupos():30)-used));}return result;}
 public List<MatriculaDto> matriculas(){return matriculas.findAllByOrderByFechaDesc().stream().map(this::dto).toList();}
 public List<MatriculaDto> matriculasAlumno(Long estudianteId){return matriculas.findByEstudianteIdOrderByFechaDesc(estudianteId).stream().map(this::dto).toList();}
 public Pagina<EstudianteDto> estudiantesPagina(String buscar,int pagina,int tamano){
  String q=buscar==null?"":buscar.trim();var page=PageRequest.of(pagina,tamano,Sort.by("nombre").ascending().and(Sort.by("id")));
  var p=q.isBlank()?estudiantes.findAll(page):estudiantes.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(q,q,page);
  return new Pagina<>(p.getContent().stream().map(this::dto).toList(),p.getTotalElements(),pagina,tamano,p.getTotalPages());
 }
 public Pagina<MatriculaResumenDto> matriculasPagina(String periodo,String estado,Long planId,String buscar,int pagina,int tamano){
  String p=filtro(periodo),e=filtro(estado),q=filtro(buscar);
  long total=((Number)consultaMatriculas("select count(m)",p,e,planId,q).getSingleResult()).longValue();
  return paginaMatriculas(p,e,planId,q,pagina,tamano,total);
 }
 private Pagina<MatriculaResumenDto> paginaMatriculas(String p,String e,Long planId,String q,int pagina,int tamano,long total){
  @SuppressWarnings("unchecked") List<Object[]> result=consultaMatriculas("select m, s.nombre",p,e,planId,q)
      .setFirstResult(Math.multiplyExact(pagina,tamano)).setMaxResults(tamano).getResultList();
  Map<Long,String> carreras=new HashMap<>();
  var items=result.stream().map(row->{Matricula m=(Matricula)row[0];String carrera=carreras.computeIfAbsent(m.planId,id->planes.plan(id).school());return new MatriculaResumenDto(m.id,m.estudianteId,(String)row[1],m.planId,carrera,m.periodo,m.estado,m.creditos,m.monto,m.pagado,m.monto.subtract(m.pagado).max(BigDecimal.ZERO),m.moneda,m.fecha);}).toList();
  return new Pagina<>(items,total,pagina,tamano,(int)((total+tamano-1)/tamano));
 }
 public MatriculaDto matricula(Long id){return dto(matriculas.findById(id).orElseThrow(()->new NoSuchElementException("Matrícula no encontrada")));}
 public Map<String,Object> reporteMatriculas(String periodo,String estado,Long planId,String buscar,int pagina,int tamano){
  String p=filtro(periodo),e=filtro(estado),q=filtro(buscar);
  Object[] totals=(Object[])consultaMatriculas("select count(m), coalesce(sum(m.monto),0), coalesce(sum(m.pagado),0)",p,e,planId,q).getSingleResult();
  long total=((Number)totals[0]).longValue();
  var rows=paginaMatriculas(p,e,planId,q,pagina,tamano,total);
  BigDecimal monto=(BigDecimal)totals[1],pagado=(BigDecimal)totals[2];
  Map<String,Object> out=new LinkedHashMap<>();out.put("total",rows.total());out.put("monto",monto);out.put("pagado",pagado);out.put("saldo",monto.subtract(pagado));out.put("matriculas",rows.items());out.put("pagina",pagina);out.put("tamano",tamano);out.put("paginas",rows.paginas());return out;
 }
 private String filtro(String value){return value==null||value.isBlank()?null:value.trim();}
 private Query consultaMatriculas(String select,String periodo,String estado,Long planId,String buscar){
  StringBuilder hql=new StringBuilder(select).append(" from Matricula m, Estudiante s where s.id=m.estudianteId");
  if(periodo!=null)hql.append(" and lower(m.periodo)=:periodo");
  if(estado!=null)hql.append(" and lower(m.estado)=:estado");
  if(planId!=null)hql.append(" and m.planId=:planId");
  if(buscar!=null)hql.append(" and (locate(:buscar,lower(s.nombre))>0 or locate(:buscar,lower(s.codigo))>0 or locate(:buscar,lower(m.periodo))>0)");
  if(select.startsWith("select m,"))hql.append(" order by m.fecha desc, m.id desc");
  Query query=entityManager.createQuery(hql.toString());
  if(periodo!=null)query.setParameter("periodo",periodo.toLowerCase(Locale.ROOT));
  if(estado!=null)query.setParameter("estado",estado.toLowerCase(Locale.ROOT));
  if(planId!=null)query.setParameter("planId",planId);
  if(buscar!=null)query.setParameter("buscar",buscar.toLowerCase(Locale.ROOT));
  return query;
 }
 public Map<String,Object> contrato(Long id,Long estudianteId){Matricula m=matriculas.findById(id).orElseThrow(()->new NoSuchElementException("Matrícula no encontrada"));if(!m.estudianteId.equals(estudianteId))throw new IllegalArgumentException("La matrícula no pertenece al alumno");if(m.pagado.signum()<=0)throw new IllegalArgumentException("El contrato estará disponible después de registrar el primer pago");MatriculaDto d=dto(m);Map<String,Object> result=new LinkedHashMap<>();result.put("tipo","Contrato de estudios");result.put("matricula",d.id());result.put("estudiante",d.estudiante());result.put("carrera",d.carrera());result.put("periodo",d.periodo());result.put("cursos",d.cursos());result.put("monto",d.monto());result.put("moneda",d.moneda());result.put("estado",d.estado());Estudiante e=est(estudianteId);result.put("correo",e.correo);result.put("telefono",e.telefono);result.put("apoderado",e.apoderado);return result;}
 private String estadoPago(Pago p){return p.estado==null?"CONFIRMADO":p.estado;}
 private PagoDto pagoDto(Pago p){return new PagoDto(p.id,p.matriculaId,p.monto,p.metodo,p.referencia,p.fecha,estadoPago(p),p.voucherImagen!=null,p.observacion);}
 @Transactional PagoDto pagar(Long id,PagoInput i){
  Matricula m=matriculas.findLockedById(id).orElseThrow(()->new NoSuchElementException("Matrícula no encontrada"));
  if(pagos.findByMatriculaIdOrderByFechaDesc(id).stream().anyMatch(p->"PENDIENTE_VERIFICACION".equals(estadoPago(p))))throw new IllegalArgumentException("Revisa primero el pago pendiente del alumno");
  BigDecimal amount=i.monto().setScale(2,RoundingMode.HALF_UP);if(amount.signum()<=0)throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
  BigDecimal balance=m.monto.subtract(m.pagado);if(amount.compareTo(balance)>0)throw new IllegalArgumentException("El pago supera el saldo pendiente: "+balance+" "+m.moneda);
  Pago p=new Pago();p.matriculaId=id;p.monto=amount;p.metodo=i.metodo().trim();p.referencia=i.referencia()==null?null:i.referencia().trim();p.fecha=LocalDateTime.now();p.estado="CONFIRMADO";p.revisadoEn=LocalDateTime.now();p=pagos.saveAndFlush(p);
  m.pagado=m.pagado.add(amount);m.estado=m.pagado.compareTo(m.monto)>=0?"PAGADA":"PARCIAL";matriculas.saveAndFlush(m);return pagoDto(p);
 }
 @Transactional public PagoDto pagarAlumno(Long id,Long estudianteId,BigDecimal monto,String metodo,String referencia,String modalidad,Integer cuotas,String voucher){
  Matricula m=matriculas.findLockedById(id).orElseThrow(()->new NoSuchElementException("Matrícula no encontrada"));
  if(!m.estudianteId.equals(estudianteId))throw new IllegalArgumentException("La matrícula no pertenece al alumno");
  if(monto==null||metodo==null||metodo.isBlank())throw new IllegalArgumentException("Indica monto y medio de pago");
  String medio=metodo.trim();if(!Set.of("Efectivo","Yape","Transferencia").contains(medio))throw new IllegalArgumentException("Elige efectivo, Yape o transferencia");
  String tipo=modalidad==null?"":modalidad.trim().toUpperCase(Locale.ROOT);
  if(!Set.of("CONTADO","CUOTAS").contains(tipo))throw new IllegalArgumentException("Elige pago al contado o en cuotas");
  BigDecimal saldo=m.monto.subtract(m.pagado);BigDecimal amount=monto.setScale(2,RoundingMode.HALF_UP);
  if(amount.signum()<=0||amount.compareTo(saldo)>0)throw new IllegalArgumentException("El monto debe ser positivo y no superar el saldo");
  List<Pago> history=pagos.findByMatriculaIdOrderByFechaDesc(id);
  if(history.stream().anyMatch(p->"PENDIENTE_VERIFICACION".equals(estadoPago(p))))throw new IllegalArgumentException("Ya tienes un pago pendiente de revisión");
  List<Pago> confirmed=history.stream().filter(p->"CONFIRMADO".equals(estadoPago(p))).toList();
  if("CONTADO".equals(tipo)&&amount.compareTo(saldo)!=0)throw new IllegalArgumentException("Al contado debes declarar todo el saldo: "+saldo);
  int n=cuotas==null?0:cuotas;if("CUOTAS".equals(tipo)&&(n<2||n>12))throw new IllegalArgumentException("Elige entre 2 y 12 cuotas");
  String previous=confirmed.isEmpty()?null:confirmed.get(0).metodo;
  if(previous!=null&&previous.startsWith("CUOTAS_")&&!previous.startsWith("CUOTAS_"+n+"|"))throw new IllegalArgumentException("Mantén el plan de cuotas elegido en el primer pago");
  if("CUOTAS".equals(tipo)){
   if(confirmed.size()>=n)throw new IllegalArgumentException("Ya se registraron todas las cuotas del plan");
   BigDecimal expected=saldo.divide(BigDecimal.valueOf(n-confirmed.size()),2,RoundingMode.HALF_UP);
   if(amount.compareTo(expected)!=0)throw new IllegalArgumentException("La cuota a declarar es "+expected+" "+m.moneda);
  }
  String stored="CONTADO".equals(tipo)?"CONTADO|"+medio:"CUOTAS_"+n+"|"+medio;
  Pago p=new Pago();p.matriculaId=id;p.monto=amount;p.metodo=stored;p.referencia=referencia==null?null:referencia.trim();p.fecha=LocalDateTime.now();p.estado="PENDIENTE_VERIFICACION";
  if(!"Efectivo".equals(medio)){
   if(p.referencia==null||p.referencia.isBlank())throw new IllegalArgumentException("Indica el número de operación de "+medio);
   Voucher image=validarVoucher(voucher);p.voucherImagen=image.bytes();p.voucherMime=image.mime();
  }else if(voucher!=null&&!voucher.isBlank()){Voucher image=validarVoucher(voucher);p.voucherImagen=image.bytes();p.voucherMime=image.mime();}
  return pagoDto(pagos.saveAndFlush(p));
 }
 public record Voucher(byte[] bytes,String mime){}
 private Voucher validarVoucher(String dataUrl){
  if(dataUrl==null||!dataUrl.matches("(?s)^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+$"))throw new IllegalArgumentException("Adjunta una foto JPG, PNG o WebP del voucher");
  String mime=dataUrl.substring(5,dataUrl.indexOf(';'));String encoded=dataUrl.substring(dataUrl.indexOf(',')+1);
  if(encoded.length()>7_000_000)throw new IllegalArgumentException("El voucher supera el límite de 5 MB");
  byte[] image;try{image=Base64.getDecoder().decode(encoded);}catch(IllegalArgumentException error){throw new IllegalArgumentException("No se pudo leer la imagen del voucher");}
  if(image.length<12||image.length>5_000_000)throw new IllegalArgumentException("La imagen del voucher debe tener menos de 5 MB");
  boolean jpg=(image[0]&255)==255&&(image[1]&255)==216&&(image[2]&255)==255;
  boolean png=(image[0]&255)==137&&(image[1]&255)==80&&(image[2]&255)==78&&(image[3]&255)==71;
  boolean webp=image[0]=='R'&&image[1]=='I'&&image[2]=='F'&&image[3]=='F'&&image[8]=='W'&&image[9]=='E'&&image[10]=='B'&&image[11]=='P';
  if(!(mime.equals("image/jpeg")&&jpg||mime.equals("image/png")&&png||mime.equals("image/webp")&&webp))throw new IllegalArgumentException("El archivo no corresponde a una imagen válida");
  return new Voucher(image,mime);
 }
 public List<PagoPendienteDto> pagosPendientes(){return pagos.findAll().stream().filter(p->"PENDIENTE_VERIFICACION".equals(estadoPago(p))).sorted(Comparator.comparing((Pago p)->p.fecha).reversed()).map(p->{Matricula m=matriculas.findById(p.matriculaId).orElseThrow();Estudiante e=est(m.estudianteId);return new PagoPendienteDto(p.id,p.matriculaId,e.nombre,e.codigo,planes.plan(m.planId).school(),m.periodo,p.monto,m.moneda,p.metodo,p.referencia,p.fecha,p.voucherImagen!=null);}).toList();}
 public Voucher voucherPago(Long matriculaId,Long pagoId){Pago p=pagos.findById(pagoId).orElseThrow(()->new NoSuchElementException("Pago no encontrado"));if(!p.matriculaId.equals(matriculaId)||p.voucherImagen==null)throw new NoSuchElementException("Voucher no encontrado para esta matrícula");return new Voucher(p.voucherImagen,p.voucherMime);}
 @Transactional public PagoDto confirmarPago(Long matriculaId,Long pagoId){
  Matricula m=matriculas.findLockedById(matriculaId).orElseThrow(()->new NoSuchElementException("Matrícula no encontrada"));Pago p=pagos.findById(pagoId).orElseThrow(()->new NoSuchElementException("Pago no encontrado"));
  if(!p.matriculaId.equals(matriculaId)||!"PENDIENTE_VERIFICACION".equals(estadoPago(p)))throw new IllegalArgumentException("El pago no está pendiente de revisión");
  if(p.monto.compareTo(m.monto.subtract(m.pagado))>0)throw new IllegalArgumentException("El pago supera el saldo actual");
  p.estado="CONFIRMADO";p.revisadoEn=LocalDateTime.now();p.observacion="Pago verificado por administración";pagos.saveAndFlush(p);
  m.pagado=m.pagado.add(p.monto);m.estado=m.pagado.compareTo(m.monto)>=0?"PAGADA":"PARCIAL";matriculas.saveAndFlush(m);events.publishEvent(new PagoRevisado(p.id,m.id,p.estado,est(m.estudianteId).correo));return pagoDto(p);
 }
 @Transactional public PagoDto rechazarPago(Long matriculaId,Long pagoId,String reason){
  if(reason==null||reason.isBlank())throw new IllegalArgumentException("Indica por qué se rechaza el pago");
  Matricula m=matriculas.findLockedById(matriculaId).orElseThrow(()->new NoSuchElementException("Matrícula no encontrada"));Pago p=pagos.findById(pagoId).orElseThrow(()->new NoSuchElementException("Pago no encontrado"));
  if(!p.matriculaId.equals(m.id)||!"PENDIENTE_VERIFICACION".equals(estadoPago(p)))throw new IllegalArgumentException("El pago no está pendiente de revisión");
  p.estado="RECHAZADO";p.observacion=reason.trim();p.revisadoEn=LocalDateTime.now();p=pagos.saveAndFlush(p);events.publishEvent(new PagoRevisado(p.id,m.id,p.estado,est(m.estudianteId).correo));return pagoDto(p);
 }
 public List<PagoDto> pagosAlumno(Long id,Long estudianteId){
  Matricula m=matriculas.findById(id).orElseThrow(()->new NoSuchElementException("Matrícula no encontrada"));
  if(!m.estudianteId.equals(estudianteId))throw new IllegalArgumentException("La matrícula no pertenece al alumno");
  return pagos(id);
 }
 public Map<String,Object> comprobanteAlumno(Long id,Long pagoId,Long estudianteId){
  Matricula m=matriculas.findById(id).orElseThrow(()->new NoSuchElementException("Matrícula no encontrada"));
  if(!m.estudianteId.equals(estudianteId))throw new IllegalArgumentException("La matrícula no pertenece al alumno");
  PagoDto p=pagos(id).stream().filter(x->x.id().equals(pagoId)&&"CONFIRMADO".equals(x.estado())).findFirst().orElseThrow(()->new NoSuchElementException("No hay un pago confirmado para emitir el comprobante"));
  Map<String,Object> out=new LinkedHashMap<>();out.put("tipo","Comprobante de pago de demostración");out.put("nota","Pago confirmado por administración en este sistema; comprobante de demostración sin validez tributaria");
  out.put("pago",p.id());out.put("matricula",m.id);out.put("alumno",est(estudianteId).nombre);out.put("carrera",planes.plan(m.planId).school());out.put("periodo",m.periodo);out.put("fecha",p.fecha());out.put("monto",p.monto());out.put("moneda",m.moneda);out.put("metodo",p.metodo());out.put("referencia",p.referencia());out.put("saldo",m.monto.subtract(m.pagado));return out;
 }

 List<PagoDto> pagos(Long id){if(!matriculas.existsById(id))throw new NoSuchElementException("Matrícula no encontrada");return pagos.findByMatriculaIdOrderByFechaDesc(id).stream().map(this::pagoDto).toList();}
}







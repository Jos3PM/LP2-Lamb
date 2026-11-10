package pe.edu.upeu.fia.planes.internal;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Horarios referenciales para cursos heredados que no tienen programación. */
@Service
class HorariosService {
 private static final String[] DAYS={"Lun","Mar","Mie","Jue","Vie"};
 private static final int[] STARTS={8,10,14,16,18};
 private final CursoRepository courses;
 HorariosService(CursoRepository courses){this.courses=courses;}

 @Transactional public Map<String,Object> generarTodos(){return generar(null);}
 @Transactional public Map<String,Object> generarPlan(Long planId){return generar(planId);}
 private Map<String,Object> generar(Long selectedPlanId){
  List<Curso> all=courses.findAll().stream().filter(c->selectedPlanId==null||Objects.equals(planId(c),selectedPlanId)).toList();
  Map<String,List<Curso>> groups=new TreeMap<>();
  for(Curso c:all)groups.computeIfAbsent(planId(c)+":"+c.cycle,k->new ArrayList<>()).add(c);
  int generated=0;int plans=0;Set<Long> touchedPlans=new HashSet<>();
  List<Curso> changed=new ArrayList<>();
  for(List<Curso> group:groups.values()){
   group.sort(Comparator.comparing(c->c.id));
   boolean[][] busy=new boolean[5][20];
   for(Curso c:group)if(c.horario!=null&&!c.horario.isBlank())markExisting(busy,c.horario);
   int ordinal=0;
   for(Curso c:group){
    if(c.horario!=null&&!c.horario.isBlank()){ordinal++;continue;}
    int remaining=Math.min(6,Math.max(2,c.ht+c.hp));
    List<String> slots=new ArrayList<>();Set<Integer> usedDays=new HashSet<>();int block=0;
    while(remaining>0){
     int duration=Math.min(2,remaining);boolean placed=false;
     for(int pass=0;pass<2&&!placed;pass++)for(int offset=0;offset<5&&!placed;offset++){
      int day=(ordinal+block*2+offset)%5;
      if(pass==0&&usedDays.contains(day))continue;
      for(int start:STARTS){
       if(start+duration>20||start<12&&start+duration>12)continue;
       if(free(busy,day,start,duration)){
        for(int hour=start;hour<start+duration;hour++)busy[day][hour]=true;
        slots.add(DAYS[day]+" "+time(start)+"-"+time(start+duration));
        usedDays.add(day);remaining-=duration;block++;placed=true;break;
       }
      }
     }
     if(!placed)throw new IllegalStateException("No se pudo asignar horario sin cruce a "+c.name+"; edita el plan manualmente");
    }
    c.horario=String.join("; ",slots);
    String note="Horario referencial generado automáticamente; confirmar con administración.";
    c.notes=(c.notes==null||c.notes.isBlank())?note:(c.notes.length()>1400?c.notes.substring(0,1400):c.notes)+" | "+note;
    changed.add(c);generated++;touchedPlans.add(planId(c));ordinal++;
   }
  }
  courses.saveAll(changed);courses.flush();plans=touchedPlans.size();
  return Map.of("planesActualizados",plans,"horariosGenerados",generated,"cursosTotales",all.size(),"tipo","referencial");
 }
 public String proponer(Long planId,int cycle,int hours,Long excludeId){
  boolean[][] busy=new boolean[5][20];
  for(Curso other:courses.findAll())if(Objects.equals(planId(other),planId)&&other.cycle==cycle&&!Objects.equals(other.id,excludeId)&&other.horario!=null)markExisting(busy,other.horario);
  int remaining=Math.min(6,Math.max(2,hours));List<String> slots=new ArrayList<>();Set<Integer> usedDays=new HashSet<>();
  while(remaining>0){
   int duration=Math.min(2,remaining);boolean placed=false;
   for(int pass=0;pass<2&&!placed;pass++)for(int day=0;day<5&&!placed;day++){
    if(pass==0&&usedDays.contains(day))continue;
    for(int start:STARTS)if(start+duration<=20&&!(start<12&&start+duration>12)&&free(busy,day,start,duration)){
     for(int hour=start;hour<start+duration;hour++)busy[day][hour]=true;
     slots.add(DAYS[day]+" "+time(start)+"-"+time(start+duration));usedDays.add(day);remaining-=duration;placed=true;break;
    }
   }
   if(!placed)throw new IllegalArgumentException("No queda un horario libre para el ciclo "+cycle+" de este plan");
  }
  return String.join("; ",slots);
 }
 public void validarSinCruces(Long planId,int cycle,Long excludeId,String schedule){
  List<int[]> proposed=slots(schedule);
  for(int i=0;i<proposed.size();i++)for(int j=i+1;j<proposed.size();j++)if(overlap(proposed.get(i),proposed.get(j)))throw new IllegalArgumentException("El horario del curso contiene bloques cruzados");
  for(Curso other:courses.findAll())if(Objects.equals(planId(other),planId)&&other.cycle==cycle&&!Objects.equals(other.id,excludeId)&&other.horario!=null&&!other.horario.isBlank()){
   for(int[] a:proposed)for(int[] b:slots(other.horario))if(overlap(a,b))throw new IllegalArgumentException("Cruce de horario con "+other.name+" del ciclo "+cycle);
  }
 }
 private static boolean overlap(int[] a,int[] b){return a[0]==b[0]&&a[1]<b[2]&&b[1]<a[2];}
 private static List<int[]> slots(String schedule){
  List<int[]> out=new ArrayList<>();
  for(String value:schedule.split(";")){
   String slot=value.trim();if(!slot.matches("(?i)(Lun|Mar|Mie|Jue|Vie|Sab|Dom) [0-2][0-9]:[0-5][0-9]-[0-2][0-9]:[0-5][0-9]"))throw new IllegalArgumentException("Horario inválido. Usa Lun 08:00-10:00; Mar 10:00-12:00");
   String[] parts=slot.split(" ");String[] times=parts[1].split("-");
   int start=minutes(times[0]),end=minutes(times[1]);
   if(start>=end||end>1440)throw new IllegalArgumentException("La hora final debe ser posterior a la inicial");
   String[] all={"Lun","Mar","Mie","Jue","Vie","Sab","Dom"};int day=-1;
   for(int i=0;i<all.length;i++)if(all[i].equalsIgnoreCase(parts[0]))day=i;
   out.add(new int[]{day,start,end});
  }
  return out;
 }
 private static int minutes(String time){String[] p=time.split(":");int h=Integer.parseInt(p[0]),m=Integer.parseInt(p[1]);if(h>24||h==24&&m>0)throw new IllegalArgumentException("Hora fuera de rango");return h*60+m;}
 @Transactional public void aumentarCupos(Long id,int incremento){
  if(incremento<1||incremento>5000)throw new IllegalArgumentException("El aumento debe estar entre 1 y 5000 cupos");
  Curso c=courses.findLockedById(id).orElseThrow(()->new NoSuchElementException("Curso no encontrado"));
  c.cupos=Math.addExact(Math.max(0,c.cupos),incremento);courses.saveAndFlush(c);
 }
 private static Long planId(Curso c){return org.hibernate.Hibernate.unproxy(c.plan,Plan.class).id;}
 private static String time(int hour){return String.format(Locale.ROOT,"%02d:00",hour);}
 private static boolean free(boolean[][] busy,int day,int start,int duration){for(int h=start;h<start+duration;h++)if(busy[day][h])return false;return true;}
 private static void markExisting(boolean[][] busy,String schedule){
  for(String slot:schedule.split(";")){
   String[] parts=slot.trim().split(" ");if(parts.length!=2)continue;
   int day=Arrays.asList(DAYS).indexOf(parts[0]);if(day<0)continue;
   String[] times=parts[1].split("-");if(times.length!=2)continue;
   try{int start=Integer.parseInt(times[0].substring(0,2));int end=Integer.parseInt(times[1].substring(0,2));if(!times[1].endsWith(":00"))end++;for(int h=Math.max(0,start);h<Math.min(20,end);h++)busy[day][h]=true;}catch(RuntimeException ignored){}
  }
 }
}

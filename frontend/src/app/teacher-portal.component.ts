import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output, inject, signal } from '@angular/core';
import { FiaApiService } from './fia-api.service';
import { TeacherAssignment, TeacherProfile } from './models';

@Component({
  selector: 'app-teacher-portal', standalone: true, imports: [CommonModule],
  template: `<main class="teacher-portal"><header><div><p class="eyebrow">PORTAL DEL PROFESOR</p><h1>{{ profile()?.nombre || 'Mis asignaciones' }}</h1>
    <p>{{ profile()?.escuela }} · Código {{ profile()?.codigo }}</p></div><button type="button" (click)="logoutRequested.emit()">Cerrar sesión</button></header>
    @if (error()) { <div class="error" role="alert">{{ error() }}</div> }
    <section class="card"><h2>Mi perfil</h2>@if(profile(); as p){<dl><div><dt>Usuario</dt><dd>{{ p.username }}</dd></div><div><dt>Correo</dt><dd>{{ p.correo }}</dd></div>
      <div><dt>Escuela</dt><dd>{{ p.escuela }}</dd></div><div><dt>Curso del directorio</dt><dd>{{ p.curso || 'Pendiente de asignación' }}</dd></div></dl>}
      @else { <p>Tu perfil está cargando.</p> }</section>
    <section class="card"><div class="title"><div><h2>Mis cargas y grupos</h2><p>Solo se muestran las asignaciones vinculadas a tu docente.</p></div>
      <button type="button" (click)="refresh()">Actualizar</button></div>
      @for (assignment of assignments(); track assignment.detalleId) { <article class="assignment"><h3>{{ assignment.curso }}</h3>
        <p>{{ assignment.semestre }} · {{ assignment.sede }} · Grupo {{ assignment.grupo }}</p>
        <p>Ambiente: {{ assignment.ambiente || 'Pendiente' }} · Modalidad: {{ assignment.modalidad || 'Pendiente' }} · Cupos: {{ assignment.cupos }}</p>
        <strong>Horario</strong><ul>@for (block of assignment.bloques; track $index) { <li>{{ day(block.dia) }} {{ time(block.inicio) }}–{{ time(block.fin) }}</li> }
          @empty { <li>Horario aún no asignado.</li> }</ul></article> }
      @empty { <div class="empty">Aún no tienes cargas, grupos o cursos asignados. Consulta con administración si esperabas ver alguno.</div> }
    </section></main>`,
  styles: [`.teacher-portal{max-width:1160px;margin:2rem auto;padding:1.5rem;color:#102b4e;font:14px system-ui}.teacher-portal header,.title{display:flex;justify-content:space-between;align-items:center;gap:1rem}.eyebrow{color:#087b91;font-weight:700;letter-spacing:.1em}.teacher-portal h1{font-size:2rem;margin:.3rem 0}.card{background:white;border:1px solid #dce5ed;border-radius:12px;padding:1.3rem;margin:1rem 0;box-shadow:0 5px 20px #102b4e0b}dl{display:grid;grid-template-columns:repeat(2,1fr);gap:1rem}dt{font-size:.8rem;color:#60758a}dd{margin:.25rem 0;font-weight:650}.assignment{border-top:1px solid #dce5ed;padding:1rem 0}.assignment h3{margin:.2rem 0}.assignment p{color:#60758a}.assignment ul{display:flex;flex-wrap:wrap;gap:.5rem;padding:0;list-style:none}.assignment li{background:#eaf2f7;padding:.35rem .6rem;border-radius:5px}.empty{padding:2rem;color:#60758a;text-align:center}.error{background:#fff0ef;color:#a8322a;padding:.8rem;border-radius:6px}button{padding:.65rem .9rem;border:0;border-radius:6px;background:#087d91;color:white;font-weight:700;cursor:pointer}@media(max-width:600px){dl{grid-template-columns:1fr}.teacher-portal header,.title{align-items:flex-start;flex-direction:column}}`]
})
export class TeacherPortalComponent implements OnInit {
  @Output() logoutRequested = new EventEmitter<void>();
  private readonly api = inject(FiaApiService);
  readonly profile = signal<TeacherProfile | null>(null);
  readonly assignments = signal<TeacherAssignment[]>([]);
  readonly error = signal('');
  ngOnInit(): void { void this.refresh(); }
  async refresh(): Promise<void> {
    try { const [profile,assignments]=await Promise.all([this.api.perfilProfesor(),this.api.asignacionesProfesor()]);this.profile.set(profile);this.assignments.set(assignments);this.error.set(''); }
    catch(error){this.error.set(this.api.mensaje(error));}
  }
  day(value:number):string{return ['Dom','Lun','Mar','Mié','Jue','Vie','Sáb'][value]??`Día ${value}`;}
  time(value:number):string{return `${String(Math.floor(value/60)).padStart(2,'0')}:${String(value%60).padStart(2,'0')}`;}
}

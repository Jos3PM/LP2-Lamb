import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FiaApiService } from './fia-api.service';
import { HttpFeedbackComponent } from './http-feedback.component';
import { Curso, Plan } from './models';

interface Teacher { id: number; name: string; course: string; school: string; }

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, HttpFeedbackComponent],
  template: `
    <main class="module">
      <header><div><a routerLink="/">← Volver al portal</a><p class="eyebrow">GESTIÓN ACADÉMICA</p>
        <h1>Organización de docentes</h1><p>Asigna docentes a cursos de una escuela y consulta cada asignación.</p></div>
        <button type="button" (click)="reset()">Nueva asignación</button></header>
      <app-http-feedback />
      @if (error()) { <div class="error" role="alert">{{ error() }}</div> }
      @if (notice()) { <div class="notice" role="status">{{ notice() }}</div> }
      @if (loading()) { <p class="loading-state" role="status">Cargando escuelas, cursos y docentes…</p> }
      <section class="columns">
        <article class="card"><div class="card-head"><span class="icon-badge">D</span><div><h2>{{ editingId() ? 'Editar asignación' : 'Asignar docente' }}</h2><p>Relaciona un docente con la escuela y el curso.</p></div></div>
          <form #teacherForm="ngForm" (ngSubmit)="save()">
            <label>Escuela / carrera<select name="school" [(ngModel)]="form.school" (ngModelChange)="schoolChanged()" required>
              <option value="">Selecciona una escuela</option>
              @for (school of schools(); track school) { <option [value]="school">{{ school }}</option> }
            </select></label>
            <label>Curso<select name="course" [(ngModel)]="form.course" [disabled]="!form.school" required>
              <option value="">Selecciona un curso</option>
              @for (course of courses(); track course.name) { <option [value]="course.name">Ciclo {{ course.cycle }} · {{ course.name }}</option> }
            </select></label>
            <label>Nombre del docente<input name="name" [(ngModel)]="form.name" maxlength="180" required></label>
            <p class="hint">Si varias versiones del plan comparten el curso, la asignación se muestra en todas ellas.</p>
            <div class="actions"><button type="submit" [disabled]="busy() || teacherForm.invalid || !form.name.trim()">{{ editingId() ? 'Guardar cambios' : 'Asignar docente' }}</button>
              @if (editingId()) { <button type="button" class="quiet" (click)="reset()">Cancelar</button> }
            </div>
          </form>
        </article>
        <article class="card"><div class="card-head"><span class="icon-badge teal">{{ teachers().length }}</span><div><h2>Directorio y asignaciones</h2><p>Filtra por docente, escuela o curso y administra sus asignaciones.</p></div></div>
          <div class="filters">
            <label>Buscar<input name="search" [(ngModel)]="search" (ngModelChange)="filter()" placeholder="Nombre del docente"></label>
            <label>Escuela<select name="listSchool" [(ngModel)]="listSchool" (ngModelChange)="listCourse=''; filter()"><option value="">Todas</option>
              @for (school of schools(); track school) { <option [value]="school">{{ school }}</option> }</select></label>
            <label>Curso<select name="listCourse" [(ngModel)]="listCourse" (ngModelChange)="filter()"><option value="">Todos</option>
              @for (course of courseNamesForFilter(); track course) { <option [value]="course">{{ course }}</option> }</select></label>
          </div>
          @if (filterLoading()) { <p class="loading-state" role="status">Buscando asignaciones…</p> }
          <div class="list">@for (teacher of pagedTeachers(); track teacher.id) {
            <div class="teacher"><div><strong>{{ teacher.name }}</strong><small>{{ teacher.course }} · {{ teacher.school }}</small></div>
              <div class="actions"><button type="button" class="quiet" (click)="edit(teacher)">Editar</button><button type="button" class="danger" [disabled]="busy()" (click)="remove(teacher)">Eliminar</button></div></div>
          } @empty { @if (!loading() && !filterLoading()) { <p class="empty-state">No hay asignaciones con esos filtros.</p> } }</div>
          @if (teachers().length > pageSize) { <nav class="pager" aria-label="Páginas de docentes"><button type="button" (click)="page.set(page()-1)" [disabled]="page()===0">Anterior</button>
            <span>{{ teachers().length }} asignaciones · Página {{ page()+1 }} de {{ pages() }}</span><button type="button" (click)="page.set(page()+1)" [disabled]="page()+1>=pages()">Siguiente</button></nav> }
        </article>
      </section>
    </main>`,
  styleUrl: './admin-module.css'
})
export class TeacherOrganizationComponent implements OnInit {
  private readonly api = inject(FiaApiService);
  readonly schools = signal<string[]>([]);
  readonly allCourses = signal<Curso[]>([]);
  readonly courses = signal<Curso[]>([]);
  readonly teachers = signal<Teacher[]>([]);
  readonly busy = signal(false);
  readonly loading = signal(false);
  readonly filterLoading = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly editingId = signal<number | null>(null);
  readonly page = signal(0);
  readonly pageSize = 8;
  private filterSequence = 0;
  search = '';
  listSchool = '';
  listCourse = '';
  form = { name: '', course: '', school: '' };

  async ngOnInit(): Promise<void> {
    this.loading.set(true);
    try {
      const plans: Plan[] = await this.api.planes();
      const courses = (await Promise.all(plans.map(p => this.api.cursos(p.id)))).flat();
      this.allCourses.set(courses);
      this.schools.set([...new Set(plans.map(p => p.school))].sort());
      await this.filter();
    } catch (error) { this.fail(error); } finally { this.loading.set(false); }
  }
  pages(): number { return Math.ceil(this.teachers().length/this.pageSize); }
  pagedTeachers(): Teacher[] { return this.teachers().slice(this.page()*this.pageSize,(this.page()+1)*this.pageSize); }
  courseNamesForFilter(): string[] {
    return [...new Set(this.allCourses().filter(c => !this.listSchool || c.school === this.listSchool).map(c => c.name))].sort();
  }
  schoolChanged(): void {
    this.form.course = '';
    const unique = new Map<string, Curso>();
    for (const course of this.allCourses().filter(c => c.school === this.form.school)) unique.set(course.name, course);
    this.courses.set([...unique.values()].sort((a, b) => a.cycle - b.cycle || a.name.localeCompare(b.name)));
  }
  async filter(): Promise<void> {
    const request=++this.filterSequence;this.filterLoading.set(true);this.page.set(0);
    try { const teachers=await this.api.docentes(this.search.trim(), this.listSchool, this.listCourse);if(request===this.filterSequence)this.teachers.set(teachers); }
    catch (error) { if(request===this.filterSequence)this.fail(error); }
    finally { if(request===this.filterSequence)this.filterLoading.set(false); }
  }
  edit(teacher: Teacher): void {
    this.editingId.set(teacher.id);
    this.form = { name: teacher.name, course: '', school: teacher.school };
    this.schoolChanged();
    this.form.course = teacher.course;
  }
  reset(): void { this.editingId.set(null); this.form = { name: '', course: '', school: '' }; this.courses.set([]); }
  async save(): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true); this.error.set(''); this.notice.set('');
    try {
      const id = this.editingId();
      if (id) await this.api.editarDocente(id, this.form); else await this.api.crearDocente(this.form);
      this.reset(); await this.filter(); this.notice.set('Asignación docente guardada.');
    } catch (error) { this.fail(error); } finally { this.busy.set(false); }
  }
  async remove(teacher: Teacher): Promise<void> {
    if(this.busy())return;
    if (!confirm(`¿Eliminar la asignación de ${teacher.name} a ${teacher.course}?`)) return;
    this.busy.set(true);this.error.set('');this.notice.set('');
    try { await this.api.borrarDocente(teacher.id); await this.filter(); this.notice.set('Asignación eliminada.'); }
    catch (error) { this.fail(error); } finally { this.busy.set(false); }
  }
  private fail(error: unknown): void { this.error.set(this.api.mensaje(error)); }
}

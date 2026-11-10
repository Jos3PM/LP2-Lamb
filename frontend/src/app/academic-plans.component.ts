import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FiaApiService } from './fia-api.service';
import { Curso, Plan } from './models';

type ScheduleBlock = { day: string; start: string; end: string };

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <main class="module"><header><div><a routerLink="/">← Volver al portal</a><p class="eyebrow">DISEÑO CURRICULAR</p>
      <h1>Planes académicos</h1><p>Crea planes futuros y configura sus cursos, horarios y cupos.</p></div>
      <button type="button" (click)="newPlan()">Nuevo plan</button></header>
      @if (error()) { <div class="error" role="alert">{{ error() }}</div> }
      @if (notice()) { <div class="notice" role="status">{{ notice() }}</div> }
      @if (loadingPlans()) { <p class="loading-state" role="status">Cargando planes académicos…</p> }
      <section class="columns"><article class="card"><h2>{{ editingPlanId() ? 'Editar plan' : 'Crear plan' }}</h2>
        <form (ngSubmit)="savePlan()"><label>Escuela / carrera<input name="school" [(ngModel)]="planForm.school" maxlength="150" required></label>
          <div class="row"><label>Versión / periodo<input name="version" [(ngModel)]="planForm.version" maxlength="20" required></label>
            <label>Variante<input name="variant" [(ngModel)]="planForm.variant" maxlength="30" required></label></div>
          <label>Fuente<input name="source" [(ngModel)]="planForm.source" maxlength="300"></label>
          <div class="preview"><strong>Resumen antes de guardar</strong><p>{{ planForm.school || 'Escuela pendiente' }} · {{ planForm.version || 'Versión pendiente' }} · {{ planForm.variant }}</p>
            <p>{{ selectedPlan() && editingPlanId()===selectedPlan()?.id ? courses().length : 0 }} cursos registrados.</p></div>
          <button type="submit" [disabled]="busy()">{{ editingPlanId() ? 'Guardar plan' : 'Crear plan' }}</button></form>
        <h2>Planes registrados</h2><label>Buscar plan<input name="planSearch" [(ngModel)]="planSearch" placeholder="Escuela o versión"></label>
        <div class="list">@for (plan of visiblePlans(); track plan.id) {
          <div class="item" [class.selected]="selectedPlan()?.id===plan.id"><button type="button" class="select" (click)="selectPlan(plan)">
            <strong>{{ plan.school }}</strong><small>{{ plan.version }} · {{ plan.variant }}</small></button>
            <button type="button" class="quiet" (click)="editPlan(plan)">Editar</button>
            <button type="button" class="danger" (click)="deletePlan(plan)">Eliminar</button></div>
        } @empty { @if (!loadingPlans()) { <p class="empty-state">No hay planes con ese filtro.</p> } }</div>
      </article>
      <article class="card">@if (selectedPlan(); as plan) {
        <h2>Cursos de {{ plan.school }} · {{ plan.version }}</h2>
        @if (loadingCourses()) { <p class="loading-state" role="status">Cargando cursos y cupos…</p> }
        <div class="preview"><strong>Resumen del plan</strong><p>{{ courses().length }} cursos · {{ totalCredits() }} créditos · {{ totalHours() }} horas</p>
          <p>@for (row of summaryByCycle(); track row.cycle) { <span>Ciclo {{ row.cycle }}: {{ row.courses }} cursos / {{ row.credits }} créditos · </span> }@empty { Aún no hay cursos. }</p></div>
        <div class="toolbar"><label>Mostrar ciclo<select name="cycleFilter" [(ngModel)]="cycleFilter"><option [ngValue]="null">Todos</option>
          @for (cycle of cycles(); track cycle) { <option [ngValue]="cycle">Ciclo {{ cycle }}</option> }</select></label>
          <label>Periodo para cupos<input name="period" [(ngModel)]="period" (ngModelChange)="loadSeats()" placeholder="2027-1"></label>
          <button type="button" class="quiet" [disabled]="busy()" (click)="generateSchedules()">Proponer horarios faltantes</button></div>
        <h3>{{ editingCourseId() ? 'Editar curso' : 'Nuevo curso' }}</h3>
        <form (ngSubmit)="saveCourse()"><div class="row"><label>Ciclo<input type="number" name="cycle" [(ngModel)]="courseForm.cycle" min="1" max="10" required></label>
          <label>Créditos<input type="number" name="credits" [(ngModel)]="courseForm.credits" min="0" required></label></div>
          <label>Nombre<input name="name" [(ngModel)]="courseForm.name" maxlength="300" required></label>
          <div class="row"><label>HT<input type="number" name="ht" [(ngModel)]="courseForm.ht" min="0" required></label>
            <label>HP<input type="number" name="hp" [(ngModel)]="courseForm.hp" min="0" required></label>
            <label>HNP<input type="number" name="hnp" [(ngModel)]="courseForm.hnp" min="0" required></label></div>
          <label>Prerrequisitos<input name="prerequisites" [(ngModel)]="courseForm.prerequisites" maxlength="3000" list="plan-prerequisites" placeholder="Nombre exacto del curso previo o Ninguno"></label>
          <datalist id="plan-prerequisites">@for (candidate of courses(); track candidate.id) { <option [value]="candidate.name"></option> }</datalist>
          <fieldset class="schedule-panel" [attr.aria-describedby]="scheduleIssue() || scheduleError() ? 'schedule-error' : 'schedule-help'">
            <legend>Horario del curso</legend>
            <p id="schedule-help" class="hint">Marca uno o varios bloques en la tabla. Puedes ajustar las horas exactas debajo. Si no marcas ninguno, el sistema propondrá un horario al guardar.</p>
            <div class="schedule-grid-wrap"><table class="schedule-grid">
              <caption>Selecciona bloques de una hora por día</caption>
              <thead><tr><th scope="col">Hora</th>@for (day of days; track day) { <th scope="col">{{ day }}</th> }</tr></thead>
              <tbody>@for (start of timeSlots; track start) { <tr><th scope="row">{{ start }}</th>
                @for (day of days; track day) { <td><button type="button" class="slot-button" [class.selected]="slotSelected(day,start)" [class.covered]="slotCovered(day,start)"
                  [attr.aria-pressed]="slotCovered(day,start)" [attr.aria-label]="slotLabel(day,start)" (click)="toggleSlot(day,start)">
                  {{ slotCovered(day,start) ? '✓' : '+' }}</button></td> }
              </tr> }</tbody>
            </table></div>
            <div class="schedule-controls"><strong>Bloques seleccionados ({{ scheduleBlocks.length }})</strong>
              <button type="button" class="quiet" (click)="addBlock('Lun')">Añadir bloque con horas precisas</button></div>
            @for (block of scheduleBlocks; track block; let index = $index) {
              <div class="schedule-block"><label>Día<select [name]="'day-' + index" [(ngModel)]="block.day" (ngModelChange)="scheduleError.set('')">
                @for (day of days; track day) { <option [value]="day">{{ day }}</option> }
              </select></label>
              <label>Inicio<input type="time" [name]="'start-' + index" [(ngModel)]="block.start" (ngModelChange)="scheduleError.set('')" required></label>
              <label>Fin<input type="time" [name]="'end-' + index" [(ngModel)]="block.end" (ngModelChange)="scheduleError.set('')" required></label>
              <button type="button" class="quiet remove-block" (click)="removeBlock(index)" [attr.aria-label]="'Quitar bloque ' + (index + 1)">Quitar</button></div>
            } @empty { <p class="schedule-empty">Sin bloques seleccionados. Se propondrá un horario automáticamente.</p> }
            <p class="schedule-summary"><strong>Resumen:</strong> {{ scheduleSummary() || 'Horario por proponer' }}</p>
            @if (scheduleIssue() || scheduleError()) { <p id="schedule-error" class="field-error" role="alert">{{ scheduleIssue() || scheduleError() }}</p> }
          </fieldset>
          <label>Cupos totales<input type="number" name="cupos" [(ngModel)]="courseForm.cupos" min="1" required></label>
          <div class="preview"><strong>Curso por guardar</strong><p>Ciclo {{ courseForm.cycle }} · {{ courseForm.name || 'Nombre pendiente' }} · {{ courseForm.credits }} créditos · {{ draftHours() }} horas · {{ courseForm.cupos }} cupos</p></div>
          <div class="actions"><button type="submit" [disabled]="busy()">{{ editingCourseId() ? 'Guardar curso' : 'Añadir curso' }}</button>
            @if (editingCourseId()) { <button type="button" class="quiet" (click)="newCourse()">Cancelar edición</button> }</div></form>
        <h3>Catálogo · {{ visibleCourses().length }} cursos</h3><div class="list courses">@for (course of visibleCourses(); track course.id) {
          <div class="item"><div class="description"><strong>Ciclo {{ course.cycle }} · {{ course.name }}</strong>
            <small>{{ course.credits }} créditos · HT {{ course.ht }} · HP {{ course.hp }} · HNP {{ course.hnp }}</small>
            <small>{{ course.horario || 'Sin horario' }} · {{ course.cupos }} cupos totales · {{ remaining(course) }} disponibles en {{ period || 'el periodo' }}</small>
            <small>Prerrequisito: {{ course.prerequisites || 'Ninguno' }} · Docente: {{ course.teachers?.[0]?.name || 'Pendiente' }}</small></div>
            <div class="actions"><button type="button" class="quiet" (click)="editCourse(course)">Editar</button>
              <button type="button" class="quiet" (click)="increaseSeats(course)">+ Cupos</button>
              <button type="button" class="danger" (click)="deleteCourse(course)">Eliminar</button></div></div>
        } @empty { <p class="empty-state">Este ciclo aún no tiene cursos.</p> }</div>
      } @else { <h2>Selecciona un plan</h2><p>Elige un plan de la lista para configurar sus cursos.</p> }</article></section></main>`,
  styleUrls: ['./admin-module.css', './academic-plans.component.css']
})
export class AcademicPlansComponent implements OnInit {
  private readonly api = inject(FiaApiService);
  readonly plans = signal<Plan[]>([]);
  readonly courses = signal<Curso[]>([]);
  readonly selectedPlan = signal<Plan | null>(null);
  readonly remainingSeats = signal<Record<number, number>>({});
  readonly busy = signal(false);
  readonly loadingPlans = signal(false);
  readonly loadingCourses = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly editingPlanId = signal<number | null>(null);
  readonly editingCourseId = signal<number | null>(null);
  readonly scheduleError = signal('');
  readonly days = ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom'];
  readonly timeSlots = Array.from({length:14},(_,hour)=>`${String(hour+7).padStart(2,'0')}:00`);
  scheduleBlocks: ScheduleBlock[] = [];
  planSearch = '';
  cycleFilter: number | null = null;
  period = '';
  planForm = { school: '', version: '', variant: 'Regular', source: 'Registro administrativo' };
  courseForm = { cycle: 1, name: '', credits: 0, ht: 0, hp: 0, hnp: 0, prerequisites: '', horario: '', cupos: 30 };

  async ngOnInit(): Promise<void> { await this.refreshPlans(); }
  visiblePlans(): Plan[] { const q=this.planSearch.toLowerCase().trim(); return this.plans().filter(p=>(p.school+' '+p.version).toLowerCase().includes(q)); }
  visibleCourses(): Curso[] { return this.courses().filter(c=>this.cycleFilter===null||c.cycle===this.cycleFilter); }
  cycles(): number[] { return [...new Set(this.courses().map(c=>c.cycle))].sort((a,b)=>a-b); }
  totalCredits(): number { return this.courses().reduce((sum,c)=>sum+c.credits,0); }
  totalHours(): number { return this.courses().reduce((sum,c)=>sum+c.total,0); }
  draftHours(): number { return Number(this.courseForm.ht)+Number(this.courseForm.hp)+Number(this.courseForm.hnp); }
  addBlock(day: string): void {
    const start=this.timeSlots.find(slot=>!this.slotCovered(day,slot));
    if(!start){this.scheduleError.set(`No queda un bloque libre el ${day} en la tabla. Ajusta uno de los bloques existentes.`);return;}
    this.scheduleBlocks.push({day,start,end:this.slotEnd(start)});this.scheduleError.set('');
  }
  slotEnd(start:string):string { return `${String(Number(start.slice(0,2))+1).padStart(2,'0')}:00`; }
  slotSelected(day:string,start:string):boolean { return this.scheduleBlocks.some(b=>b.day===day&&b.start===start&&b.end===this.slotEnd(start)); }
  slotCovered(day:string,start:string):boolean { const end=this.slotEnd(start);return this.scheduleBlocks.some(b=>b.day===day&&b.start<end&&start<b.end); }
  slotLabel(day:string,start:string):string { const action=this.slotSelected(day,start)?'Quitar':this.slotCovered(day,start)?'Revisar':'Seleccionar';return `${action} ${day} ${start}–${this.slotEnd(start)}`; }
  toggleSlot(day:string,start:string):void {
    const end=this.slotEnd(start),index=this.scheduleBlocks.findIndex(b=>b.day===day&&b.start===start&&b.end===end);
    if(index>=0){this.removeBlock(index);return;}
    if(this.slotCovered(day,start)){this.scheduleError.set(`El bloque ${day} ${start}–${end} coincide con un bloque seleccionado. Ajusta o quita ese bloque en la lista.`);return;}
    this.scheduleBlocks.push({day,start,end});this.scheduleError.set('');
  }
  removeBlock(index: number): void { this.scheduleBlocks.splice(index,1); this.scheduleError.set(''); }
  scheduleSummary(): string { return this.scheduleBlocks.map(b=>`${b.day} ${b.start}–${b.end}`).join('; '); }
  scheduleIssue(): string {
    for (const block of this.scheduleBlocks) {
      if (!block.start || !block.end) return 'Completa la hora de inicio y fin de cada bloque.';
      if (block.end <= block.start) return `En ${block.day}, la hora de fin debe ser posterior al inicio.`;
    }
    for (let i=0;i<this.scheduleBlocks.length;i++) for(let j=i+1;j<this.scheduleBlocks.length;j++) {
      const a=this.scheduleBlocks[i],b=this.scheduleBlocks[j];
      if(a.day===b.day && a.start<b.end && b.start<a.end) return `Hay bloques repetidos o superpuestos el ${a.day}.`;
    }
    return '';
  }
  private loadSchedule(value: string | null): void {
    this.scheduleBlocks=[];this.scheduleError.set('');
    if(!value?.trim())return;
    for(const part of value.split(';')) {
      const match=/^\s*(Lun|Mar|Mie|Jue|Vie|Sab|Dom) ([0-2]\d:[0-5]\d)-([0-2]\d:[0-5]\d)\s*$/i.exec(part);
      if(!match){this.scheduleError.set('El horario guardado no tiene el formato esperado. Revísalo antes de guardar.');return;}
      this.scheduleBlocks.push({day:this.days.find(d=>d.toLowerCase()===match[1].toLowerCase())!,start:match[2],end:match[3]});
    }
  }
  summaryByCycle(): {cycle:number;courses:number;credits:number}[] {
    return this.cycles().map(cycle=>({cycle,courses:this.courses().filter(c=>c.cycle===cycle).length,credits:this.courses().filter(c=>c.cycle===cycle).reduce((sum,c)=>sum+c.credits,0)}));
  }
  remaining(course: Curso): number { return this.period ? this.remainingSeats()[course.id] ?? course.cupos : course.cupos; }
  async refreshPlans(): Promise<void> { this.loadingPlans.set(true);try { this.plans.set(await this.api.planes()); } catch(error) { this.fail(error); } finally { this.loadingPlans.set(false); } }
  newPlan(): void { this.editingPlanId.set(null); this.planForm={school:'',version:'',variant:'Regular',source:'Registro administrativo'}; }
  editPlan(plan: Plan): void { this.editingPlanId.set(plan.id); this.planForm={school:plan.school,version:plan.version,variant:plan.variant,source:plan.source}; void this.selectPlan(plan); }
  async selectPlan(plan: Plan): Promise<void> {
    this.selectedPlan.set(plan); this.period=plan.version; this.cycleFilter=null; this.newCourse();
    this.loadingCourses.set(true);
    try { this.courses.set(await this.api.cursos(plan.id)); await this.loadSeats(); } catch(error) { this.fail(error); }
    finally { this.loadingCourses.set(false); }
  }
  async savePlan(): Promise<void> {
    await this.run(async()=>{
      const id=this.editingPlanId();
      const saved=id ? await this.api.editarPlan(id,this.planForm) : await this.api.crearPlan(this.planForm);
      await this.refreshPlans(); await this.selectPlan(saved); this.editingPlanId.set(saved.id);
      this.notice.set('Plan guardado. Ya puedes administrar sus cursos.');
    });
  }
  async deletePlan(plan: Plan): Promise<void> {
    if(!confirm(`¿Eliminar el plan ${plan.school} ${plan.version}? Solo se permite si no tiene cursos ni matrículas vinculadas.`)) return;
    await this.run(async()=>{ await this.api.borrarPlan(plan.id); if(this.selectedPlan()?.id===plan.id){this.selectedPlan.set(null);this.courses.set([]);} await this.refreshPlans();this.notice.set('Plan eliminado.'); });
  }
  newCourse(): void { this.editingCourseId.set(null); this.courseForm={cycle:1,name:'',credits:0,ht:0,hp:0,hnp:0,prerequisites:'',horario:'',cupos:30}; this.loadSchedule(''); this.error.set(''); }
  editCourse(course: Curso): void { this.editingCourseId.set(course.id); this.courseForm={cycle:course.cycle,name:course.name,credits:course.credits,ht:course.ht,hp:course.hp,hnp:course.hnp,prerequisites:course.prerequisites||'',horario:course.horario||'',cupos:course.cupos}; this.loadSchedule(course.horario); this.error.set(''); }
  async saveCourse(): Promise<void> {
    const plan=this.selectedPlan(); if(!plan) return;
    const issue=this.scheduleIssue() || this.scheduleError();
    if(issue){this.scheduleError.set(issue);return;}
    await this.run(async()=>{
      const payload={...this.courseForm,horario:this.scheduleBlocks.map(b=>`${b.day} ${b.start}-${b.end}`).join('; '),planId:plan.id}; const id=this.editingCourseId();
      try { if(id) await this.api.editarCurso(id,payload); else await this.api.crearCurso(payload); }
      catch(error) { const message=this.api.mensaje(error); if(/horario|cruce|bloque/i.test(message))this.scheduleError.set(message); throw error; }
      this.newCourse(); this.courses.set(await this.api.cursos(plan.id)); await this.loadSeats();
      this.notice.set('Curso guardado con validación de horario.');
    });
  }
  async deleteCourse(course: Curso): Promise<void> {
    if(!confirm(`¿Eliminar ${course.name}? No se permite si ya tiene carga o matrícula vinculada.`)) return;
    await this.run(async()=>{await this.api.borrarCurso(course.id);this.courses.set(await this.api.cursos(this.selectedPlan()!.id));this.notice.set('Curso eliminado.');});
  }
  async generateSchedules(): Promise<void> {
    const plan=this.selectedPlan(); if(!plan) return;
    await this.run(async()=>{const result=await this.api.generarHorariosPlan(plan.id);this.courses.set(await this.api.cursos(plan.id));this.notice.set(`${result.horariosGenerados} horarios propuestos sin modificar los horarios existentes.`);});
  }
  async increaseSeats(course: Curso): Promise<void> {
    const value=prompt(`¿Cuántos cupos añadir a ${course.name}?`, '5'); if(value===null)return;
    const amount=Number(value); if(!Number.isInteger(amount)||amount<1||amount>5000){this.error.set('Ingresa entre 1 y 5000 cupos.');return;}
    await this.run(async()=>{await this.api.aumentarCupos(course.id,amount);this.courses.set(await this.api.cursos(this.selectedPlan()!.id));await this.loadSeats();this.notice.set(`${amount} cupos añadidos a ${course.name}.`);});
  }
  async loadSeats(): Promise<void> {
    const plan=this.selectedPlan(); if(!plan||!this.period.trim()){this.remainingSeats.set({});return;}
    try { this.remainingSeats.set(await this.api.cuposAdmin(plan.id,this.period.trim())); } catch(error) { this.fail(error); }
  }
  private async run(action:()=>Promise<void>):Promise<void>{
    if(this.busy())return; this.busy.set(true);this.error.set('');this.notice.set('');
    try{await action();}catch(error){if(!this.scheduleError())this.fail(error);}finally{this.busy.set(false);}
  }
  private fail(error:unknown):void{this.error.set(this.api.mensaje(error));}
}


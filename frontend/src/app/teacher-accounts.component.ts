import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FiaApiService } from './fia-api.service';
import { HttpFeedbackComponent } from './http-feedback.component';
import { TeacherAccount } from './models';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, HttpFeedbackComponent],
  template: `
    <main class="module"><header><div><a routerLink="/">← Volver al portal</a><p class="eyebrow">ADMINISTRACIÓN</p>
      <h1>Cuentas de profesores</h1><p>Vincula una cuenta a un docente del directorio y controla su acceso.</p></div>
      <button type="button" (click)="reset()">Nueva cuenta</button></header><app-http-feedback />
      @if (error()) { <div class="error" role="alert">{{ error() }}</div> }
      @if (notice()) { <div class="notice" role="status">{{ notice() }}</div> }
      @if (loading()) { <p class="loading-state" role="status">Cargando cuentas y docentes…</p> }
      <section class="columns"><article class="card"><div class="card-head"><span class="icon-badge">P</span><div><h2>{{ editing() ? 'Editar cuenta' : 'Crear cuenta de profesor' }}</h2><p>El acceso queda vinculado a una ficha docente.</p></div></div>
        <form #accountForm="ngForm" (ngSubmit)="save()">
          <label>Buscar docente<input name="teacherQuery" [(ngModel)]="teacherQuery" placeholder="Nombre, escuela o curso" autocomplete="off"></label>
          <label>Docente<select name="docenteId" [(ngModel)]="form.docenteId" [disabled]="!!editing()" required>
            <option [ngValue]="null">Selecciona un docente</option>
            @for (teacher of availableTeachers(); track teacher.id) { <option [ngValue]="teacher.id">{{ teacher.name }} · {{ teacher.school }} · {{ teacher.course }}</option> }
          </select></label>
          <label>Usuario<input name="username" [(ngModel)]="form.username" [disabled]="!!editing()" required minlength="4" maxlength="80" pattern="[A-Za-z0-9._-]+" autocomplete="off"></label>
          <label>Código de profesor<input name="codigo" [(ngModel)]="form.codigo" required minlength="2" maxlength="30" pattern="[A-Za-z0-9-]+"></label>
          <label>Correo<input name="correo" [(ngModel)]="form.correo" type="email" required maxlength="180"></label>
          <label>{{ editing() ? 'Nueva contraseña (opcional)' : 'Contraseña temporal' }}<input name="password" [(ngModel)]="form.password" type="password" [required]="!editing()" minlength="12" maxlength="72" autocomplete="new-password"></label>
          @if (editing()) { <label class="toggle"><input type="checkbox" name="activo" [(ngModel)]="form.activo"> Cuenta activa</label> }
          <p class="hint">El docente solo podrá consultar su perfil y sus cargas. Comparte la contraseña por un canal privado.</p>
          <div class="actions"><button type="submit" [disabled]="busy() || accountForm.invalid">{{ editing() ? 'Guardar cambios' : 'Crear cuenta' }}</button>
            @if (editing()) { <button type="button" class="quiet" (click)="reset()">Cancelar edición</button> }</div>
        </form>
      </article><article class="card"><div class="card-head"><span class="icon-badge teal">{{ visibleAccounts().length }}</span><div><h2>Accesos registrados</h2><p>Busca, edita o cambia el estado de una cuenta.</p></div></div>
        <div class="filters"><label>Buscar cuenta<input name="buscar" [(ngModel)]="search" (ngModelChange)="page.set(0)" placeholder="Docente, usuario, código o correo"></label>
          <label>Estado<select name="estado" [(ngModel)]="statusFilter" (ngModelChange)="page.set(0)"><option value="all">Todas</option><option value="active">Activas</option><option value="inactive">Desactivadas</option></select></label></div>
        <div class="list">@for (account of pagedAccounts(); track account.username) {
          <div class="item"><div><strong>{{ account.docente }}</strong><small>{{ account.username }} · {{ account.codigo }} · {{ account.correo }}</small>
            <small><span class="badge" [class.inactive]="!account.activo">{{ account.activo ? 'Activa' : 'Desactivada' }}</span> · Modificada por {{ account.modificadoPor }}</small></div>
            <div class="actions"><button type="button" class="quiet" (click)="edit(account)">Editar</button>
              <button type="button" [disabled]="busy()" [class.danger]="account.activo" [class.quiet]="!account.activo" (click)="toggle(account)">{{ account.activo ? 'Desactivar' : 'Activar' }}</button></div></div>
        } @empty { @if (!loading()) { <p class="empty-state">No hay cuentas con esos filtros.</p> } }</div>
        @if (visibleAccounts().length > pageSize) { <nav class="pager" aria-label="Páginas de cuentas de profesores"><button type="button" (click)="page.set(page()-1)" [disabled]="page()===0">Anterior</button>
          <span>{{ visibleAccounts().length }} cuentas · Página {{ page()+1 }} de {{ pages() }}</span><button type="button" (click)="page.set(page()+1)" [disabled]="page()+1>=pages()">Siguiente</button></nav> }
      </article></section></main>`,
  styleUrl: './admin-module.css'
})
export class TeacherAccountsComponent implements OnInit {
  private readonly api = inject(FiaApiService);
  readonly accounts = signal<TeacherAccount[]>([]);
  readonly teachers = signal<{id:number;name:string;school:string;course:string}[]>([]);
  readonly editing = signal<string | null>(null);
  readonly busy = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly page = signal(0);
  readonly pageSize = 8;
  search = '';
  statusFilter = 'all';
  teacherQuery = '';
  form: {username:string;password:string;docenteId:number|null;codigo:string;correo:string;activo:boolean} =
    {username:'',password:'',docenteId:null,codigo:'',correo:'',activo:true};

  async ngOnInit(): Promise<void> { await this.refresh(); }
  async refresh(): Promise<void> {
    this.loading.set(true);
    try { const [accounts,teachers] = await Promise.all([this.api.cuentasProfesores(),this.api.docentes()]); this.accounts.set(accounts); this.teachers.set(teachers); this.page.set(Math.min(this.page(),Math.max(0,this.pages()-1))); }
    catch(error) { this.fail(error); } finally { this.loading.set(false); }
  }
  availableTeachers() { const used = new Set(this.accounts().filter(a=>a.username!==this.editing()).map(a=>a.docenteId)); const q=this.teacherQuery.trim().toLowerCase();return this.teachers().filter(t=>!used.has(t.id)&&`${t.name} ${t.school} ${t.course}`.toLowerCase().includes(q)); }
  visibleAccounts(): TeacherAccount[] { const q=this.search.trim().toLowerCase(); return this.accounts().filter(a=>`${a.docente} ${a.username} ${a.codigo} ${a.correo}`.toLowerCase().includes(q)&&(this.statusFilter==='all'||(this.statusFilter==='active')===a.activo)); }
  pages(): number { return Math.ceil(this.visibleAccounts().length/this.pageSize); }
  pagedAccounts(): TeacherAccount[] { return this.visibleAccounts().slice(this.page()*this.pageSize,(this.page()+1)*this.pageSize); }
  reset(): void { this.editing.set(null); this.teacherQuery='';this.form={username:'',password:'',docenteId:null,codigo:'',correo:'',activo:true}; }
  edit(account: TeacherAccount): void {
    this.editing.set(account.username);this.teacherQuery='';
    this.form={username:account.username,password:'',docenteId:account.docenteId,codigo:account.codigo,correo:account.correo,activo:account.activo};
  }
  async save(): Promise<void> {
    if(this.busy()||!this.form.docenteId)return; this.busy.set(true);this.error.set('');this.notice.set('');
    try {
      const editing=this.editing();
      if(editing) await this.api.editarCuentaProfesor(editing,{codigo:this.form.codigo,correo:this.form.correo,password:this.form.password||null,activo:this.form.activo});
      else await this.api.crearCuentaProfesor({username:this.form.username,password:this.form.password,docenteId:this.form.docenteId,codigo:this.form.codigo,correo:this.form.correo});
      this.reset();await this.refresh();this.notice.set('Cuenta de profesor guardada.');
    } catch(error) { this.fail(error); } finally { this.busy.set(false); }
  }
  async toggle(account: TeacherAccount): Promise<void> {
    if(this.busy())return;
    if(!confirm(`${account.activo?'Desactivar':'Activar'} la cuenta de ${account.docente}?`))return;
    this.busy.set(true);this.error.set('');this.notice.set('');
    try {await this.api.editarCuentaProfesor(account.username,{codigo:account.codigo,correo:account.correo,password:null,activo:!account.activo});await this.refresh();this.notice.set(`Cuenta ${account.activo?'desactivada':'activada'}.`);}
    catch(error){this.fail(error);}finally{this.busy.set(false);}
  }
  private fail(error:unknown):void{this.error.set(this.api.mensaje(error));}
}

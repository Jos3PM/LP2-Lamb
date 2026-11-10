import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FiaApiService } from './fia-api.service';
import { HttpFeedbackComponent } from './http-feedback.component';
import { TeacherPortalComponent } from './teacher-portal.component';
import { Curso, Eleccion, Estudiante, Matricula, Pago, PagoPendiente, Paso, Plan, SolicitudMatricula, Tarifa } from './models';

@Component({ selector: 'app-portal', imports: [CommonModule, FormsModule, RouterLink, HttpFeedbackComponent, TeacherPortalComponent], templateUrl: './app.html', styleUrl: './app.css' })
export class App implements OnInit {
  private readonly api = inject(FiaApiService);
  readonly pasos: { id: Paso; numero: string; titulo: string; subtitulo: string }[] = [
    { id: 'usuarios', numero: '00', titulo: 'Usuarios alumnos', subtitulo: 'Crear credenciales de acceso' },
    { id: 'eleccion', numero: '01', titulo: 'Elección de carrera', subtitulo: 'Estudiante, plan y periodo' },
    { id: 'tarifas', numero: '02', titulo: 'Configuración de tarifas', subtitulo: 'Base, crédito y moneda' },
    { id: 'cursos', numero: '03', titulo: 'Selección de cursos', subtitulo: 'Cursos del plan elegido' },
    { id: 'generar', numero: '04', titulo: 'Generar matrícula', subtitulo: 'Revisar solicitudes y emitir' },
    { id: 'pagos', numero: '05', titulo: 'Registro de pagos', subtitulo: 'Abonos y saldo' },
    { id: 'consulta', numero: '06', titulo: 'Consulta de matrícula', subtitulo: 'Cursos e historial' },
  ];
  readonly paso = signal<Paso>('usuarios');
  readonly loading = signal(true);
  readonly authChecked = signal(false);
  readonly authenticated = signal(false);
  readonly role = signal('ADMIN');
  readonly username = signal('');
  readonly busy = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly perfil = signal('');
  readonly plans = signal<Plan[]>([]);
  readonly students = signal<Estudiante[]>([]);
  readonly studentPage=signal(0);readonly studentPages=signal(0);readonly studentTotal=signal(0);
  readonly choices = signal<Eleccion[]>([]);
  readonly tariffs = signal<Tarifa[]>([]);
  readonly enrollments = signal<Matricula[]>([]);
  readonly enrollmentPage=signal(0);readonly enrollmentPages=signal(0);readonly enrollmentTotal=signal(0);
  readonly enrollmentDetail=signal<Matricula|null>(null);
  readonly courses = signal<Curso[]>([]);
  readonly payments = signal<Pago[]>([]);
  readonly choiceId = signal<number | null>(null);
  readonly enrollmentId = signal<number | null>(null);
  readonly courseIds = signal<number[]>([]);
  readonly courseQuery = signal('');
  readonly courseCycle = signal<number | null>(null);
  readonly enrollmentQuery = signal('');
  readonly studentChoiceId = signal<number|null>(null);
  readonly studentStep = signal<1|2|3>(1);
  readonly studentAgreement = signal(false);
  readonly studentApproved = signal<number[]>([]);
  readonly studentCourseTab = signal<'current'|'other'>('current');
  readonly studentCourseQuery = signal('');
  readonly studentCurrentCycle = computed(()=>{const courses=this.studentCourses();const approved=this.studentApproved();return [...new Set(courses.map(c=>c.cycle))].sort((a,b)=>a-b).find(cycle=>courses.some(c=>c.cycle===cycle&&!approved.includes(c.id)))??courses[0]?.cycle??1;});
  readonly visibleStudentCourses = computed(()=>this.studentCourses().filter(c=>(this.studentCourseTab()==='current'?c.cycle===this.studentCurrentCycle():c.cycle!==this.studentCurrentCycle())&&c.name.toLowerCase().includes(this.studentCourseQuery().toLowerCase().trim())));
  readonly selectedStudentCourses = computed(()=>this.studentCourses().filter(c=>this.studentSelected().includes(c.id)));
  readonly studentProfileReady = computed(()=>!!this.studentProfile()?.correo&&!!this.studentProfile()?.telefono&&!!this.studentProfile()?.apoderado);
  readonly scheduleDays=['Lun','Mar','Mie','Jue','Vie','Sab'];
  readonly scheduleHours=[8,9,10,11,12,13,14,15,16,17,18,19];
  readonly studentPayments = signal<Pago[]>([]);
  readonly studentPaymentPending = computed(()=>this.studentPayments().some(p=>p.estado==='PENDIENTE_VERIFICACION'));
  readonly confirmedStudentPayments = computed(()=>this.studentPayments().filter(p=>p.estado==='CONFIRMADO'));
  readonly studentVoucher = signal<string|null>(null);
  readonly studentVoucherName = signal('');
  readonly pendingAdminPayments = signal<PagoPendiente[]>([]);
  readonly adminVoucherUrl = signal<string|null>(null);
  readonly adminVoucherPaymentId = signal<number|null>(null);
  adminRejectReasons:Record<number,string>={};
  readonly studentReceipt = signal<Record<string,unknown>|null>(null);
  studentPaymentMode:'CONTADO'|'CUOTAS'='CONTADO';
  studentInstallments=3;
  readonly adminStudentCode=signal('');readonly adminStudentName=signal('');
  readonly adminSelectedStudentId=signal<number|null>(null);
  readonly adminStudentResults=computed(()=>this.students());
  readonly adminSelectedStudent=computed(()=>this.students().find(s=>s.id===this.adminSelectedStudentId())??null);
  async searchAdminStudents(pagina=0):Promise<void>{try{const page=await this.api.estudiantesPagina((this.adminStudentCode()||this.adminStudentName()).trim(),pagina);this.students.set(page.items);this.studentPage.set(page.pagina);this.studentPages.set(page.paginas);this.studentTotal.set(page.total);}catch(error){this.error.set(this.api.mensaje(error));}}
  async searchEnrollments(pagina=0):Promise<void>{try{const page=await this.api.matriculasPagina(this.enrollmentQuery().trim(),pagina);this.enrollments.set(page.items);this.enrollmentPage.set(page.pagina);this.enrollmentPages.set(page.paginas);this.enrollmentTotal.set(page.total);}catch(error){this.error.set(this.api.mensaje(error));}}
  readonly adminStudentChoices=computed(()=>this.choices().filter(c=>c.estudianteId===this.adminSelectedStudentId()));
  selectAdminStudent(id:number):void{this.adminSelectedStudentId.set(id);this.choiceForm.estudianteId=id;}
  readonly studentCourses = signal<Curso[]>([]);
  readonly studentSelected = signal<number[]>([]);
  readonly studentRemaining = signal<Record<number,number>>({});
  readonly studentRequests = signal<SolicitudMatricula[]>([]);
  readonly adminRequests = signal<SolicitudMatricula[]>([]);
  readonly adminReviewRequestId = signal<number|null>(null);
  readonly adminReviewCourses = signal<Curso[]>([]);
  readonly studentTariffs = signal<Tarifa[]>([]);
  readonly studentChoice = computed(()=>this.studentCareerChoices().find(c=>c.id===this.studentChoiceId())??null);
  readonly studentCredits = computed(()=>this.studentCourses().filter(c=>this.studentSelected().includes(c.id)).reduce((sum,c)=>sum+c.credits,0));
  readonly studentTariff = computed(()=>this.studentTariffs().find(t=>t.periodo.toLowerCase()===this.studentChoice()?.periodo.toLowerCase())??null);
  readonly studentEstimate = computed(()=>this.studentTariff() ? Number(this.studentTariff()!.base)+Number(this.studentTariff()!.porCredito)*this.studentCredits() : null);
  readonly studentRequest = computed(()=>this.studentRequests().find(r=>r.periodo===this.studentChoice()?.periodo)??null);
  readonly studentHasEnrollment = computed(()=>this.studentEnrollments().some(m=>m.periodo===this.studentChoice()?.periodo));
  readonly studentScheduleConflict = computed(()=>{const cs=this.studentCourses().filter(c=>this.studentSelected().includes(c.id));for(let i=0;i<cs.length;i++)for(let j=i+1;j<cs.length;j++)if(this.schedulesOverlap(cs[i].horario||'',cs[j].horario||''))return `${cs[i].name} y ${cs[j].name}`;return '';});
  readonly studentCareerChoices = signal<Eleccion[]>([]);
  readonly studentCanEditCareer = signal(false);
  readonly studentCareerPlanId = signal<number | null>(null);
  studentCareerPeriod = '';
  readonly managedPlanId = signal<number | null>(null);
  managedCourses = signal<Curso[]>([]);
  readonly adminRemaining=signal<Record<number,number>>({});
  readonly accounts = signal<{username:string;estudianteId:number;role:string;puedeEditarCarrera:boolean}[]>([]);
  readonly studentEnrollments = signal<Matricula[]>([]);
  readonly studentProfile = signal<Estudiante | null>(null);
  readonly activeStudentEnrollment = signal<number | null>(null);
  readonly contract = signal<Record<string,unknown> | null>(null);
  readonly contractEnrollment = computed(() => this.studentEnrollments().find(x=>x.id===this.activeStudentEnrollment()) ?? null);
  readonly editingPlanId = signal<number | null>(null);
  accountEdit = {studentId: null as number|null, username:'', password: '', puedeEditarCarrera:false};
  profileForm = {correo:'',telefono:'',apoderado:''};
  studentPayment = {monto: null as number|null, metodo:'Yape', referencia:''};
  approvedForm = {studentId:null as number|null,planId:null as number|null,courseId:null as number|null};
  approvedCourses = signal<Curso[]>([]);
  async loadApprovedCourses():Promise<void>{this.approvedForm.courseId=null;this.approvedCourses.set(this.approvedForm.planId?await this.api.cursos(this.approvedForm.planId):[]);}
  async approveCourse():Promise<void>{if(!this.approvedForm.studentId||!this.approvedForm.courseId)return;await this.action(async()=>{await this.api.aprobar(this.approvedForm.studentId!,this.approvedForm.courseId!);this.notice.set('Curso aprobado registrado para el alumno.');});}
  teacherForm = {name:'',course:'',school:''};
  private schedulesOverlap(a:string,b:string):boolean{for(const x of a.split(';'))for(const y of b.split(';')){const p=x.trim().split(/\s+/),q=y.trim().split(/\s+/);if(p.length!==2||q.length!==2||p[0].toLowerCase()!==q[0].toLowerCase())continue;const u=p[1].split('-'),v=q[1].split('-');if(u.length===2&&v.length===2&&u[0]<v[1]&&v[0]<u[1])return true;}return false;}
  async selectStudentChoice(id:number|null):Promise<void>{this.studentChoiceId.set(id);this.studentSelected.set([]);this.studentCourseTab.set('current');this.studentCourseQuery.set('');const choice=this.studentChoice();if(!choice){this.studentCourses.set([]);this.studentRemaining.set({});return;}try{const [courses,remaining]=await Promise.all([this.api.cursos(choice.planId),this.api.cuposAlumno(choice.periodo)]);this.studentCourses.set(courses);this.studentRemaining.set(remaining);const request=this.studentRequests().find(r=>r.periodo===choice.periodo);if(request?.estado==='PENDIENTE'||request?.estado==='RECHAZADA')this.studentSelected.set(request.cursos);}catch(error){this.error.set(this.api.mensaje(error));}}
  toggleStudentCourse(id:number,checked:boolean):void{this.studentSelected.update(ids=>checked?[...ids,id]:ids.filter(x=>x!==id));}
  async confirmStudentCourses():Promise<void>{const choice=this.studentChoice();if(!choice||!this.studentSelected().length||this.studentCredits()>25||this.studentScheduleConflict())return;await this.action(async()=>{const enrollment=await this.api.matricularAlumno({periodo:choice.periodo,cursos:this.studentSelected()});this.studentEnrollments.set(await this.api.alumnoMatriculas());this.studentRemaining.set(await this.api.cuposAlumno(choice.periodo));await this.selectStudentEnrollment(enrollment.id);this.studentStep.set(3);this.notice.set(`Matrícula #${enrollment.id} creada y cupos ocupados. Elige cómo pagar.`);});}
  coursesInSlot(day:string,hour:number):Curso[]{return this.selectedStudentCourses().filter(c=>(c.horario||'').split(';').some(slot=>{const parts=slot.trim().split(/\s+/);if(parts.length!==2||parts[0].toLowerCase()!==day.toLowerCase())return false;const times=parts[1].split('-');if(times.length!==2)return false;const start=Number(times[0].slice(0,2))+Number(times[0].slice(3))/60,end=Number(times[1].slice(0,2))+Number(times[1].slice(3))/60;return start<hour+1&&hour<end;}));}
  async saveProfileAndContinue():Promise<void>{if(!this.studentAgreement()){this.error.set('Confirma que revisaste tus datos y el resumen de matrícula.');return;}await this.action(async()=>{this.studentProfile.set(await this.api.guardarPerfilAlumno(this.profileForm));this.studentStep.set(2);this.notice.set('Datos guardados. Ahora selecciona tus cursos y revisa sus horarios.');});}
  async selectStudentEnrollment(id:number):Promise<void>{try{this.activeStudentEnrollment.set(id);this.studentPayments.set(await this.api.pagosAlumno(id));this.studentReceipt.set(null);this.contract.set(null);this.studentVoucher.set(null);this.studentVoucherName.set('');const first=this.studentPayments().find(p=>p.estado!=='RECHAZADO');if(first?.metodo.startsWith('CUOTAS_')){this.studentPaymentMode='CUOTAS';this.studentInstallments=Number(first.metodo.split('|')[0].slice(7))||3;}else if(first&&this.contractEnrollment()?.saldo){this.studentPaymentMode='CUOTAS';this.studentInstallments=Math.max(2,this.confirmedStudentPayments().length+1);}else this.studentPaymentMode='CONTADO';this.prepareStudentPayment();this.studentStep.set(3);}catch(error){this.error.set(this.api.mensaje(error));}}
  prepareStudentPayment():void{const m=this.contractEnrollment();if(!m)return;const remaining=this.studentPaymentMode==='CUOTAS'?Math.max(1,this.studentInstallments-this.confirmedStudentPayments().length):1;this.studentPayment.monto=Number((Number(m.saldo)/remaining).toFixed(2));}
  async onStudentVoucher(event:Event):Promise<void>{const input=event.target as HTMLInputElement;const file=input.files?.[0];this.studentVoucher.set(null);this.studentVoucherName.set('');if(!file)return;if(!['image/jpeg','image/png','image/webp'].includes(file.type)||file.size>5_000_000){this.error.set('Adjunta una foto JPG, PNG o WebP de máximo 5 MB.');input.value='';return;}const reader=new FileReader();reader.onload=()=>{this.studentVoucher.set(String(reader.result));this.studentVoucherName.set(file.name);};reader.onerror=()=>this.error.set('No se pudo leer el voucher.');reader.readAsDataURL(file);}
  async refreshStudentPaymentStatus():Promise<void>{const id=this.activeStudentEnrollment();if(!id)return;await this.action(async()=>{const [enrollments,payments]=await Promise.all([this.api.alumnoMatriculas(),this.api.pagosAlumno(id)]);this.studentEnrollments.set(enrollments);this.studentPayments.set(payments);this.prepareStudentPayment();this.notice.set(this.studentPaymentPending()?'Tu pago sigue pendiente de verificación.':'Estado de pagos actualizado.');});}
  async showStudentReceipt(pagoId:number):Promise<void>{const id=this.activeStudentEnrollment();if(!id)return;await this.action(async()=>{this.studentReceipt.set(await this.api.comprobanteAlumno(id,pagoId));});}
  async refreshAdminPayments():Promise<void>{this.pendingAdminPayments.set(await this.api.pagosPendientes());}
  async viewAdminVoucher(payment:PagoPendiente):Promise<void>{await this.action(async()=>{const blob=await this.api.voucherPago(payment.matriculaId,payment.id);if(this.adminVoucherUrl())URL.revokeObjectURL(this.adminVoucherUrl()!);this.adminVoucherUrl.set(URL.createObjectURL(blob));this.adminVoucherPaymentId.set(payment.id);});}
  async confirmAdminPayment(payment:PagoPendiente):Promise<void>{await this.action(async()=>{await this.api.confirmarPago(payment.matriculaId,payment.id);this.pendingAdminPayments.set(await this.api.pagosPendientes());await this.searchEnrollments(this.enrollmentPage());if(this.enrollmentId()===payment.matriculaId){this.enrollmentDetail.set(await this.api.matricula(payment.matriculaId));this.payments.set(await this.api.pagos(payment.matriculaId));}this.notice.set(`Pago #${payment.id} confirmado. El alumno ya puede ver su boleta y contrato.`);if(this.adminVoucherUrl()){URL.revokeObjectURL(this.adminVoucherUrl()!);this.adminVoucherUrl.set(null);}this.adminVoucherPaymentId.set(null);});}
  async rejectAdminPayment(payment:PagoPendiente):Promise<void>{if(!this.adminRejectReasons[payment.id]?.trim()){this.error.set('Escribe el motivo del rechazo para informar al alumno.');return;}await this.action(async()=>{await this.api.rechazarPago(payment.matriculaId,payment.id,this.adminRejectReasons[payment.id].trim());this.pendingAdminPayments.set(await this.api.pagosPendientes());delete this.adminRejectReasons[payment.id];this.notice.set(`Pago #${payment.id} rechazado. El alumno puede enviar otro voucher.`);if(this.adminVoucherUrl()){URL.revokeObjectURL(this.adminVoucherUrl()!);this.adminVoucherUrl.set(null);}this.adminVoucherPaymentId.set(null);});}
  remainingForStudent(courseId:number):number{const count=this.studentRemaining()[courseId]??this.studentCourses().find(c=>c.id===courseId)?.cupos??0;return count+(this.studentRequest()?.estado==='PENDIENTE'&&this.studentRequest()?.cursos.includes(courseId)?1:0);}
  courseSchedule(courseId:number):string{return this.studentCourses().find(c=>c.id===courseId)?.horario||'Horario disponible en el plan';}
  async reviewRequest(request:SolicitudMatricula):Promise<void>{await this.action(async()=>{const courses=await this.api.cursos(request.planId);this.adminReviewCourses.set(courses.filter(c=>request.cursos.includes(c.id)));this.adminReviewRequestId.set(request.id);});}
  async generateRequest(id:number):Promise<void>{await this.action(async()=>{const m=await this.api.generarSolicitud(id);this.adminRequests.set(await this.api.solicitudesAdmin());await this.searchEnrollments();this.adminReviewRequestId.set(null);await this.selectEnrollment(m.id);this.notice.set(`Matrícula #${m.id} generada desde la solicitud del alumno.`);});}
  async rejectRequest(id:number):Promise<void>{await this.action(async()=>{await this.api.rechazarSolicitud(id);this.adminRequests.set(await this.api.solicitudesAdmin());this.adminReviewRequestId.set(null);this.notice.set('Solicitud rechazada; el alumno puede corregir y reenviar su selección.');});}
  cuposIncrease = 5;
  async generatePlanSchedules():Promise<void>{const id=this.managedPlanId();if(!id)return;await this.action(async()=>{const result=await this.api.generarHorariosPlan(id);this.managedCourses.set(await this.api.cursos(id));this.notice.set(`${result.horariosGenerados} horarios generados para este plan.`);});}
  async generateSchedules():Promise<void>{await this.action(async()=>{const result=await this.api.generarHorarios();if(this.managedPlanId())this.managedCourses.set(await this.api.cursos(this.managedPlanId()!));this.notice.set(`${result.horariosGenerados} horarios referenciales generados en ${result.planesActualizados} planes. Puedes editarlos curso por curso.`);});}
  async increaseSeats(course:Curso):Promise<void>{const amount=Number(this.cuposIncrease);if(!Number.isInteger(amount)||amount<1||amount>5000){this.error.set('Ingresa un aumento entre 1 y 5000 cupos.');return;}await this.action(async()=>{await this.api.aumentarCupos(course.id,amount);if(this.managedPlanId())this.managedCourses.set(await this.api.cursos(this.managedPlanId()!));await this.loadAdminCupos();this.notice.set(`${amount} cupos añadidos a ${course.name}.`);});}
  adminPeriodo = '';
  occupied(courseId:number):number{const course=this.managedCourses().find(c=>c.id===courseId);return course?Math.max(0,course.cupos-(this.adminRemaining()[courseId]??course.cupos)):0;}
  async loadAdminCupos():Promise<void>{const id=this.managedPlanId();if(!id||!this.adminPeriodo.trim()){this.adminRemaining.set({});return;}try{this.adminRemaining.set(await this.api.cuposAdmin(id,this.adminPeriodo.trim()));}catch(error){this.error.set(this.api.mensaje(error));}}
  prepareCareerEdit(choice:Eleccion):void { this.studentCareerPeriod=choice.periodo;this.studentCareerPlanId.set(choice.planId); }
  async saveStudentCareer():Promise<void>{if(!this.studentCareerPeriod||!this.studentCareerPlanId())return;await this.action(async()=>{await this.api.editarCarreraAlumno({periodo:this.studentCareerPeriod,planId:this.studentCareerPlanId()!});const state=await this.api.carreraAlumno();this.studentCareerChoices.set(state.elecciones);this.studentCareerPeriod='';this.studentCareerPlanId.set(null);await this.selectStudentChoice(state.elecciones.find(c=>c.periodo===this.studentChoice()?.periodo)?.id??state.elecciones[0]?.id??null);this.notice.set('Carrera actualizada con autorización del administrador.');});}
  async saveProfile(): Promise<void> { await this.action(async()=>{this.studentProfile.set(await this.api.guardarPerfilAlumno(this.profileForm));this.notice.set('Datos de contacto guardados.');}); }
  async payAsStudent(): Promise<void> {const id=this.activeStudentEnrollment();const monto=this.studentPayment.monto;if(!id||!monto||this.studentPaymentPending())return;if(this.studentPayment.metodo!=='Efectivo'&&!this.studentVoucher()){this.error.set('Adjunta la foto del voucher de Yape o transferencia.');return;}await this.action(async()=>{await this.api.pagarAlumno(id,{monto,metodo:this.studentPayment.metodo,referencia:this.studentPayment.referencia,modalidad:this.studentPaymentMode,cuotas:this.studentPaymentMode==='CUOTAS'?this.studentInstallments:null,voucher:this.studentVoucher()});this.studentPayments.set(await this.api.pagosAlumno(id));this.studentVoucher.set(null);this.studentVoucherName.set('');this.notice.set('Pago enviado a verificación. El administrador revisará el voucher o confirmará el efectivo recibido.');});}
  async showContract(id:number): Promise<void> { await this.action(async()=>{this.activeStudentEnrollment.set(id);this.contract.set(await this.api.contratoAlumno(id));}); }
  printContract(): void { window.print(); }

  planForm = { school: '', version: '', variant: 'Regular', source: 'Registro administrativo' };
  editingStudentId = signal<number | null>(null);
  studentForm = { codigo: '', nombre: '', correo: '' };
  choiceForm: { estudianteId: number | null; planId: number | null; periodo: string } = { estudianteId: null, planId: null, periodo: '' };
  tariffForm = { periodo: '', base: null as number | null, porCredito: null as number | null, moneda: 'PEN' };
  paymentForm = { monto: null as number | null, metodo: 'Transferencia', referencia: '' };
  loginForm = { username: 'admin', password: '' };
  registerMode = false;
  registerForm = { username: '', password: '', codigo: '', nombre: '', correo: '' };
  accountForm = { username: '', password: '', codigo: '', nombre: '', correo: '' };
  courseForm = { cycle: 1, name: '', credits: 0, ht: 0, hp: 0, hnp: 0, prerequisites: '', horario:'', cupos:30 };
  editingCourseId = signal<number | null>(null);

  readonly selectedChoice = computed(() => this.choices().find(x => x.id === this.choiceId()) ?? null);
  readonly selectedTariff = computed(() => this.tariffs().find(x => x.periodo.toLocaleLowerCase() === this.selectedChoice()?.periodo.toLocaleLowerCase()) ?? null);
  readonly selectedCourses = computed(() => this.courses().filter(x => this.courseIds().includes(x.id)));
  readonly totalCredits = computed(() => this.selectedCourses().reduce((n, x) => n + x.credits, 0));
  readonly estimatedTotal = computed(() => {
    const tariff = this.selectedTariff();
    return tariff ? Number(tariff.base) + Number(tariff.porCredito) * this.totalCredits() : null;
  });
  readonly filteredCourses = computed(() => this.courses().filter(x =>
    (this.courseCycle() === null || x.cycle === this.courseCycle()) &&
    x.name.toLocaleLowerCase().includes(this.courseQuery().trim().toLocaleLowerCase())
  ));
  readonly selectedEnrollment = computed(() => this.enrollmentDetail());
  readonly pendingCount = computed(() => this.enrollments().filter(enrollment => Number(enrollment.saldo) > 0).length);
  readonly filteredEnrollments = computed(() => this.enrollments());
  readonly alreadyEnrolled = computed(() => {
    const choice = this.selectedChoice();
    return choice ? this.enrollments().some(x => x.estudianteId === choice.estudianteId && x.periodo === choice.periodo) : false;
  });

  async ngOnInit(): Promise<void> {
    try { const status = await this.api.authStatus(); this.authenticated.set(status.authenticated); this.username.set(status.username || ''); this.role.set(status.role || 'ADMIN'); } catch { this.authenticated.set(false); }
    this.authChecked.set(true);
    if (this.authenticated()) await this.load();
  }

  async register(): Promise<void> {
    await this.action(async () => { await this.api.register(this.registerForm); this.loginForm.username = this.registerForm.username; this.registerMode = false; this.notice.set('Registro creado. Ahora inicia sesión como alumno.'); this.registerForm = { username: '', password: '', codigo: '', nombre: '', correo: '' }; });
  }
  async login(): Promise<void> {
    await this.action(async () => { const result = await this.api.login(this.loginForm); this.username.set(result.username); this.role.set(result.role); this.authenticated.set(true); this.loginForm.password = ''; await this.load(); });
  }

  async logout(): Promise<void> { try { await this.api.logout(); } finally { this.authenticated.set(false); this.username.set(''); this.role.set('ADMIN'); this.plans.set([]); this.students.set([]); } }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      if (this.role() === 'TEACHER') { return; }
      if (this.role() === 'STUDENT') { const [status, plans, profile, enrollments, career, requests, tariffs, approved] = await Promise.all([this.api.estado(), this.api.planes(),this.api.perfilAlumno(),this.api.alumnoMatriculas(),this.api.carreraAlumno(),this.api.solicitudesAlumno(),this.api.tarifas(),this.api.aprobadosAlumno()]); this.perfil.set(status.perfil); this.plans.set(plans);this.studentProfile.set(profile);this.profileForm={correo:profile.correo||'',telefono:profile.telefono||'',apoderado:profile.apoderado||''};this.studentEnrollments.set(enrollments);this.studentCareerChoices.set(career.elecciones);this.studentCanEditCareer.set(career.puedeEditar);this.studentRequests.set(requests);this.studentTariffs.set(tariffs);this.studentApproved.set(approved);this.studentStep.set(enrollments.length?3:1);await this.selectStudentChoice(career.elecciones[0]?.id??null); return; }
      const [status, plans, students, choices, tariffs, enrollments, accounts, requests, pendingPayments] = await Promise.all([
        this.api.estado(), this.api.planes(), this.api.estudiantesPagina(), this.api.elecciones(),
        this.api.tarifas(), this.api.matriculasPagina(),this.api.cuentas(),this.api.solicitudesAdmin(),this.api.pagosPendientes(),
      ]);
      this.perfil.set(status.perfil);
      this.plans.set(plans.sort((a, b) => a.school.localeCompare(b.school) || b.version.localeCompare(a.version)));
      this.students.set(students.items);this.studentTotal.set(students.total);this.studentPages.set(students.paginas);this.accounts.set(accounts);this.adminRequests.set(requests);this.pendingAdminPayments.set(pendingPayments);
      this.choices.set(choices);
      this.tariffs.set(tariffs);
      this.enrollments.set(enrollments.items);this.enrollmentTotal.set(enrollments.total);this.enrollmentPages.set(enrollments.paginas);
      if (this.choiceId() && !choices.some(x => x.id === this.choiceId())) this.choiceId.set(null);
      if (this.enrollmentId() && !enrollments.items.some(x => x.id === this.enrollmentId())) this.enrollmentId.set(null);
    } catch (error) { this.error.set(this.api.mensaje(error)); }
    finally { this.loading.set(false); }
  }

  private async action(task: () => Promise<void>): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.notice.set('');
    try { await task(); }
    catch (error) { this.error.set(this.api.mensaje(error)); }
    finally { this.busy.set(false); }
  }

  go(paso: Paso): void { this.paso.set(paso); this.error.set(''); this.notice.set(''); }

  async managePlan(id: number): Promise<void> { this.managedPlanId.set(id); this.adminPeriodo=this.plans().find(p=>p.id===id)?.version||''; this.editingCourseId.set(null); this.courseForm={cycle:1,name:'',credits:0,ht:0,hp:0,hnp:0,prerequisites:'',horario:'',cupos:30}; this.managedCourses.set(await this.api.cursos(id));await this.loadAdminCupos(); }
  async saveCourse(): Promise<void> { const planId=this.managedPlanId(); if(!planId||!this.courseForm.name.trim()) return; await this.action(async()=>{const input={planId,cycle:Number(this.courseForm.cycle),name:this.courseForm.name.trim(),credits:Number(this.courseForm.credits),ht:Number(this.courseForm.ht),hp:Number(this.courseForm.hp),hnp:Number(this.courseForm.hnp),prerequisites:this.courseForm.prerequisites.trim(),horario:this.courseForm.horario.trim(),cupos:Number(this.courseForm.cupos)}; if(this.editingCourseId()) await this.api.editarCurso(this.editingCourseId()!,input); else await this.api.crearCurso(input); this.managedCourses.set(await this.api.cursos(planId)); this.courseForm={cycle:1,name:'',credits:0,ht:0,hp:0,hnp:0,prerequisites:'',horario:'',cupos:30}; this.editingCourseId.set(null); this.notice.set('Curso guardado en el plan.');}); }
  editCourse(c: Curso): void { this.editingCourseId.set(c.id); this.courseForm={cycle:c.cycle,name:c.name,credits:c.credits,ht:c.ht,hp:c.hp,hnp:c.hnp,prerequisites:c.prerequisites||'',horario:c.horario||'',cupos:c.cupos||30}; }
  async deleteCourse(c: Curso): Promise<void> { await this.action(async()=>{await this.api.borrarCurso(c.id); if(this.managedPlanId()) this.managedCourses.set(await this.api.cursos(this.managedPlanId()!)); this.notice.set('Curso eliminado del plan.');}); }  editPlan(p:Plan):void { this.editingPlanId.set(p.id);this.planForm={school:p.school,version:p.version,variant:p.variant,source:p.source}; }
  async savePlan(): Promise<void> { if(!this.planForm.school.trim()||!this.planForm.version.trim()) return; await this.action(async()=>{const p=this.editingPlanId()?await this.api.editarPlan(this.editingPlanId()!,{...this.planForm}):await this.api.crearPlan({...this.planForm}); this.plans.set(await this.api.planes()); this.planForm={school:'',version:'',variant:'Regular',source:'Registro administrativo'};this.editingPlanId.set(null);await this.managePlan(p.id);this.notice.set(`Plan ${p.version} guardado. Añade sus cursos aquí abajo.`);}); }
  async deletePlan(p:Plan):Promise<void> {if(!confirm(`¿Eliminar el plan ${p.school} ${p.version}?`))return;await this.action(async()=>{await this.api.borrarPlan(p.id);this.plans.set(await this.api.planes());this.notice.set('Plan eliminado.');});}
  async createTeacher():Promise<void> {if(!this.teacherForm.name||!this.teacherForm.course||!this.teacherForm.school)return;await this.action(async()=>{await this.api.crearDocente(this.teacherForm);if(this.managedPlanId())this.managedCourses.set(await this.api.cursos(this.managedPlanId()!));this.teacherForm={name:'',course:'',school:''};this.notice.set('Docente asignado al curso.');});}
  prepareAccount(student:Estudiante):void{this.accountForm={username:'',password:'',codigo:student.codigo,nombre:student.nombre,correo:student.correo||''};}
  async createStudentAccount(): Promise<void> { if(!this.accountForm.username.trim()||!this.accountForm.password||!this.accountForm.codigo.trim()||!this.accountForm.nombre.trim()) return; await this.action(async()=>{ await this.api.register({...this.accountForm}); await this.searchAdminStudents();this.accounts.set(await this.api.cuentas()); this.accountForm={username:'',password:'',codigo:'',nombre:'',correo:''}; this.notice.set('Usuario alumno creado. Asigna su carrera y periodo para que pueda matricularse por sí mismo.'); }); }
  async changeStudentAccount(username:string):Promise<void>{await this.action(async()=>{await this.api.editarCuenta(username,{username:this.accountEdit.username||username,password:this.accountEdit.password,puedeEditarCarrera:this.accountEdit.puedeEditarCarrera});this.accounts.set(await this.api.cuentas());this.accountEdit={studentId:null,username:'',password:'',puedeEditarCarrera:false};this.notice.set('Cuenta actualizada.');});}
  async editStudent(student: Estudiante): Promise<void> { this.editingStudentId.set(student.id); this.studentForm={codigo:student.codigo,nombre:student.nombre,correo:student.correo||''}; }
  async saveStudent(): Promise<void> {
    await this.action(async () => {
      const input={codigo:this.studentForm.codigo.trim(),nombre:this.studentForm.nombre.trim(),correo:this.studentForm.correo.trim()};
      const saved=this.editingStudentId() ? await this.api.editarEstudiante(this.editingStudentId()!,input) : await this.api.crearEstudiante(input);
      await this.searchAdminStudents(); this.choiceForm.estudianteId=saved.id; this.studentForm={codigo:'',nombre:'',correo:''}; this.editingStudentId.set(null); this.notice.set('Estudiante guardado.');
    });
  }

  async saveChoice(): Promise<void> {
    if (!this.choiceForm.estudianteId || !this.choiceForm.planId || !this.choiceForm.periodo.trim()) return;
    await this.action(async () => {
      const choice = await this.api.elegir({
        estudianteId: this.choiceForm.estudianteId!, planId: this.choiceForm.planId!, periodo: this.choiceForm.periodo.trim(),
      });
      this.choices.set(await this.api.elecciones());
      this.adminSelectedStudentId.set(choice.estudianteId);
      await this.selectChoice(choice.id);
      this.notice.set('Carrera y periodo asignados. El alumno ya puede elegir sus cursos desde su cuenta; verifica la tarifa del periodo.');
    });
  }

  async saveTariff(): Promise<void> {
    if (this.tariffForm.base === null || this.tariffForm.porCredito === null) return;
    await this.action(async () => {
      await this.api.configurarTarifa({
        periodo: this.tariffForm.periodo.trim(), base: this.tariffForm.base!,
        porCredito: this.tariffForm.porCredito!, moneda: this.tariffForm.moneda.trim().toUpperCase(),
      });
      this.tariffs.set(await this.api.tarifas());
      this.go('cursos');
      this.notice.set('Tarifa guardada. Selecciona los cursos del plan.');
    });
  }

  async selectChoice(id: number | null): Promise<void> {
    this.choiceId.set(id);
    this.courseIds.set([]);
    this.courseCycle.set(null);
    this.courseQuery.set('');
    this.courses.set([]);
    const choice = this.choices().find(x => x.id === id);
    if (!choice) return;
    this.tariffForm.periodo = choice.periodo;
    try { this.courses.set(await this.api.cursos(choice.planId)); }
    catch (error) { this.error.set(this.api.mensaje(error)); }
  }

  toggleCourse(id: number, checked: boolean): void {
    this.courseIds.update(ids => checked ? [...ids, id] : ids.filter(x => x !== id));
  }

  async generate(): Promise<void> {
    const choice = this.selectedChoice();
    if (!choice || !this.selectedTariff() || !this.courseIds().length || this.alreadyEnrolled()) return;
    await this.action(async () => {
      const enrollment = await this.api.matricular({
        estudianteId: choice.estudianteId, planId: choice.planId,
        periodo: choice.periodo, cursos: this.courseIds(),
      });
      await this.searchEnrollments();
      await this.selectEnrollment(enrollment.id);
      this.courseIds.set([]);
      this.go('consulta');
      this.notice.set(`Matrícula #${enrollment.id} guardada. Total: ${this.money(enrollment.monto, enrollment.moneda)}.`);
    });
  }

  async selectEnrollment(id: number | null): Promise<void> {
    this.enrollmentId.set(id);
    this.enrollmentDetail.set(null);
    this.payments.set([]);
    if (!id) return;
    try { const [detail,payments]=await Promise.all([this.api.matricula(id),this.api.pagos(id)]);this.enrollmentDetail.set(detail);this.payments.set(payments); }
    catch (error) { this.error.set(this.api.mensaje(error)); }
  }

  async savePayment(): Promise<void> {
    const enrollment = this.selectedEnrollment();
    const amount = this.paymentForm.monto;
    if (!enrollment || amount === null || amount <= 0) return;
    if (amount > Number(enrollment.saldo)) { this.error.set('El pago no puede superar el saldo pendiente.'); return; }
    await this.action(async () => {
      await this.api.pagar(enrollment.id, {
        monto: amount, metodo: this.paymentForm.metodo, referencia: this.paymentForm.referencia.trim(),
      });
      await this.searchEnrollments(this.enrollmentPage());this.enrollmentDetail.set(await this.api.matricula(enrollment.id));
      this.payments.set(await this.api.pagos(enrollment.id));
      this.paymentForm = { monto: null, metodo: 'Transferencia', referencia: '' };
      this.go('consulta');
      this.notice.set('Pago registrado. El saldo y estado de la matrícula se actualizaron.');
    });
  }

  planLabel(planId: number): string {
    const plan = this.plans().find(x => x.id === planId);
    return plan ? `${plan.school} · ${plan.version}${plan.variant === 'SP' ? ' · SP' : ''}` : `Plan #${planId}`;
  }
  money(amount: number | null | undefined, currency = 'PEN'): string {
    return `${currency} ${Number(amount ?? 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
  date(value: string): string { return new Date(value).toLocaleString('es-PE'); }
}












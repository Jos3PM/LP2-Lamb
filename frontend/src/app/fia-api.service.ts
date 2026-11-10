import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Curso, Eleccion, Estudiante, Matricula, Pago, PagoPendiente, Plan, SolicitudMatricula, Tarifa, TeacherAccount, TeacherAssignment, TeacherProfile } from './models';

@Injectable({ providedIn: 'root' })
export class FiaApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';
  register(input: { username: string; password: string; codigo: string; nombre: string; correo: string }) { return firstValueFrom(this.http.post<{ username: string; role: string; estudianteId: number }>(`${this.base}/auth/register`, input)); }
  login(input: { username: string; password: string }) { return firstValueFrom(this.http.post<{ username: string; role: string }>(`${this.base}/auth/login`, input)); }
  authStatus() { return firstValueFrom(this.http.get<{ authenticated: boolean; username: string; role?: string }>(`${this.base}/auth/status`)); }
  cuentas() { return firstValueFrom(this.http.get<{username:string;estudianteId:number;role:string;puedeEditarCarrera:boolean}[]>(`${this.base}/auth/accounts`)); }
  cuentasProfesores() { return firstValueFrom(this.http.get<TeacherAccount[]>(`${this.base}/profesores/cuentas`)); }
  crearCuentaProfesor(input: {username:string;password:string;docenteId:number;codigo:string;correo:string}) { return firstValueFrom(this.http.post<TeacherAccount>(`${this.base}/profesores/cuentas`,input)); }
  editarCuentaProfesor(username:string,input:{codigo:string;correo:string;password:string|null;activo:boolean}) { return firstValueFrom(this.http.put<TeacherAccount>(`${this.base}/profesores/cuentas/${encodeURIComponent(username)}`,input)); }
  perfilProfesor() { return firstValueFrom(this.http.get<TeacherProfile>(`${this.base}/profesor/perfil`)); }
  asignacionesProfesor() { return firstValueFrom(this.http.get<TeacherAssignment[]>(`${this.base}/profesor/asignaciones`)); }
  editarCuenta(username:string,input:{username:string;password:string;puedeEditarCarrera:boolean}) { return firstValueFrom(this.http.put(`${this.base}/auth/accounts/${encodeURIComponent(username)}`,input)); }
  perfilAlumno() { return firstValueFrom(this.http.get<Estudiante>(`${this.base}/alumno/perfil`)); }
  guardarPerfilAlumno(input:{correo:string;telefono:string;apoderado:string}) { return firstValueFrom(this.http.put<Estudiante>(`${this.base}/alumno/perfil`,input)); }
  pagarAlumno(id:number,input:{monto:number;metodo:string;referencia:string;modalidad:string;cuotas:number|null;voucher:string|null}) { return firstValueFrom(this.http.post<Pago>(`${this.base}/alumno/matriculas/${id}/pagos`,input)); }
  carreraAlumno() { return firstValueFrom(this.http.get<{puedeEditar:boolean;elecciones:Eleccion[]}>(`${this.base}/alumno/carrera`)); }
  editarCarreraAlumno(input:{planId:number;periodo:string}) { return firstValueFrom(this.http.put<Eleccion>(`${this.base}/alumno/carrera`,input)); }
  aprobadosAlumno(){return firstValueFrom(this.http.get<number[]>(`${this.base}/alumno/aprobados`));}
  cuposAlumno(periodo:string){return firstValueFrom(this.http.get<Record<number,number>>(`${this.base}/alumno/cupos`,{params:{periodo}}));}
  matricularAlumno(input:{periodo:string;cursos:number[]}){return firstValueFrom(this.http.post<Matricula>(`${this.base}/alumno/matriculas`,input));}
  pagosAlumno(id:number){return firstValueFrom(this.http.get<Pago[]>(`${this.base}/alumno/matriculas/${id}/pagos`));}
  comprobanteAlumno(id:number,pagoId:number){return firstValueFrom(this.http.get<Record<string,unknown>>(`${this.base}/alumno/matriculas/${id}/pagos/${pagoId}/comprobante`));}
  solicitudesAlumno(){return firstValueFrom(this.http.get<SolicitudMatricula[]>(`${this.base}/alumno/solicitudes`));}
  solicitarAlumno(input:{periodo:string;cursos:number[]}){return firstValueFrom(this.http.post<SolicitudMatricula>(`${this.base}/alumno/solicitudes`,input));}
  solicitudesAdmin(){return firstValueFrom(this.http.get<SolicitudMatricula[]>(`${this.base}/solicitudes-matricula`));}
  generarSolicitud(id:number){return firstValueFrom(this.http.post<Matricula>(`${this.base}/solicitudes-matricula/${id}/generar`,{}));}
  rechazarSolicitud(id:number){return firstValueFrom(this.http.post<SolicitudMatricula>(`${this.base}/solicitudes-matricula/${id}/rechazar`,{}));}
  alumnoMatriculas() { return firstValueFrom(this.http.get<Matricula[]>(`${this.base}/alumno/matriculas`)); }
  contratoAlumno(id: number) { return firstValueFrom(this.http.get<Record<string, unknown>>(`${this.base}/alumno/matriculas/${id}/contrato`)); }
  logout() { return firstValueFrom(this.http.post<void>(`${this.base}/auth/logout`, {})); }
  estado() { return firstValueFrom(this.http.get<{ estado: string; perfil: string }>(`${this.base}/estado`)); }
  planes() { return firstValueFrom(this.http.get<Plan[]>(`${this.base}/planes`)); }
  generarHorarios(){return firstValueFrom(this.http.post<{planesActualizados:number;horariosGenerados:number;cursosTotales:number;tipo:string}>(`${this.base}/planes/generar-horarios`,{}));}
  generarHorariosPlan(id:number){return firstValueFrom(this.http.post<{planesActualizados:number;horariosGenerados:number;cursosTotales:number;tipo:string}>(`${this.base}/planes/${id}/generar-horarios`,{}));}
  aumentarCupos(id:number,incremento:number){return firstValueFrom(this.http.post<Curso>(`${this.base}/cursos/${id}/aumentar-cupos`,{incremento}));}
  cuposAdmin(planId:number,periodo:string){return firstValueFrom(this.http.get<Record<number,number>>(`${this.base}/planes/${planId}/cupos`,{params:{periodo}}));}
  crearPlan(input: { school: string; version: string; variant: string; source: string }) { return firstValueFrom(this.http.post<Plan>(`${this.base}/planes`, input)); }
  docentes(buscar='',escuela='',curso='') { return firstValueFrom(this.http.get<{id:number;name:string;course:string;school:string}[]>(`${this.base}/docentes`,{params:{buscar,escuela,curso}})); }
  crearDocente(input:{name:string;course:string;school:string}) { return firstValueFrom(this.http.post(`${this.base}/docentes`,input)); }
  editarDocente(id:number,input:{name:string;course:string;school:string}) { return firstValueFrom(this.http.put(`${this.base}/docentes/${id}`,input)); }
  borrarDocente(id:number) { return firstValueFrom(this.http.delete<void>(`${this.base}/docentes/${id}`)); }
  editarPlan(id: number, input: { school: string; version: string; variant: string; source: string }) { return firstValueFrom(this.http.put<Plan>(`${this.base}/planes/${id}`, input)); }
  borrarPlan(id:number) { return firstValueFrom(this.http.delete<void>(`${this.base}/planes/${id}`)); }
  cursos(planId: number) { return firstValueFrom(this.http.get<Curso[]>(`${this.base}/cursos`, { params: { planId, orden: 'cycle' } })); }
  crearCurso(input: {planId:number;cycle:number;name:string;credits:number;ht:number;hp:number;hnp:number;prerequisites:string;horario:string;cupos:number}) { return firstValueFrom(this.http.post<Curso>(`${this.base}/cursos`, input)); }
  editarCurso(id:number,input:{planId:number;cycle:number;name:string;credits:number;ht:number;hp:number;hnp:number;prerequisites:string;horario:string;cupos:number}) { return firstValueFrom(this.http.put<Curso>(`${this.base}/cursos/${id}`, input)); }
  borrarCurso(id:number) { return firstValueFrom(this.http.delete<void>(`${this.base}/cursos/${id}`)); }  estudiantes() { return firstValueFrom(this.http.get<Estudiante[]>(`${this.base}/estudiantes`)); }
  estudiantesPagina(buscar='',pagina=0,tamano=25){return firstValueFrom(this.http.get<{items:Estudiante[];total:number;pagina:number;tamano:number;paginas:number}>(`${this.base}/estudiantes/pagina`,{params:{buscar,pagina,tamano}}));}
  aprobados(id:number) { return firstValueFrom(this.http.get<number[]>(`${this.base}/estudiantes/${id}/aprobados`)); }
  aprobar(id:number,cursoId:number) { return firstValueFrom(this.http.post<void>(`${this.base}/estudiantes/${id}/aprobados/${cursoId}`,{})); }
  crearEstudiante(input: { codigo: string; nombre: string; correo: string }) { return firstValueFrom(this.http.post<Estudiante>(`${this.base}/estudiantes`, input)); }
  editarEstudiante(id: number, input: { codigo: string; nombre: string; correo: string }) { return firstValueFrom(this.http.put<Estudiante>(`${this.base}/estudiantes/${id}`, input)); }
  borrarEstudiante(id: number) { return firstValueFrom(this.http.delete<void>(`${this.base}/estudiantes/${id}`)); }
  elecciones() { return firstValueFrom(this.http.get<Eleccion[]>(`${this.base}/elecciones-carrera`)); }
  elegir(input: { estudianteId: number; planId: number; periodo: string }) { return firstValueFrom(this.http.post<Eleccion>(`${this.base}/elecciones-carrera`, input)); }
  tarifas() { return firstValueFrom(this.http.get<Tarifa[]>(`${this.base}/tarifas-matricula`)); }
  configurarTarifa(input: { periodo: string; base: number; porCredito: number; moneda: string }) { return firstValueFrom(this.http.put<Tarifa>(`${this.base}/tarifas-matricula`, input)); }
  matriculas() { return firstValueFrom(this.http.get<Matricula[]>(`${this.base}/matriculas`)); }
  matriculasPagina(buscar='',pagina=0,tamano=25){return firstValueFrom(this.http.get<{items:Matricula[];total:number;pagina:number;tamano:number;paginas:number}>(`${this.base}/matriculas/pagina`,{params:{buscar,pagina,tamano}}));}
  matricula(id:number){return firstValueFrom(this.http.get<Matricula>(`${this.base}/matriculas/${id}`));}
  matricular(input: { estudianteId: number; planId: number; periodo: string; cursos: number[] }) { return firstValueFrom(this.http.post<Matricula>(`${this.base}/matriculas`, input)); }
  pagosPendientes(){return firstValueFrom(this.http.get<PagoPendiente[]>(`${this.base}/pagos/pendientes`));}
  voucherPago(matriculaId:number,pagoId:number){return firstValueFrom(this.http.get(`${this.base}/matriculas/${matriculaId}/pagos/${pagoId}/voucher`,{responseType:'blob'}));}
  confirmarPago(matriculaId:number,pagoId:number){return firstValueFrom(this.http.post<Pago>(`${this.base}/matriculas/${matriculaId}/pagos/${pagoId}/confirmar`,{}));}
  rechazarPago(matriculaId:number,pagoId:number,observacion:string){return firstValueFrom(this.http.post<Pago>(`${this.base}/matriculas/${matriculaId}/pagos/${pagoId}/rechazar`,{observacion}));}
  pagos(matriculaId: number) { return firstValueFrom(this.http.get<Pago[]>(`${this.base}/matriculas/${matriculaId}/pagos`)); }
  pagar(matriculaId: number, input: { monto: number; metodo: string; referencia: string }) { return firstValueFrom(this.http.post<Pago>(`${this.base}/matriculas/${matriculaId}/pagos`, input)); }
  mensaje(error: unknown): string {
    if (error instanceof HttpErrorResponse) { const body = error.error as { detail?: string; title?: string; requestId?: string; accionSugerida?: string } | null; const detail=body?.detail || body?.title || `La solicitud falló (HTTP ${error.status || 'sin conexión'}).`; return [detail,body?.accionSugerida,body?.requestId?`Código de soporte: ${body.requestId}`:null].filter(Boolean).join(' '); }
    return error instanceof Error ? error.message : 'No se pudo completar la operación.';
  }
}








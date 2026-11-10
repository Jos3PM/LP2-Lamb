export interface Plan {
  id: number;
  school: string;
  version: string;
  variant: string;
  source: string;
}

export interface Curso {
  id: number;
  planId: number;
  school: string;
  cycle: number;
  name: string;
  credits: number;
  ht: number;
  hp: number;
  hnp: number;
  total: number;
  prerequisites: string | null;
  horario: string | null;
  notes?: string | null;
  cupos: number;
  teachers: { id:number; name:string }[];
}

export interface Estudiante {
  id: number;
  codigo: string;
  nombre: string;
  correo: string | null;
  telefono: string | null;
  apoderado: string | null;
}

export interface Eleccion {
  id: number;
  estudianteId: number;
  estudiante: string;
  planId: number;
  carrera: string;
  periodo: string;
  fecha: string;
}

export interface Tarifa {
  periodo: string;
  base: number;
  porCredito: number;
  moneda: string;
  actualizado: string;
}

export interface DetalleMatricula {
  cursoId: number;
  curso: string;
  creditos: number;
}

export interface Matricula {
  id: number;
  estudianteId: number;
  estudiante: string;
  planId: number;
  carrera: string;
  periodo: string;
  estado: 'PENDIENTE' | 'PARCIAL' | 'PAGADA';
  creditos: number;
  monto: number;
  pagado: number;
  saldo: number;
  moneda: string;
  fecha: string;
  cursos: DetalleMatricula[];
}

export interface SolicitudMatricula {
  id:number; estudianteId:number; estudiante:string; planId:number; carrera:string; periodo:string;
  cursos:number[]; creditos:number; monto:number; moneda:string; estado:'PENDIENTE'|'APROBADA'|'RECHAZADA'; fecha:string;
}

export interface Pago {
  id: number;
  matriculaId: number;
  monto: number;
  metodo: string;
  referencia: string | null;
  fecha: string;
  estado: 'PENDIENTE_VERIFICACION' | 'CONFIRMADO' | 'RECHAZADO';
  tieneVoucher: boolean;
  observacion: string | null;
}

export interface PagoPendiente {
  id:number; matriculaId:number; estudiante:string; codigo:string; carrera:string; periodo:string;
  monto:number; moneda:string; metodo:string; referencia:string|null; fecha:string; tieneVoucher:boolean;
}

export type Paso = 'usuarios' | 'eleccion' | 'tarifas' | 'cursos' | 'generar' | 'pagos' | 'consulta';

export interface TeacherAccount {
  username: string; docenteId: number; docente: string; codigo: string; correo: string;
  activo: boolean; creadoPor: string; modificadoPor: string;
  creadoEn: string; modificadoEn: string;
}
export interface TeacherProfile {
  username: string; docenteId: number; codigo: string; nombre: string; correo: string;
  escuela: string; curso: string; asignaciones: number;
}
export interface TeacherAssignment {
  cargaId: number; detalleId: number; planId: number; cursoId: number; curso: string;
  semestre: string; sede: string; grupo: number; ambiente: string; modalidad: string;
  cupos: number; horas: number; bloques: {dia: number; inicio: number; fin: number}[];
}


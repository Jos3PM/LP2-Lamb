package pe.edu.upeu.fia.matricula.internal;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Table(name="FIA_ESTUDIANTE", uniqueConstraints=@UniqueConstraint(name="UK_FIA_EST_CODIGO",columnNames="codigo"))
class Estudiante {
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="fia_est_seq") @SequenceGenerator(name="fia_est_seq",sequenceName="FIA_ESTUDIANTE_SEQ",allocationSize=1) Long id;
 @Column(nullable=false,length=30) String codigo;
 @Column(nullable=false,length=180) String nombre;
 @Column(length=180) String correo;
 @Column(length=30) String telefono;
 @Column(length=180) String apoderado;
 protected Estudiante(){}
}

@Entity @Table(name="FIA_ELECCION_CARRERA", uniqueConstraints=@UniqueConstraint(name="UK_FIA_ELECCION",columnNames={"estudiante_id","periodo"}))
class EleccionCarrera {
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="fia_elec_seq") @SequenceGenerator(name="fia_elec_seq",sequenceName="FIA_ELECCION_SEQ",allocationSize=1) Long id;
 @Column(name="estudiante_id",nullable=false) Long estudianteId;
 @Column(name="plan_id",nullable=false) Long planId;
 @Column(nullable=false,length=30) String periodo;
 @Column(nullable=false) LocalDateTime fecha;
 protected EleccionCarrera(){}
}

@Entity @Table(name="FIA_TARIFA", uniqueConstraints=@UniqueConstraint(name="UK_FIA_TARIFA_PERIODO",columnNames="periodo"))
class Tarifa {
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="fia_tarifa_seq") @SequenceGenerator(name="fia_tarifa_seq",sequenceName="FIA_TARIFA_SEQ",allocationSize=1) Long id;
 @Column(nullable=false,length=30) String periodo;
 @Column(name="monto_base",nullable=false,precision=12,scale=2) BigDecimal base;
 @Column(name="por_credito",nullable=false,precision=12,scale=2) BigDecimal porCredito;
 @Column(nullable=false,length=3) String moneda;
 @Column(nullable=false) LocalDateTime actualizado;
 protected Tarifa(){}
}

@Entity @Table(name="FIA_MATRICULA", uniqueConstraints=@UniqueConstraint(name="UK_FIA_MAT_EST_PER",columnNames={"estudiante_id","periodo"}))
class Matricula {
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="fia_matricula_seq") @SequenceGenerator(name="fia_matricula_seq",sequenceName="FIA_MATRICULA_SEQ",allocationSize=1) Long id;
 @Column(name="estudiante_id",nullable=false) Long estudianteId;
 @Column(name="plan_id",nullable=false) Long planId;
 @Column(nullable=false,length=30) String periodo;
 @Column(nullable=false,length=20) String estado;
 @Column(nullable=false) int creditos;
 @Column(nullable=false,precision=12,scale=2) BigDecimal monto;
 @Column(nullable=false,precision=12,scale=2) BigDecimal pagado;
 @Column(nullable=false,length=3) String moneda;
 @Column(nullable=false) LocalDateTime fecha;
 protected Matricula(){}
}

@Entity @Table(name="FIA_MATRICULA_DET", uniqueConstraints=@UniqueConstraint(name="UK_FIA_MAT_CURSO",columnNames={"matricula_id","curso_id"}))
class MatriculaDetalle {
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="fia_mat_det_seq") @SequenceGenerator(name="fia_mat_det_seq",sequenceName="FIA_MAT_DET_SEQ",allocationSize=1) Long id;
 @Column(name="matricula_id",nullable=false) Long matriculaId;
 @Column(name="curso_id",nullable=false) Long cursoId;
 @Column(nullable=false,length=300) String curso;
 @Column(nullable=false) int creditos;
 protected MatriculaDetalle(){}
}

@Entity @Table(name="FIA_PAGO")
class Pago {
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="fia_pago_seq") @SequenceGenerator(name="fia_pago_seq",sequenceName="FIA_PAGO_SEQ",allocationSize=1) Long id;
 @Column(name="matricula_id",nullable=false) Long matriculaId;
 @Column(nullable=false,precision=12,scale=2) BigDecimal monto;
 @Column(nullable=false,length=30) String metodo;
 @Column(length=100) String referencia;
 @Column(nullable=false) LocalDateTime fecha;
 @Column(length=30) String estado;
 @Column(length=300) String observacion;
 @Column(name="revisado_en") LocalDateTime revisadoEn;
 @Lob @Column(name="voucher_imagen") byte[] voucherImagen;
 @Column(name="voucher_mime",length=30) String voucherMime;
 protected Pago(){}
}


@Entity @Table(name="FIA_CURSO_APROBADO",uniqueConstraints=@UniqueConstraint(columnNames={"estudiante_id","curso_id"}))
class CursoAprobado {
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="fia_apr_seq") @SequenceGenerator(name="fia_apr_seq",sequenceName="FIA_APROBADO_SEQ",allocationSize=1) Long id;
 @Column(name="estudiante_id",nullable=false) Long estudianteId;
 @Column(name="curso_id",nullable=false) Long cursoId;
 protected CursoAprobado() {}
}

@Entity @Table(name="FIA_SOLICITUD_MATRICULA", uniqueConstraints=@UniqueConstraint(name="UK_FIA_SOL_EST_PER",columnNames={"estudiante_id","periodo"}))
class SolicitudMatricula {
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="fia_sol_seq") @SequenceGenerator(name="fia_sol_seq",sequenceName="FIA_SOLICITUD_SEQ",allocationSize=1) Long id;
 @Column(name="estudiante_id",nullable=false) Long estudianteId;
 @Column(name="plan_id",nullable=false) Long planId;
 @Column(nullable=false,length=30) String periodo;
 @Column(name="curso_ids",nullable=false,length=3000) String cursoIds;
 @Column(nullable=false) int creditos;
 @Column(nullable=false,precision=12,scale=2) BigDecimal monto;
 @Column(nullable=false,length=3) String moneda;
 @Column(nullable=false,length=20) String estado;
 @Column(nullable=false) LocalDateTime fecha;
 protected SolicitudMatricula() {}
}

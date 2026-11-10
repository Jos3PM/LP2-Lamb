package pe.edu.upeu.fia.carga.internal;
import jakarta.persistence.*;
@Embeddable 
class Bloque {int dia; int inicio; int fin; protected Bloque(){} Bloque(int d,int i,int f){dia=d;inicio=i;fin=f;} }

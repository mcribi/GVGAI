package tracks.singlePlayer.evaluacion.src_CRIBILLES_PEREZ_MARIA;

import java.util.ArrayList;
import java.util.Objects;

import ontology.Types;
import ontology.Types.ACTIONS;
import tools.Vector2d;

public class Nodo implements Comparable<Nodo>  { 	//TIENE QUE EXTENDER DE ALGO? 
	
	//informacion que queremos almacenar de cada nodo
	//dentro de cada nodo tiene un estado 
	//un nodo esta asociado a un unico estado, mientras que un mismo estado puede estar representado por diferentes nodos
	
	//puntero al nodo padre
	Nodo padre; 
	int heuristica; 
	int coste; 
	boolean capa_roja; 
	boolean capa_azul; 
	Vector2d posicion; 
	
	//accion que ha hecho el padre para llegar hasta este nodo
	ACTIONS accion_padre; 
	
	
	/* * initialize all variables for the nodo
	*/
	public Nodo (Vector2d pos_avatar, Nodo padre, int heuristic, int coste, ACTIONS accion_padre, boolean capa_roja, boolean capa_azul) { //constructor que crea un nodo
		this.padre=padre; 
		this.heuristica=heuristic; 
		//this.profundidad=profund; 
		this.posicion=pos_avatar; 
		this.coste=coste; 
		//this.acciones= new ArrayList<>();
		this.capa_roja=capa_roja; 
		this.capa_azul=capa_azul; 
		this.accion_padre=accion_padre;
	}

	
	@Override
	public int compareTo(Nodo otro_nodo) {
	    return Integer.compare(this.coste, otro_nodo.coste);
	}
	
	//Para cuando sea A*
//    @Override
//    public int compareTo(Nodo otro) {
//        // Ordenar por coste total (coste + heurística) de menor a mayor.
//        return Integer.compare(this.coste + this.heuristica, otro.coste + otro.heuristica);
//    }
//}
	
	//funcion para comparar nodos
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Nodo otro = (Nodo) obj;
		return capa_roja == otro.capa_roja && capa_azul == otro.capa_azul && posicion.equals(otro.posicion);
		//return posicion.equals(otro.posicion);
	}
	
	//calcular hash
	@Override
	public int hashCode() {
		//return posicion.hashCode();
		return (int) posicion.x * 31 + (int) posicion.y * (17) + (capa_roja ? 1 : 0) * 2 + (capa_azul ? 1 : 0) * 3;
		//return 31 * (31 * (int)posicion.x + (int)posicion.y) + (capa_roja ? 1 : 0) + (capa_azul ? 2 : 0);
		//return Objects.hash(posicion, capa_roja, capa_azul);
	}

}
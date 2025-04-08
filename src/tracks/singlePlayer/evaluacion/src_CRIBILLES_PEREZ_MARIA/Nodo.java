package tracks.singlePlayer.evaluacion.src_CRIBILLES_PEREZ_MARIA;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
//import java.util.Objects;
import ontology.Types.ACTIONS;
import tools.Vector2d;

public class Nodo implements Comparable<Nodo>  {  
	
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
	int antiguedad; 
	int f; //f=g+h
	
	//accion que ha hecho el padre para llegar hasta este nodo
	ACTIONS accion_padre; 

	HashSet <Vector2d> capas_rojas = new HashSet<>(); //posiciones de las capas rojas restantes de cada nodo
	HashSet <Vector2d> capas_azules = new HashSet<>(); //posiciones de las capas azules restantes de cada nodo
	
	/* * initialize all variables for the nodo
	*/
	public Nodo (Vector2d pos_avatar, Nodo padre, int heuristica, int coste, ACTIONS accion_padre, boolean capa_roja, boolean capa_azul, HashSet<Vector2d> capas_rojas, HashSet<Vector2d> capas_azules, int antiguedad) { //constructor que crea un nodo 
		this.posicion=pos_avatar; 
		this.padre=padre;
		this.heuristica=heuristica; 
		this.f=heuristica+coste; 
		this.coste=coste; 
		this.capa_roja=capa_roja; 
		this.capa_azul=capa_azul; 
		this.accion_padre=accion_padre;
		this.capas_rojas=new HashSet <> (capas_rojas);
		this.capas_azules=new HashSet <> (capas_azules);
		this.antiguedad=antiguedad;
		
	}

	
	@Override
	public int compareTo(Nodo otro_nodo) {
		//en caso de empate, se coge el nodo con mas antiguedad
		if (this.f == otro_nodo.f) {
			return Integer.compare(this.antiguedad, otro_nodo.antiguedad); //cuanto menos numero de antiguedad es el mas antiguo
		}else {
			return Integer.compare(this.f, otro_nodo.f);
		}  
	}
	
	//funcion para comparar nodos
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Nodo otro = (Nodo) obj;
		
		boolean iguales = capa_roja == otro.capa_roja && capa_azul == otro.capa_azul && posicion.equals(otro.posicion) && capas_rojas.equals(otro.capas_rojas) && capas_azules.equals(otro.capas_azules);
		
		return iguales;
	}
	
	//calcular hash
	@Override
	public int hashCode() {
		//return posicion.hashCode() + (capa_roja ? 13 : 0) + (capa_azul ? 37 : 0);
		//return posicion.hashCode();
		return (int) posicion.x * 31 + (int) posicion.y * (17) + (capa_roja ? 1 : 0) * 13 + (capa_azul ? 1 : 0) * 37 + capas_rojas.hashCode() * 43 + capas_azules.hashCode() * 47;
		//return 31 * (31 * (int)posicion.x + (int)posicion.y) + (capa_roja ? 1 : 0) + (capa_azul ? 2 : 0);
		//return (int) posicion.x * 31 + (int) posicion.y * (17) + (capa_roja ? 1 : 0) * 13 + (capa_azul ? 1 : 0) * 37; 
		//return Objects.hash(posicion, capa_roja, capa_azul, capas_rojas, capas_azules);
		//return Objects.hash(posicion, capa_roja, capa_azul, capas_rojas, capas_azules);
	}
	
	public String generarClave() {
	    StringBuilder clave = new StringBuilder();
	    
	    clave.append(this.posicion.x).append(",").append(this.posicion.y).append(";"); //posicion
	    clave.append(this.capa_roja).append(",").append(this.capa_azul).append(";"); //capa roja y azul
	    
	    clave.append("R:"); //capas_rojas restantes
	    this.capas_rojas.stream()
	        .sorted(Comparator.comparingDouble((Vector2d v) -> v.x).thenComparingDouble(v -> v.y))
	        .forEach(v -> clave.append(v.x).append("_").append(v.y).append(","));
	    
	    clave.append("B:"); //capas_azules restantes
	    this.capas_azules.stream()
	        .sorted(Comparator.comparingDouble((Vector2d v) -> v.x).thenComparingDouble(v -> v.y))
	        .forEach(v -> clave.append(v.x).append("_").append(v.y).append(","));
	    
	    return clave.toString();
	}

	


}
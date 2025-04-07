package tracks.singlePlayer.evaluacion.src_CRIBILLES_PEREZ_MARIA;

import java.util.ArrayList;
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
	
	//accion que ha hecho el padre para llegar hasta este nodo
	ACTIONS accion_padre; 

	ArrayList <Vector2d> capas_rojas = new ArrayList<>(); //posiciones de las capas rojas restantes de cada nodo
	ArrayList <Vector2d> capas_azules = new ArrayList<>(); //posiciones de las capas azules restantes de cada nodo
	
	/* * initialize all variables for the nodo
	*/
	public Nodo (Vector2d pos_avatar, Nodo padre, int heuristic, int coste, ACTIONS accion_padre, boolean capa_roja, boolean capa_azul, ArrayList<Vector2d> capas_rojas, ArrayList<Vector2d> capas_azules, int antiguedad) { //constructor que crea un nodo
		this.padre=padre; 
		this.heuristica=heuristic; 
		this.posicion=pos_avatar; 
		this.coste=coste; 
		this.capa_roja=capa_roja; 
		this.capa_azul=capa_azul; 
		this.accion_padre=accion_padre;
		this.capas_rojas=new ArrayList <> (capas_rojas);
		this.capas_azules=new ArrayList <> (capas_azules);
		this.antiguedad=antiguedad;
	}

	
	@Override
	public int compareTo(Nodo otro_nodo) {
		//en caso de empate, se coge el nodo con mas antiguedad
		if (this.coste == otro_nodo.coste) {
			return Integer.compare(this.antiguedad, otro_nodo.antiguedad); //cuanto menos numero de antiguedad es el mas antiguo
		}else {
			return Integer.compare(this.coste, otro_nodo.coste);
		}  
	}
	
	//funcion para comparar nodos
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Nodo otro = (Nodo) obj;
		
		boolean iguales = capa_roja == otro.capa_roja && capa_azul == otro.capa_azul && posicion.equals(otro.posicion) && capas_rojas.equals(otro.capas_rojas) && capas_azules.equals(otro.capas_azules);
		//boolean iguales = capa_roja == otro.capa_roja && capa_azul == otro.capa_azul && posicion.equals(otro.posicion);
		

		return iguales;
	}
	
	//calcular hash
//	@Override
//	public int hashCode() {
//		//return posicion.hashCode();
//		return (int) posicion.x * 31 + (int) posicion.y * (17) + (capa_roja ? 1 : 0) * 13 + (capa_azul ? 1 : 0) * 37 + capas_rojas.hashCode() * 43 + capas_azules.hashCode() * 47;
//		//return 31 * (31 * (int)posicion.x + (int)posicion.y) + (capa_roja ? 1 : 0) + (capa_azul ? 2 : 0);
//		//return (int) posicion.x * 31 + (int) posicion.y * (17) + (capa_roja ? 1 : 0) * 13 + (capa_azul ? 1 : 0) * 37; 
//		//return Objects.hash(posicion, capa_roja, capa_azul, capas_rojas, capas_azules);
//		//return Objects.hash(posicion, capa_roja, capa_azul);
//	}


}
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

	HashSet <String> capas_rojas = new HashSet<>(); //posiciones de las capas rojas restantes de cada nodo
	HashSet <String> capas_azules = new HashSet<>(); //posiciones de las capas azules restantes de cada nodo
	
	 private int cachedHash;  // Campo para almacenar el hash calculado
	 private boolean hashCalculated = false;
	    
	/* * initialize all variables for the nodo
	*/
	public Nodo (Vector2d pos_avatar, Nodo padre, int heuristica, int coste, ACTIONS accion_padre, boolean capa_roja, boolean capa_azul, HashSet<String> capas_rojas, HashSet<String> capas_azules, int antiguedad) { //constructor que crea un nodo 
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
		
		//primero comparamos f
	    if (this.f != otro_nodo.f) {
	        return Integer.compare(this.f, otro_nodo.f);
	    }
	    //si f es igual, en el a* se compara g, es decir, el coste
	    if (this.coste != otro_nodo.coste) {
	        return Integer.compare(this.coste, otro_nodo.coste); 
	    }
	    //si el coste tambien coincide, comparamos la antiguedad
	    return Integer.compare(this.antiguedad, otro_nodo.antiguedad);  //cuanto menos numero de antiguedad es el mas antiguo 
	}
	
	//funcion para comparar nodos
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj) return true;
//		if (obj == null || getClass() != obj.getClass()) return false;
//		Nodo otro = (Nodo) obj;
//		
//		boolean iguales = capa_roja == otro.capa_roja && capa_azul == otro.capa_azul && posicion.equals(otro.posicion) && capas_rojas.equals(otro.capas_rojas) && capas_azules.equals(otro.capas_azules);
//		
//		return iguales;
//	}
	
	@Override
	public boolean equals(Object obj) {
	    if (this == obj) return true;
	    if (obj == null || getClass() != obj.getClass()) return false;
	    
	    Nodo otro = (Nodo) obj;
	    
	    // 1. Comparación más rápida: booleanos
	    if (capa_roja != otro.capa_roja || capa_azul != otro.capa_azul) 
	        return false;
	    
	    // 2. Comparación de coordenadas (más rápida que Vector2d.equals())
	    if (posicion.x != otro.posicion.x || posicion.y != otro.posicion.y) 
	        return false;
	    
	    // 3. Comparación de conjuntos (solo si lo anterior coincide)
	    if (!capas_rojas.equals(otro.capas_rojas)) 
	        return false;
	    
	    return capas_azules.equals(otro.capas_azules);
	}
	
	@Override
    public int hashCode() {
        if (!hashCalculated) {
            cachedHash = calcularHash();
            hashCalculated = true;
        }
        return cachedHash;
    }
	
	//calcular hash
	//@Override
	public int calcularHash() {
		return (int) posicion.x * 31 + (int) posicion.y * (17) + (capa_roja ? 1 : 0) * 13 + (capa_azul ? 1 : 0) * 37 + capas_rojas.hashCode() * 43 + capas_azules.hashCode() * 47;
	}
	
	//prueba de otros hash mas senicllos para optimizar
//	@Override    
//	public int hashCode() {        
//		int hash = 7;        
//		hash= 31 * hash + (int) this.posicion.x;        
//		hash = 31 * hash + (int)this.posicion.y;        
//		hash =	31 * hash + (this.capa_azul? 1 : 0);   
//		hash =	31 * hash + (this.capa_roja? 1 : 0);
//		hash = 31 * hash +	capas_rojas.hashCode();  
//		hash = 31 * hash +	capas_azules.hashCode(); 
//		return hash;    
//	}
	

	
//	private int calcularHash() {
//        int hash = 17;  // Número primo inicial
//        hash = 31 * hash + (int)posicion.x;
//        hash = 31 * hash + (int)posicion.y;
//        hash = 31 * hash + (capa_roja ? 1 : 0);
//        hash = 31 * hash + (capa_azul ? 1 : 0);
//        hash = 31 * hash + capas_rojas.hashCode();
//        hash = 31 * hash + capas_azules.hashCode();
//        return hash;
//    }

	
//	public String generarClave() {
//	    StringBuilder clave = new StringBuilder();
//	    
//	    clave.append(this.posicion.x).append(",").append(this.posicion.y).append(";"); //posicion
//	    clave.append(this.capa_roja).append(",").append(this.capa_azul).append(";"); //capa roja y azul
//	    
//	    clave.append("R:"); //capas_rojas restantes
//	    this.capas_rojas.stream()
//	        .sorted(Comparator.comparingDouble((Vector2d v) -> v.x).thenComparingDouble(v -> v.y))
//	        .forEach(v -> clave.append(v.x).append("_").append(v.y).append(","));
//	    
//	    clave.append("B:"); //capas_azules restantes
//	    this.capas_azules.stream()
//	        .sorted(Comparator.comparingDouble((Vector2d v) -> v.x).thenComparingDouble(v -> v.y))
//	        .forEach(v -> clave.append(v.x).append("_").append(v.y).append(","));
//	    
//	    return clave.toString();
//	}
	
	public String generarClave() {
	    StringBuilder clave = new StringBuilder();

	    // Posición actual (puede dejarse como está si sigue siendo Vector2d)
	    clave.append(this.posicion.x).append(",").append(this.posicion.y).append(";");

	    // Estado de las capas del nodo
	    clave.append(this.capa_roja).append(",").append(this.capa_azul).append(";");

	    // Capas rojas restantes (ordenadas)
	    clave.append("R:");
	    this.capas_rojas.stream()
	        .sorted()
	        .forEach(pos -> clave.append(pos).append(","));

	    // Capas azules restantes (ordenadas)
	    clave.append("B:");
	    this.capas_azules.stream()
	        .sorted()
	        .forEach(pos -> clave.append(pos).append(","));

	    return clave.toString();
	}


	


}
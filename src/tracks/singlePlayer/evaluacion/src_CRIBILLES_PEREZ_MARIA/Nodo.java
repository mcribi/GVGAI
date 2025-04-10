package tracks.singlePlayer.evaluacion.src_CRIBILLES_PEREZ_MARIA;

import java.util.HashSet;
import ontology.Types.ACTIONS;
import tools.Vector2d;

public class Nodo implements Comparable<Nodo>  {  
	
	//informacion que queremos almacenar de cada nodo
	//dentro de cada nodo tiene un estado 
	//un nodo esta asociado a un unico estado, mientras que un mismo estado puede estar representado por diferentes nodos
	
	//posiblemente hubiese quedado mas claro hacer una clase diferente para el estado y otra para el nodo, 
	//pero por simplicidad lo hemos hecho todo en la misma clase
	//aun asi, lo ideal es tener siempre claro que seria del nodo y que seria del estado
	
	//Estado vs. Nodo (son diferentes):
	//Estado: posición, capa_roja, capa_azul, capas rojas y capasazules
	//Nodo: padre, heuristica, coste, f, accion_padre, antiguedad
	
	//Atributos de la clase Nodo: 
	Nodo padre; //puntero al nodo padre
	int heuristica;  //heuristica del nodo (distancia manhattan). Es como una aproximacion de lo lejos que esta del portal
	int coste;  //coste del nodo
	boolean capa_roja; //booleano true/false para saber si el nodo tiene una capa roja o no
	boolean capa_azul; //booleano true/false para saber si el nodo tiene una capa azul o no
	Vector2d posicion; //posicion del avatar en el mapa
	int antiguedad; //para el criterio de desempate
	int f; //f=g+h
	ACTIONS accion_padre; //accion que ha hecho el padre para llegar hasta este nodo

	//hemos hecho un HashSet de String para que sea mas eficiente ya que vector2d no tiene hashcode y por tanto no era tan eficiente
	//simplemente la diferencia es que se almacena una clave de string que es la posicion pasada a string
	HashSet <String> capas_rojas = new HashSet<>(); //posiciones de las capas rojas restantes de cada nodo
	HashSet <String> capas_azules = new HashSet<>(); //posiciones de las capas azules restantes de cada nodo
	
	private int num_hash;  //para almacenar el hash calculado
	private boolean hash_calculado = false; //booleano para saber si el hash ha sido calculado antes o no
	    
	//Constructor para incializar todos los atributos del nodo a unos parametros dados
	public Nodo (Vector2d pos_avatar, Nodo padre, int heuristica, int coste, ACTIONS accion_padre, boolean capa_roja, boolean capa_azul, HashSet<String> capas_rojas, HashSet<String> capas_azules, int antiguedad) { //constructor que crea un nodo 
		this.posicion=pos_avatar; 
		this.padre=padre;
		this.heuristica=heuristica; 
		this.f=heuristica+coste; //esto es lo unico que no pasamos por parametro ya que hacemos la suma directamente aqui
		this.coste=coste; 
		this.capa_roja=capa_roja; 
		this.capa_azul=capa_azul; 
		this.accion_padre=accion_padre;
		this.capas_rojas=new HashSet <> (capas_rojas); //en java no ese copian, se referencian. Por eso hacemos un nuevo hashset
		this.capas_azules=new HashSet <> (capas_azules); //en java no ese copian, se referencian. Por eso hacemos un nuevo hashset
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
	@Override
	public boolean equals(Object obj) {
	    if (this == obj) return true; //si son el mismo objeto
	    if (obj == null || getClass() != obj.getClass()) return false; //si el objeto es null o no es del mismo tipo
	    
	    Nodo otro = (Nodo) obj; //convertimos el objeto a nodo
	    
	    //comparamos primero los booleanos porque son los mas rapidos
	    if (capa_roja != otro.capa_roja || capa_azul != otro.capa_azul) 
	        return false;
	    
	    //despues la posicion (vector2d)
	    if (posicion.x != otro.posicion.x || posicion.y != otro.posicion.y) 
	        return false;
	    
	    //por ultimo lo mas costoso
	    if (!capas_rojas.equals(otro.capas_rojas)) 
	        return false;
	    
	    return capas_azules.equals(otro.capas_azules);
	}
	
	//hashcode
	@Override
    public int hashCode() {
        if (!hash_calculado) { //si no hemos calculado antes el hash
            num_hash = calcularHash(); //lo calculamos
            hash_calculado = true; //y lo marcamos como calculado
        }
        return num_hash; //devolvemos el hash
    }
	
	//calcular hash
	public int calcularHash() {  //multiplicamos por numeros primos para que haya los menos conflictos posibles y asi sea los mas eficiente posible
		return (int) posicion.x * 31 + (int) posicion.y * (17) + (capa_roja ? 1 : 0) * 13 + (capa_azul ? 1 : 0) * 37 + capas_rojas.hashCode() * 43 + capas_azules.hashCode() * 47;
	}

	//para crear una clave univoca para cada nodo y poder guardar y buscar en una tabla hash
	//representa el estado del nodo
	//basicamente es codificar en un string separados con , y ; los atributos para que no sean vector2d, int, booleanos...
	public String generarClave() { //este metodo me lo ha hecho principalmente ChatGPT (es solo crear un string ordenado)
		//creamos un stringbuilder para construir eficientemente la cadena
	    StringBuilder clave = new StringBuilder();

	    //añadimos posicion actual (es un vector2d pasado a string)
	    clave.append(this.posicion.x).append(",").append(this.posicion.y).append(";");

	    //añadimos el estado de la capa azul y roja
	    clave.append(this.capa_roja).append(",").append(this.capa_azul).append(";");

	    //añadimos las capas rojas restantes (ordenadas)
	    clave.append("R:");
	    this.capas_rojas.stream()
	        .sorted()
	        .forEach(pos -> clave.append(pos).append(","));

	    //añadimos las capas azules restantes (ordenadas)
	    clave.append("B:");
	    this.capas_azules.stream()
	        .sorted()
	        .forEach(pos -> clave.append(pos).append(","));

	    return clave.toString(); //lo convertimos a string y devolvemos la clave final
	}


	


}
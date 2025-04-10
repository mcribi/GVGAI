package tracks.singlePlayer.evaluacion.src_CRIBILLES_PEREZ_MARIA;

import core.player.AbstractPlayer;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.HashSet;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import java.util.LinkedList;


public class AgenteDijkstra extends AbstractPlayer {
	LinkedList <ACTIONS> ruta; //ruta con las acciones a seguir. Es una lista pero que se puede insertar y extraer elementos de cualquier parte de la lista
	Vector2d fescala; //numero de pixeles de cada celda
	Vector2d portal;//posicion portal (solucion)
	Vector2d pos_avatar; //posicion avatar 
	int nodos_expandidos; //contador de nodos expandidos
	HashSet <Observation> obstaculos; //observacion obstaculos (objetos inmoviles)
	HashSet <Observation> capas; //observacion capas
	HashSet<String> capasIniciales_rojas; //posiciones de las capas iniciales rojas
	HashSet<String> capasIniciales_azules; //posiciones de las capas iniciales azules
	
	//abiertos y visitados
	//hacemos una cola con prioridad comparando f(g+h) para que se ordenen y saque el que menos f tenga 
	PriorityQueue<Nodo> abiertos;
	HashSet<Nodo> visitados;
	
	//tamaño mapa
	int anchura;
    int altura;
    
    //para desempate
    int antiguedad; 
    
    //para medir los tiempos
    long tiempoTotalms;
    long tInicio; 
    long tFin;
    
    //para optimizacion: 
    private boolean[][] esPosicionValida; // true = transitable, false = obstáculo
    private boolean[][] esMuroRojo; //posiciones de muros rojos
    private boolean[][] esMuroAzul; //posiciones de muros azules
    
     static final Vector2d[] direcciones = { //son constantes, no cambian nunca. Asi no la creamos cada vez
    		new Vector2d(1, 0),  //derecha
    		new Vector2d(-1, 0), //izquierda
    		new Vector2d(0, -1), //arriba
    		new Vector2d(0, 1)   //abajo
    };

   //cambios/desplazamientos dependiendo de la direccion de la accion
	 static final ACTIONS[] ordenAcciones = { //son constantes, no cambian nunca. Asi no la creamos cada vez
		ACTIONS.ACTION_RIGHT, //es el orden de expansion predeterminado en clase
		ACTIONS.ACTION_LEFT,
		ACTIONS.ACTION_UP,
		ACTIONS.ACTION_DOWN
	};
	
	
	
	/* * initialize all variables for the agent
	* @param stateObs Observation of the current state.
	* @param elapsedTimer Timer when the action returned is due.
	*/
	public AgenteDijkstra(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) { //todo lo que se tenga que inicializar
		
		obstaculos=  new HashSet<>(); //observacion obstaculos (objetos inmoviles)
		capas=  new HashSet<>(); //observacion capas
		capasIniciales_rojas = new HashSet<>(); //posiciones de las capas iniciales rojas
		capasIniciales_azules = new HashSet<>(); //posiciones de las capas iniciales azules
		nodos_expandidos=0; //contador de nodos expandidos
		antiguedad=0; 
		
		//inicializamos la ruta
		ruta = new LinkedList<>();
		
		//inicializamos abiertos y visitados
		abiertos = new PriorityQueue<>();
        visitados = new HashSet<>();

        //obtenemos el tamaño del mapa
		ArrayList<Observation>[][] obsGrid=stateObs.getObservationGrid(); //intentamos hacer las minimas llamadas posibles a funciones de GVGAI
		anchura= obsGrid.length;
		altura=obsGrid[0].length;
		
		//Calculamos el factor de escala entre mundos (pixeles -> grid)
		Dimension dimensiones=stateObs.getWorldDimension(); 
	    fescala = new Vector2d(dimensiones.width / anchura, dimensiones.height / altura);
	    
	    //posicion del avatar
	    Vector2d pos=stateObs.getAvatarPosition();
		pos_avatar= new Vector2d(pos.x / fescala.x, pos.y / fescala.y); //convertimos a posiciones que manejamos de enteros 
		
	    //posicion obstaculos
	    ArrayList <Observation>[] obstaculos_temp= stateObs.getImmovablePositions(); //obstaculos Observation
	    
	    if (obstaculos_temp != null) { //si hay obstaculos los guardamos
		    for (ArrayList<Observation> lista : obstaculos_temp){ //iteramos sobre cada celda que tenga obstaculos
		    	for (Observation obs : lista) { //iteramos por cada obstaculo en la celda 
		    		obstaculos.add(obs); //guardamos el obstaculo entero	
		    	} 
		    }
	    }
	    
        //mismo tamaño que el mapa (matriz de booelanos), es para mejorar la eficiencia
        esPosicionValida = new boolean[anchura][altura];
        esMuroRojo = new boolean[anchura][altura];
        esMuroAzul = new boolean[anchura][altura];
	    
	   //rellenamos la matriz de booleanos con los obstaculos
	    for (int x = 0; x < anchura; x++) { 
	        for (int y = 0; y < altura; y++) {
	            esPosicionValida[x][y] = true; //por defecto todas las posiciones son validas
	        }
	    }
	    
	    //actualizamos la matriz de booleanos con los obstaculos
	    for (Observation obs : obstaculos) { 
	    	//convertimos la posicion a la escala del mundo
	        int x = (int)(obs.position.x / fescala.x);
	        int y = (int)(obs.position.y / fescala.y);
	        
	        //muro normal (w): nunca se puede pasar
	        if (obs.itype == 5) esPosicionValida[x][y] = false; 
	        
	        //muro rojo (r), guardamos donde hay muros rojos
	        else if (obs.itype == 6) esMuroRojo[x][y] = true;
	        
	        //muro azul (b), guardamos donde hay muros azules
	        else if (obs.itype == 7) esMuroAzul[x][y] = true;    
	        
	        //trampa (t): nunca se puede pasar
	        else if (obs.itype == 3) esPosicionValida[x][y] = false; 
	   }
	    
	   //posicion capas 
	   //quiero guardar tambien el tipo de capa (roja o azul)
	   ArrayList <Observation>[] capas_temp= stateObs.getResourcesPositions(); //obstaculos Observation
	   //es una lista de lista donde primero es muro(y sus posiciones), bloque(y sus posiciones)...
	   if (capas_temp != null) { //comprobacion por si no hubiese para no perder tiempo
		    for (ArrayList<Observation> lista : capas_temp){ //iteramos sobre cada celda que tenga capa
		    	for (Observation obs : lista) { //iteramos por cada capa
		    		capas.add(obs);		
		    	} 
		    }
	    }
		
	   //incializamos las capas inciales por cada color (solo posiciones)
	    for (Observation capa : capas) {
	    	String key = (int)capa.position.x + "," + (int)capa.position.y; //creamos la key para la capa (es un hashset de strings) para que sea mas eficiente
	    	if (capa.itype == 8) { //si es capa roja 8
	    		capasIniciales_rojas.add(key); //guardamos la posicion de la capa roja
	    	}else if (capa.itype == 9) { //si es capa azul 9
	    		capasIniciales_azules.add(key); //guardamos la posicion de la capa azul
	    	}	
	    }
	
	    //se puede suponer que solo hay un portal (se dijo en clase)
		//Se crea una lista de observaciones de portales, ordenada por cercanía al avatar 
		portal = stateObs.getPortalsPositions(pos)[0].get(0).position; //cogemos el primer portal (suponemos que es el unico)
  
		//convertimos a posiciones con la escala calculada antes
		portal.x = Math.floor(portal.x / fescala.x);  
		portal.y = Math.floor(portal.y / fescala.y);

	}
	
	//primero se ejecuta una vez el constructor y despues se van ejecutando muchas veces el metodo act. La primera es la que mas tarda
	//ya que es en ella en la que se crea la ruta y despues en las siguientes solo se va sacando la primera accion de la ruta
	
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) { //devuelve la proxima accion (es el metodo que piensa)
		//si no tenemos ruta la calculamos (solo el primer act) 
		if (ruta.isEmpty()) {    
		    //calculamos la ruta
			ruta=dijkstra(stateObs,elapsedTimer, pos_avatar, portal);
			
			//si no se encuentra solucion 
	        if (ruta.isEmpty()) {
	            System.out.println("No se encontró camino al portal");
	            return ACTIONS.ACTION_NIL; //acción por defecto
	        }
		}
		return (ruta.removeFirst()); //si la ruta ya esta calculada devolvemos (y eliminamos de la linkedlist) la primera accion
	}
	
	public LinkedList<ACTIONS> dijkstra (StateObservation stateObs, ElapsedCpuTimer elapsedTimer, Vector2d posInicial, Vector2d posFinal) {
		//empezamos a medir el tiempo
		tInicio = System.nanoTime();

		//metemos el primer nodo (nodo raiz): con padre null, sin coste, sin heuristica (en dijkstra no se considera), sin accion padre, sin capa roja ni azul
        Nodo inicial = new Nodo(posInicial,null, 0, 0, Types.ACTIONS.ACTION_NIL, false, false, capasIniciales_rojas, capasIniciales_azules, antiguedad); //inicializamos el nodo inicial
        abiertos.add(inicial); //lo añadimos a abiertos
        antiguedad++; //aumentamos la antiguedad del nodo inicial (cada nodo va a tener una antiguedad diferente)
	
		
		while (!abiertos.isEmpty()) { //mientras que queden nodos por visitar
			//actual: nodo de menor coste que no haya estado visitado
			Nodo nodo_actual=abiertos.poll(); //coge el primer nodo de abiertos y lo quita

			//comprobamos que no haya sido visitado
		     if (visitados.contains(nodo_actual)) {
		    	 continue; //saltamos si el nodo ya ha sido visitado
		     }
			
			//sumamos uno a los nodos expandidos
			nodos_expandidos++; //el nodo actual tambien es expandido
			
			//comprobar si el nodo en el que esta el avatar es el portal
			//si actual==objetivo (para Vector2d se usa equals())
			if (nodo_actual.posicion.equals(posFinal)) {
				ruta=reconstruirRuta(nodo_actual); 
				
				//resultados cuando se encuentra la solución
	            System.out.println("Nodos expandidos totales: " + nodos_expandidos);
	            System.out.println("Tamaño de la ruta calculada: " + ruta.size() + " acciones");
	           
	            break; //salimos del bucle si hemos encontrado la ruta
			}
			
			//añadimos el nodo actual a visitados
			visitados.add(nodo_actual);

			//expandir sucesores
			for (int i = 0; i < ordenAcciones.length; i++) {//iteramos por cada accion disponible	
				ACTIONS accion = ordenAcciones[i]; //accion a realizar
				Vector2d offset = direcciones[i]; //desplazamiento segun la accion que se realiza
				
				//la nueva posicion es la posicion actual + el desplazamiento
				Vector2d nuevaPos = nodo_actual.posicion.copy().add(offset);

				//si la posicion no es valida porque sea un obstaculo pasamos y no perdermos tiempo
				if (!esPosicionValida(nodo_actual, nuevaPos)) {
					continue;
				}
				
				//creamos el sucesor de la posicion valida
				Nodo sucesor = new Nodo(nuevaPos, nodo_actual, 0, nodo_actual.coste + 1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, antiguedad);

				if(sucesor.coste < nodo_actual.coste+1) { //hacemos esta comprobacion primero para evitar pasar por el contains (es muy costoso)
					//aqui no va a entrar nunca ya que el coste es uniforme
					//aun asi no lo quitamos porque si no estariamos implementando anchura, no dijkstra
					if ( !visitados.contains(sucesor)) { //si el sucesor no ha sido visitado y es mejor que el actual
						sucesor.coste=nodo_actual.coste+1; //actualizamos el coste del sucesor
						
						//actualizamos el padre
						sucesor.padre=nodo_actual;
						
						//añadimos el sucesor a abiertos
						actualizarCapas(sucesor); //actualizamos las capas del sucesor
						abiertos.add(sucesor);
						antiguedad++; 
					}
				}
				
				//añadimos el sucesor a abiertos, actualizamos las capas y la antiguedad
				actualizarCapas(sucesor); //actualizamos las capas del sucesor
				abiertos.add(sucesor);
				antiguedad++;
			}
		}
		
		//tiempo final y total
		tFin = System.nanoTime();
		tiempoTotalms = (tFin - tInicio) / 1000000;
		
		//imprimimos el resultados del tiempo total para calcular la ruta con dijsktra
	    System.out.println("Tiempo total Dijkstra: " + tiempoTotalms + " ms");
	    
	    return ruta; //devolvemos la ruta con todos los nodos hasta la meta
		
	}
	
	//función para reconstruir la ruta a partir de un nodo
	public LinkedList<ACTIONS> reconstruirRuta (Nodo nodoFinal){
		LinkedList<ACTIONS> ruta= new LinkedList<ACTIONS>(); //como arraylist pero se puede añadir al final y al principio
		Nodo nodoActual=nodoFinal; //vamos a ir hacia atras desde el nodo final hasta el inicial
		
		//mientras que no lleguemos al primer nodo añadido (sabemos que el padre del primero es null)
		while (nodoActual.padre!=null) {
			ruta.addFirst(nodoActual.accion_padre); //añadimos al principio el nodo padre
			nodoActual=nodoActual.padre;  //actualizamos el nodo actual 
		}
		
		return ruta; 
	}
	
	//función para comprobar si una posicion del tablero es valida (no hay obstaculo y esta dentro del tablero)
	private boolean esPosicionValida(Nodo nodo, Vector2d pos) {
		//verificamos los límites del mapa
	    if (pos.x < 0 || pos.y < 0 || pos.x >= anchura || pos.y >= altura) {
	        return false;
	    }
	    
//    Obstáculo - Tipo: 3 → 't' (trampa)
//    Obstáculo - Tipo: 5 → 'w' (muro normal)
//    Obstáculo - Tipo: 7 → 'b' (muro azul)
//    Obstáculo - Tipo: 6 → 'r' (muro rojo)
	    
	    int x = (int)pos.x;
	    int y = (int)pos.y;
	    
	    //si es muro rojo solo es pasable con la capa roja
	    if (esMuroRojo[x][y]) return nodo.capa_roja;
	    
	    //si es muro azul solo es pasable con la capa azul
	    if (esMuroAzul[x][y]) return nodo.capa_azul;
	    
	    //trampas o muros normales
	    return esPosicionValida[x][y];
	}
	
	//funcion para actualizar las capas del nodo
	public void actualizarCapas(Nodo nodo) {
	    //usamos la misma key que cuando hemos inicializado
	    String posKey = (int)(nodo.posicion.x * fescala.x) + "," + (int)(nodo.posicion.y * fescala.y);
	    
	    //si la posicion del nodo es igual a la de la capa
	    if (nodo.capas_azules.remove(posKey)) { 
	        nodo.capa_azul = true; //si la capa azul se ha eliminado, el nodo tiene capa azul
	        nodo.capa_roja = false; //no puede tener las dos capas a la vez
	    } else if (nodo.capas_rojas.remove(posKey)) {
	        nodo.capa_roja = true; //si la capa roja se ha eliminado, el nodo tiene capa roja
	        nodo.capa_azul = false; //no puede tener las dos capas a la vez
	    }
	}




}
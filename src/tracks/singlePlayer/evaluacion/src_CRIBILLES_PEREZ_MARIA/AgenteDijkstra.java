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
	//ArrayList <Nodo> ruta; //ruta con nodos
	LinkedList <ACTIONS> ruta; //ruta con las acciones a seguir
	Vector2d fescala; //numero de pixeles de cada celda
	Vector2d portal;//posicion portal
	Vector2d pos_avatar; //posicion avatar 
	int nodos_expandidos; //contador de nodos expandidos
	HashSet <Observation> obstaculos; //observacion obstaculos (objetos inmoviles)
	HashSet <Observation> capas; //observacion capas
	HashSet<String> capasIniciales_rojas; //posiciones de las capas iniciales rojas
	HashSet<String> capasIniciales_azules; //posiciones de las capas iniciales azules
	
	//abiertos y cerrados
	//hacemos una cola con prioridad para que se ordenen y saque el que menos coste tenga 
	PriorityQueue<Nodo> abiertos;
	//HashSet<Nodo> visitados;
	HashSet<Nodo> visitados;
	//tamaño mapa
	int anchura;
    int altura;
    int antiguedad; 
    long tiempoTotalms;
    long tInicio; 
    long tFin;
    
    //para optimizacion: 
    private boolean[][] esPosicionValida; // true = transitable, false = obstáculo
    private boolean[][] esMuroRojo;      // Posiciones de muros rojos
    private boolean[][] esMuroAzul;      // Posiciones de muros azules
    
     static final Vector2d[] direcciones = { //son constantes, no cambian nunca. Asi no la creamos cada vez
    		new Vector2d(1, 0),  // DERECHA
    		new Vector2d(-1, 0), // IZQUIERDA
    		new Vector2d(0, -1), // ARRIBA
    		new Vector2d(0, 1)   // ABAJO
    };

	 static final ACTIONS[] ordenAcciones = { //son constantes, no cambian nunca. Asi no la creamos cada vez
		ACTIONS.ACTION_RIGHT,
		ACTIONS.ACTION_LEFT,
		ACTIONS.ACTION_UP,
		ACTIONS.ACTION_DOWN
	};
	
	
	
	/* * initialize all variables for the agent
	* @param stateObs Observation of the current state.
	* @param elapsedTimer Timer when the action returned is due.
	*/
	public AgenteDijkstra(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		
		obstaculos=  new HashSet<>(); //observacion obstaculos (objetos inmoviles)
		capas=  new HashSet<>(); //observacion capas
		capasIniciales_rojas = new HashSet<>(); //posiciones de las capas iniciales rojas
		capasIniciales_azules = new HashSet<>();
		nodos_expandidos=0; 
		antiguedad=0; 
		//inicializamos la ruta
		ruta = new LinkedList<>();
		
		//inicializamos abiertos y cerrados
		abiertos = new PriorityQueue<>();
        visitados = new HashSet<>();

		

	    
	  //Calculamos el factor de escala entre mundos (pixeles -> grid)
		ArrayList<Observation>[][] obsGrid=stateObs.getObservationGrid();
		anchura= obsGrid.length;
		altura=obsGrid[0].length;
		
		//Calculamos el factor de escala entre mundos (pixeles -> grid)
		Dimension dimensiones=stateObs.getWorldDimension(); 
	    fescala = new Vector2d(dimensiones.width / anchura, dimensiones.height / altura);
	    
	    Vector2d pos=stateObs.getAvatarPosition();
		pos_avatar= new Vector2d(pos.x / fescala.x, pos.y / fescala.y);
		
	    
	    //posicion obstaculos (lo mismo que los enemigos pero con obstaculo inmoviles con la funcion getImmovablePositions())
	    ArrayList <Observation>[] obstaculos_temp= stateObs.getImmovablePositions(); //obstaculos Observation
	    
	    if (obstaculos_temp != null) { //si hay obstaculos los guardamos
		    for (ArrayList<Observation> lista : obstaculos_temp){ //iteramos sobre cada celda que tenga obstaculos
		    	for (Observation obs : lista) { //iteramos por cada obstaculo en la celda (en una celda puede haber mas de un enemigo)
		    		obstaculos.add(obs); //guardamos el obstaculo entero
		    		//System.out.println("Obstáculo en: " + (obs.position.x)/fescala.x + ", " + (obs.position.y)/fescala.y);
		    	} 
		    }
	    }
	    
        
        // Inicialización:
        esPosicionValida = new boolean[anchura][altura];
        esMuroRojo = new boolean[anchura][altura];
        esMuroAzul = new boolean[anchura][altura];
	    
	 // Rellenar los mapas (en el constructor):
	    for (int x = 0; x < anchura; x++) {
	        for (int y = 0; y < altura; y++) {
	            esPosicionValida[x][y] = true; // Por defecto, transitable
	        }
	    }
	
	    for (Observation obs : obstaculos) {
	        int x = (int)(obs.position.x / fescala.x);
	        int y = (int)(obs.position.y / fescala.y);
	        
	        if (obs.itype == 5) esPosicionValida[x][y] = false;      // Muro normal
	        else if (obs.itype == 6) esMuroRojo[x][y] = true;        // Muro rojo
	        else if (obs.itype == 7) esMuroAzul[x][y] = true;        // Muro azul
	        else if (obs.itype == 3) esPosicionValida[x][y] = false; // Trampa
	   }
	    
	  //posicion capas (lo mismo que los enemigos pero con las capas con la funcion getResourcesPositions())
	   //quiero guardar tambien el tipo de capa (roja o azul)
	   ArrayList <Observation>[] capas_temp= stateObs.getResourcesPositions(); //obstaculos Observation
	   //es una lista de lista donde primero es muro(y sus posiciones), bloque(y sus posiciones)...
	   if (capas_temp != null) { //comprobacion por si no hubiese para no perder tiempo
		    for (ArrayList<Observation> lista : capas_temp){ //iteramos sobre cada celda que tenga obstaculos
		    	for (Observation obs : lista) { //iteramos por cada obstaculo en la celda (en una celda puede haber mas de un enemigo)
		    		capas.add(obs);		
		    	} 
		    }
	    }
		
	   //Capas inciales por cada color (solo posiciones)
	    for (Observation capa : capas) {
	    	//String key = (int)(capa.position.x * fescala.x) + "," + (int)(capa.position.y * fescala.y);
	    	String key = (int)capa.position.x + "," + (int)capa.position.y; 
	    	if (capa.itype == 8) { //si es capa roja 8
	    		capasIniciales_rojas.add(key); //guardamos la posicion de la capa roja
	    		 //capasIniciales_rojas.add(capa.key); //guardamos la posicion de la capa roja
	    	}else if (capa.itype == 9) {
	    		capasIniciales_azules.add(key);
	    		//capasIniciales_azules.add(capa.position); //guardamos la posicion de la capa azul
	    	}	
	    }
	
	    //se puede suponer que solo hay un portal (se dijo en clase)
		//Se crea una lista de observaciones de portales, ordenada por cercanía al avatar 
		portal = stateObs.getPortalsPositions(pos)[0].get(0).position; //cogemos el primer portal (suponemos que es el unico)
  
		//convertimos a posiciones
		portal.x = Math.floor(portal.x / fescala.x);  
		portal.y = Math.floor(portal.y / fescala.y);

	}
	
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) { //devuelve la proxima accion
		//si no tenemos ruta la calculamos (solo el primer act) 
		if (ruta.isEmpty()) {    
		    
			ruta=dijkstra(stateObs,elapsedTimer, pos_avatar, portal);
			
			//si no se encuentra solucion
	        if (ruta.isEmpty()) {
	            System.out.println("No se encontró camino al portal");
	            return ACTIONS.ACTION_NIL; //acción por defecto
	        }
		}
		return (ruta.removeFirst()); //si la ruta ya esta calculada devolvemos la primera accion
	}
	
	public LinkedList<ACTIONS> dijkstra (StateObservation stateObs, ElapsedCpuTimer elapsedTimer, Vector2d posInicial, Vector2d posFinal) {
		//empezamos a medir el tiempo
		tInicio = System.nanoTime();
		
	    
	    // ==== Métricas ====
//	    long tiempoTotalExpansion = 0;
//	    long tiempoTotalEsPosicionValida = 0;
//	    long tiempoTotalActualizarCapas = 0;
//	    long tiempoTotalPriorityQueue = 0;
//	    long tiempoTotalVisitados = 0;
	    // =================
		
		
		
		//metemos el primer nodo (nodo raiz)
        Nodo inicial = new Nodo(posInicial,null, 0, 0, Types.ACTIONS.ACTION_NIL, false, false, capasIniciales_rojas, capasIniciales_azules, antiguedad); //inicializamos el nodo inicial
        abiertos.add(inicial);
        antiguedad++; 
	
		
		while (!abiertos.isEmpty()) { //mientras que queden nodos por visitar
			//actual: nodo de menor coste que no haya estado visitado
			//long inicioPoll = System.nanoTime();
			Nodo nodo_actual=abiertos.poll(); //coge el primer nodo de abiertos y lo quita
//			tiempoTotalPriorityQueue += System.nanoTime() - inicioPoll;
//			
//			long inicioVisitados = System.nanoTime();
			//comprobamos que no haya sido visitado
		    //saltamos si el nodo ya ha sido visitado
		      if (visitados.contains(nodo_actual)) {
		          continue;
		      }
		    
		    
//		    tiempoTotalVisitados += System.nanoTime() - inicioVisitados;
			
			//sumamos uno a los nodos expandidos
			nodos_expandidos++; //el nodo actual tambien es expandido
			
			//comprobar si el nodo en el que esta el avatar es el portal
			//si actual==objetivo (para Vector2d se usa equals())
			if (nodo_actual.posicion.equals(posFinal)) {
				ruta=reconstruirRuta(nodo_actual); 
				 // Imprimir estadísticas cuando se encuentra la solución
	            System.out.println("Nodos expandidos totales: " + nodos_expandidos);
	            System.out.println("Tamaño de la ruta calculada: " + ruta.size() + " acciones");
	           
	            break;
			}
			//long inicioAddVisitados = System.nanoTime();
			//lo añadimos a visitados y expandimos sus suscesores
			visitados.add(nodo_actual);
//			tiempoTotalVisitados += System.nanoTime() - inicioAddVisitados;
			
//			long inicioExpansion = System.nanoTime();
//			long tiempoCreacionNodos = 0;
//			long tiempoComparacionCoste = 0;
//			long tiempoContencionVisitados = 0;
//			long tiempoAddAbiertos = 0;
//			long calculoNuevaPos=0; 
			//expandir sucesores
			//Quiero que el orden exacto de expansion sea: DERECHA, IZQUIERDA, ARRIBA, ABAJO
			//ACTIONS[] ordenAcciones = {ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN};

			//ArrayList <Nodo> sucesores=new ArrayList<>();
			//for (ACTIONS accion: ordenAcciones) { //iteramos por cada accion disponible
			for (int i = 0; i < ordenAcciones.length; i++) {	
				ACTIONS accion = ordenAcciones[i];
				Vector2d offset = direcciones[i];
				//long inicioNuevaPos = System.nanoTime();
				Vector2d nuevaPos = nodo_actual.posicion.copy().add(offset);
				//calculoNuevaPos += System.nanoTime() - inicioNuevaPos;
				//long inicioEsPosicionValida = System.nanoTime();
				if (!esPosicionValida(nodo_actual, nuevaPos)) {
					continue;
				}
				//tiempoTotalEsPosicionValida += System.nanoTime() - inicioEsPosicionValida;
				//long inicioCreacionNodo = System.nanoTime();
				Nodo sucesor = new Nodo(nuevaPos, nodo_actual, 0, nodo_actual.coste + 1, accion,
						nodo_actual.capa_roja, nodo_actual.capa_azul,
						nodo_actual.capas_rojas, nodo_actual.capas_azules, antiguedad);
				//tiempoCreacionNodos += System.nanoTime() - inicioCreacionNodo;
				
				//long inicioComparacionCoste = System.nanoTime();
				if(sucesor.coste < nodo_actual.coste+1) {
					//tiempoComparacionCoste += System.nanoTime() - inicioComparacionCoste;
					
					//long inicioContencionVisitados = System.nanoTime();
					if ( !visitados.contains(sucesor)) { //si el sucesor no ha sido visitado y es mejor que el actual					
						//tiempoContencionVisitados += System.nanoTime() - inicioContencionVisitados;
						sucesor.coste=nodo_actual.coste+1; //actualizamos el coste del sucesor
						//actualizamos el padre
						sucesor.padre=nodo_actual;
						//añadimos el sucesor a abiertos
						actualizarCapas(sucesor); //actualizamos las capas del sucesor
						abiertos.add(sucesor);
						antiguedad++;
					}
				}
				
				//long inicioActualizarCapas = System.nanoTime();
				actualizarCapas(sucesor); //actualizamos las capas del sucesor
				//tiempoTotalActualizarCapas += System.nanoTime() - inicioActualizarCapas;
				//long inicioAddAbiertos = System.nanoTime();
				abiertos.add(sucesor);
				//tiempoAddAbiertos += System.nanoTime() - inicioAddAbiertos;
				antiguedad++;
			}
			//tiempoTotalExpansion += System.nanoTime() - inicioExpansion;
			//System.out.println("\n=== DESGLOSE DE TIEMPOS EN EXPANSIÓN ===");
			//System.out.println("  - Cálculo nueva posición: " + (calculoNuevaPos / 1_000_000) + " ms");
			//System.out.println("  - Validación posición: " + (tiempoTotalEsPosicionValida / 1_000_000) + " ms");
//			System.out.println("  - Creación de nodos: " + (tiempoCreacionNodos / 1_000_000) + " ms");
//			System.out.println("  - Comparación de costes: " + (tiempoComparacionCoste / 1_000_000) + " ms");
//			System.out.println("  - Verificación en 'visitados': " + (tiempoContencionVisitados / 1_000_000) + " ms");
			//System.out.println("  - Actualización de capas: " + (tiempoTotalActualizarCapas / 1_000_000) + " ms");
			//System.out.println("  - Añadir a 'abiertos': " + (tiempoAddAbiertos / 1_000_000) + " ms");
		}
	
		tFin = System.nanoTime();
		tiempoTotalms = (tFin - tInicio) / 1_000_000;
	    System.out.println("Tiempo total Dijkstra: " + tiempoTotalms + " ms");
		
	 // ==== Imprimir resultados ====
//	    System.out.println("\n=== RESULTADOS DE PROFILING ===");
//	    System.out.println("Tiempo total Dijkstra: " + tiempoTotalms + " ms");
//	    System.out.println("  - PriorityQueue.poll(): " + (tiempoTotalPriorityQueue / 1_000_000) + " ms");
//	    System.out.println("  - visitados.contains/add: " + (tiempoTotalVisitados / 1_000_000) + " ms");
//	    System.out.println("  - esPosicionValida(): " + (tiempoTotalEsPosicionValida / 1_000_000) + " ms");
//	    System.out.println("  - actualizarCapas(): " + (tiempoTotalActualizarCapas / 1_000_000) + " ms");
//	    System.out.println("  - Expansión de nodos: " + (tiempoTotalExpansion / 1_000_000) + " ms");
	    
	    System.out.println("Nodos expandidos: " + nodos_expandidos);
//	    System.out.println("==============================\n");
	    
	    return ruta; //devolvemos la ruta con todos los nodos hasta la meta
		
	}
	
	//función para reconstruir la ruta a partir de un nodo
	public LinkedList<ACTIONS> reconstruirRuta (Nodo nodoFinal){
		LinkedList<ACTIONS> ruta= new LinkedList<ACTIONS>(); //como arraylist pero se peude añadir al final y al principio
		Nodo nodoActual=nodoFinal; 
		//mientras que no lleguemos al primer nodo añadido (sabemos que el padre del primero es null)
		while (nodoActual.padre!=null) {
			ruta.addFirst(nodoActual.accion_padre); //añadimos al principio el nodo padre
			nodoActual=nodoActual.padre;  //actualizamos el nodo actual 
		}
		return ruta; 
	}
	
	//función para comprobar si una posicion del tablero es valida (no hay obstaculo y esta dentro del tablero)
//	private boolean esPosicionValida(Nodo nodo, Vector2d pos, StateObservation stateObs) {
//		//verificamos los límites del mapa
//	    if (pos.x<0 || pos.y<0 || pos.x>= anchura || pos.y>= altura) {
//	        return false;
//	    }
//	    
////      Obstáculo - Tipo: 3 → 't' (trampa)
////      Obstáculo - Tipo: 5 → 'w' (muro normal)
////      Obstáculo - Tipo: 7 → 'b' (muro azul)
////      Obstáculo - Tipo: 6 → 'r' (muro rojo)
//	    
//	    //convertimos la posicion a la escala del mundo
//	    Vector2d posMundo = new Vector2d(pos.x * fescala.x, pos.y * fescala.y);
//	    
//	    //verificamos si hay obstáculos en esa posición
//	    for (Observation obstaculo : obstaculos) { //como tenemos los obstaculos en una lista
//	        if (obstaculo.position.equals(posMundo)) {
//		    	if (obstaculo.itype == 5) { //si es un muro normal
//		            return false;
//		        }
//		    	else if (obstaculo.itype == 6) { //si es muro rojo
//		        	return nodo.capa_roja; //si el nodo tiene capa roja
//		        }else if (obstaculo.itype == 7) { //si es muro azul
//		        	return nodo.capa_azul; //si el nodo tiene capa azul
//		        }else if (obstaculo.itype == 3) { //si es trampa
//		        	return false;
//		        }		        	
//	        }
//	    }
//	    return true; //si no es ningun obstaculo de los de antes se puede pasar
//	}
	
	private boolean esPosicionValida(Nodo nodo, Vector2d pos) {
	    // Verificar límites del mapa
	    if (pos.x < 0 || pos.y < 0 || pos.x >= anchura || pos.y >= altura) {
	        return false;
	    }
	    
	    int x = (int)pos.x;
	    int y = (int)pos.y;
	    
	    // Muro rojo (solo pasable con capa roja)
	    if (esMuroRojo[x][y]) return nodo.capa_roja;
	    
	    // Muro azul (solo pasable con capa azul)
	    if (esMuroAzul[x][y]) return nodo.capa_azul;
	    
	    // Caso general (trampas/muros normales)
	    return esPosicionValida[x][y];
	}
	
//	public void actualizarCapas(Nodo nodo_actual) {
//		
//		//primero cambiamos la posicion del nodo actual a la escala del mundo
//		Vector2d posMundo = new Vector2d(nodo_actual.posicion.x * fescala.x,nodo_actual.posicion.y * fescala.y);
////		System.out.println("Posición del nodo actual: " + nodo_actual.posicion + 
////				 ", Capa roja: " + nodo_actual.capa_roja + 
////				 ", Capa azul: " + nodo_actual.capa_azul);
//		//si la casilla donde estamos es capa actualizamos las capas del nodo
//		for (Vector2d capa : nodo_actual.capas_azules) { //iteramos por cada capa
//			if (capa.equals(posMundo)) { //si la posicion del nodo es igual a la de la capa
//				nodo_actual.capa_azul=true; 
//				nodo_actual.capa_roja=false; //no se pueden tener las dos a la vez
//				//eliminamos la capa azul de la lista porque se elimina al recogerla
//				nodo_actual.capas_azules.remove(capa);
//				break; //salimos del bucle si ya hemos encontrado la capa (solo hay una capa por posicion)
//			}
//		}
//		
//		for (Vector2d capa : nodo_actual.capas_rojas) { //iteramos por cada capa
//			if (capa.equals(posMundo)) { //si la posicion del nodo es igual a la de la capa
//				nodo_actual.capa_roja=true;
//				nodo_actual.capa_azul=false; //no se pueden tener las dos a la vez
//				//eliminamos la capa roja de la lista porque se elimina al recogerla
//				nodo_actual.capas_rojas.remove(capa);
//				break; //salimos del bucle si ya hemos encontrado la capa (solo hay una capa por posicion)
//			}
//		}
//	}
	
	public void actualizarCapas(Nodo nodo) {
	    // Usar misma conversión que en inicialización
	    String posKey = (int)(nodo.posicion.x * fescala.x) + "," + (int)(nodo.posicion.y * fescala.y);
	    
	    if (nodo.capas_azules.remove(posKey)) {
	        nodo.capa_azul = true;
	        nodo.capa_roja = false;
	    } else if (nodo.capas_rojas.remove(posKey)) {
	        nodo.capa_roja = true;
	        nodo.capa_azul = false;
	    }
	}




}
//package tracks.singlePlayer.evaluacion.src_CRIBILLES_PEREZ_MARIA;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedList;
//
//import core.game.Observation;
//import core.game.StateObservation;
//import core.player.AbstractPlayer;
//import ontology.Types;
//import ontology.Types.ACTIONS;
//import tools.ElapsedCpuTimer;
//import tools.Vector2d;
//
//public class AgenteLRTAStar extends AbstractPlayer {
//		LinkedList <ACTIONS> ruta; //ruta con las acciones a seguir
//		Vector2d fescala; //numero de pixeles de cada celda
//		Vector2d portal;//posicion portal
//		Vector2d pos_avatar; //posicion avatar 
//		int nodos_expandidos; //contador de nodos expandidos
//		HashSet <Observation> obstaculos; //observacion obstaculos (objetos inmoviles)
//		HashSet <Observation> capas; //observacion capas
//		HashSet<Vector2d> capasIniciales_rojas; //posiciones de las capas iniciales rojas
//		HashSet<Vector2d> capasIniciales_azules; //posiciones de las capas iniciales azules
//		Nodo nodo_actual;
//	    //tamaño mapa
//		int anchura;
//	    int altura;
//		
//		//tabla hash (posicion del vector + heuristica) para almacenar heuristicas de los nodos
//		HashMap<String, Integer> tablaHeuristicas; //la posicion del vector la codificamos como un string para poder acceder a el y que no haya duplicados
//		
//		//Constructor
//		public AgenteLRTAStar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
//			obstaculos=  new HashSet<>(); //observacion obstaculos (objetos inmoviles)
//			capas=  new HashSet<>(); //observacion capas
//			capasIniciales_rojas = new HashSet<>(); //posiciones de las capas iniciales rojas
//			capasIniciales_azules = new HashSet<>();
//			nodos_expandidos=0; 
//			ruta = new LinkedList<>();
//			tablaHeuristicas= new HashMap<>();
//			
//			
//			//Calculamos el factor de escala entre mundos (pixeles -> grid)
//			anchura= stateObs.getObservationGrid().length;
//			altura=stateObs.getObservationGrid()[0].length;
//			 
//		    fescala = new Vector2d(stateObs.getWorldDimension().width / anchura,
//		    stateObs.getWorldDimension().height / altura);
//		    
//		  //posicion obstaculos (lo mismo que los enemigos pero con obstaculo inmoviles con la funcion getImmovablePositions())
//		    ArrayList <Observation>[] obstaculos_temp= stateObs.getImmovablePositions(); //obstaculos Observation
//		    
//		    if (obstaculos_temp != null) { //si hay obstaculos los guardamos
//			    for (ArrayList<Observation> lista : obstaculos_temp){ //iteramos sobre cada celda que tenga obstaculos
//			    	for (Observation obs : lista) { //iteramos por cada obstaculo en la celda (en una celda puede haber mas de un enemigo)
//			    		obstaculos.add(obs); //guardamos el obstaculo entero
//			    		//System.out.println("Obstáculo en: " + (obs.position.x)/fescala.x + ", " + (obs.position.y)/fescala.y);
//			    	} 
//			    }
//		    }
//		    
//		  //posicion capas (lo mismo que los enemigos pero con las capas con la funcion getResourcesPositions())
//		   //quiero guardar tambien el tipo de capa (roja o azul)
//		   ArrayList <Observation>[] capas_temp= stateObs.getResourcesPositions(); //obstaculos Observation
//		   //es una lista de lista donde primero es muro(y sus posiciones), bloque(y sus posiciones)...
//		   if (capas_temp != null) { //comprobacion por si no hubiese para no perder tiempo
//			    for (ArrayList<Observation> lista : capas_temp){ //iteramos sobre cada celda que tenga obstaculos
//			    	for (Observation obs : lista) { //iteramos por cada obstaculo en la celda (en una celda puede haber mas de un enemigo)
//			    		capas.add(obs);		
//			    	} 
//			    }
//		    }
//			
//		   //Capas inciales por cada color (solo posiciones)
//		    for (Observation capa : capas) {
//		    	 if (capa.itype == 8) { //si es capa roja 8
//		    		capasIniciales_rojas.add(capa.position); //guardamos la posicion de la capa roja
//		    	}else if (capa.itype == 9) {
//		    		capasIniciales_azules.add(capa.position); //guardamos la posicion de la capa azul
//		    	}	
//		    }
//		
//		    //SE PUEDE SUPONER QUE SOLO VA A HABER UN PORTAL
//			//Se crea una lista de observaciones de portales, ordenada por cercanía al avatar 
//			ArrayList<Observation>[] posiciones = stateObs.getPortalsPositions(stateObs.getAvatarPosition());
//			//Seleccionamos el portal mas proximo 
//			portal = posiciones[0].get(0).position;  //cogemos el primer portal (suponemos que es el unico)
//			//convertimos a posiciones
//			portal.x = Math.floor(portal.x / fescala.x);  
//			portal.y = Math.floor(portal.y / fescala.y);
//		}
//		
//		@Override
//		public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) { //devuelve la proxima accion
//			//posicion del avatar (convertimos getAvatarPosition() con el factor de esacala a un vector2d)
//		    pos_avatar= new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
//		    stateObs.getAvatarPosition().y / fescala.y);
//			
//		    long tInicio = System.nanoTime();
//		    ACTIONS accion=alg_rta(stateObs,elapsedTimer, pos_avatar, portal);
//		    long tFin = System.nanoTime();
//			long tiempoTotalms = (tFin - tInicio)/1000000;
//			//System.out.println("Tiempo total " + tiempoTotalms + " en calcular la ruta con RTA*");
//			
//			
//			 // Imprimir estadísticas cuando se encuentra la solución
//            System.out.println("Nodos expandidos totales: " + nodos_expandidos);
////            System.out.println("Tamaño de la ruta calculada: " + nodos_expandidos + " acciones");
////	        System.out.println("============================");
//
//			return accion;
//		}
//		
//		//algoritmo de busqueda RTA*: al ser en tiempo real no devolvemos una ruta completa, si no que accion en accion
//		public ACTIONS alg_rta(StateObservation stateObs, ElapsedCpuTimer elapsedTimer, Vector2d posInicial, Vector2d posFinal){
//			if (nodo_actual==null) {
//			
//				int antiguedad=0; 
//				
//				//actual=nodo_inicial
//				int heuristica_inicial=distanciaManhattan(posInicial, posFinal); 
//				nodo_actual = new Nodo(posInicial,null, heuristica_inicial, 0, Types.ACTIONS.ACTION_NIL, false, false, capasIniciales_rojas, capasIniciales_azules, antiguedad); //inicializamos el nodo inicial
//				//String clave = nodo_actual.posicion.x + "," + nodo_actual.posicion.y +"," + nodo_actual.capa_roja + "," + nodo_actual.capa_azul;
//				String clave=nodo_actual.generarClave();
//				tablaHeuristicas.put(clave, nodo_actual.heuristica); //metemos la primera heuristica en la tabla hash
//			}
//			
//			nodos_expandidos++; //aumentamos el contador de nodos expandidos
//			
//			
//			//comprobar si el nodo en el que esta el avatar es el portal
//			if (nodo_actual.posicion.equals(posFinal)) {
//	            return ACTIONS.ACTION_NIL;
//			}
//			
//			
//			
//			//expandimos los vecinos del nodo actual
//			ArrayList<Nodo> sucesores = new ArrayList<Nodo>(); //arraylist de sucesores
//			//orden de las expansiones
//			ACTIONS[] ordenAcciones = {ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN};
//			for (ACTIONS accion: ordenAcciones) { //iteramos por cada accion disponible
//				
//				//inicializamos sucesor y depsues lo actualizamos
//				Nodo sucesor=null;
//
//
//				if (accion==ACTIONS.ACTION_RIGHT) {				
//					//creamos un nuevo nodo con la accion y posicion correspondiente
//					Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x+1, nodo_actual.posicion.y);
//					if (esPosicionValida(nodo_actual, nuevaPos, stateObs)) { //despues actualizamos la heuristica
//						sucesor = new Nodo(nuevaPos, nodo_actual, 0, 1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas,nodo_actual.capas_azules, nodo_actual.antiguedad+1);
//						actualizarCapas(sucesor);
//						sucesores.add(sucesor);
//					}
//				}
//				else if (accion==ACTIONS.ACTION_LEFT) {				
//					//creamos un nuevo nodo con la accion y posicion correspondiente
//					Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x-1, nodo_actual.posicion.y);
//					if (esPosicionValida(nodo_actual, nuevaPos, stateObs)) {
//						sucesor = new Nodo(nuevaPos, nodo_actual, 0, 1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul,nodo_actual.capas_rojas, nodo_actual.capas_azules, nodo_actual.antiguedad+1);
//						actualizarCapas(sucesor);
//						sucesores.add(sucesor);
//					}
//				}
//				else if (accion==ACTIONS.ACTION_UP) {				
//					//creamos un nuevo nodo con la accion y posicion correspondiente
//					Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x, nodo_actual.posicion.y-1);
//					if (esPosicionValida(nodo_actual, nuevaPos, stateObs)) {
//						sucesor = new Nodo(nuevaPos, nodo_actual, 0, 1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, nodo_actual.antiguedad+1);
//						actualizarCapas(sucesor);
//						sucesores.add(sucesor);
//					}
//				}
//				else if (accion==ACTIONS.ACTION_DOWN) {				
//					//creamos un nuevo nodo con la accion y posicion correspondiente
//					Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x, nodo_actual.posicion.y+1);
//					if (esPosicionValida(nodo_actual, nuevaPos, stateObs)) {
//						sucesor = new Nodo(nuevaPos, nodo_actual, 0, 1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, nodo_actual.antiguedad+1);
//						actualizarCapas(sucesor);
//						sucesores.add(sucesor);
//					}
//				}
//			}
//			
//			if (sucesores.isEmpty()) {
//	            System.out.println("No hay sucesores válidos");
//	            return ACTIONS.ACTION_NIL;
//	        }
//			
//			
//			//calculamos el coste de cada sucesor y las actualizamos
//			for (Nodo sucesor : sucesores) { //iteramos por cada sucesor
//				String clave_suc=sucesor.generarClave();
//				//String clave_suc = sucesor.posicion.x + "," + sucesor.posicion.y+ "," + sucesor.capa_roja + "," + sucesor.capa_azul;;
//				if (tablaHeuristicas.containsKey(clave_suc)) { //si ya esta en la tabla
//			        sucesor.heuristica = tablaHeuristicas.get(clave_suc);
//			    } else { //si es la primera vez es la distancia manhattan, la introducimos
//			        sucesor.heuristica = distanciaManhattan(sucesor.posicion, posFinal);
//			        //String clave_sucesor = sucesor.posicion.x + "," + sucesor.posicion.y+ "," + sucesor.capa_roja + "," + sucesor.capa_azul;
//			        String clave_sucesor=sucesor.generarClave();
//			        tablaHeuristicas.put(clave_sucesor, sucesor.heuristica);
//			    }
//			    sucesor.f = sucesor.coste + sucesor.heuristica; //1 es la distancia entre un nodo y su padre
//			}
//			
//			//ordenamos los sucesores por la f (compareTo)
//			Collections.sort(sucesores);
//			
//			//seleccionamos el mejor 
//			Nodo mejorSucesor = null;
//			int mejorF = Integer.MAX_VALUE; //inicializamos el mejor minimo al maximo posible
//			
//			for (Nodo sucesor : sucesores) {
//			    if (sucesor.f < mejorF) {
//			        mejorF = sucesor.f;
//			        mejorSucesor = sucesor;
//			    }
//			}
//
//			//actualizamos heurística del nodo actual (usando solo el mejor sucesor)
//			String claveActual = nodo_actual.generarClave();
//			int nuevaHeuristica;
//
//			if (mejorSucesor != null) {
//			    nuevaHeuristica = Math.max(nodo_actual.heuristica, mejorF);
//			} else {
//			    nuevaHeuristica = nodo_actual.heuristica;
//			}
//
//			//guardamos en tabla y actualizar nodo
//			tablaHeuristicas.put(claveActual, nuevaHeuristica);
//			nodo_actual.heuristica = nuevaHeuristica;
//			nodo_actual.f = nuevaHeuristica + nodo_actual.coste;  // f(n) = h(n) + g(n)
//
//			//nos movemos al mejor sucesor si existe
//			if (mejorSucesor != null) {
//			    nodo_actual = mejorSucesor;
//			    actualizarCapas(nodo_actual);
//			}
//			
//			//bucle para imprimir valores de los sucesores y sus heuristicas
//			//			System.out.println("Sucesores: ");
////			for (Nodo sucesor : sucesores) {
////				System.out.println("Posición: " + sucesor.posicion + ", Heuristica: " + sucesor.heuristica + " coste: "+sucesor.coste+ "Capa roja: " + sucesor.capa_roja + ", Capa azul: " + sucesor.capa_azul);
////			}
////			System.out.println("MEJOR VECINO");
////			System.out.println("Posición: " + mejorSucesor.posicion + ", Heuristica: " + mejorSucesor.heuristica + " coste: "+mejorSucesor.coste+ "Capa roja: " + mejorSucesor.capa_roja + ", Capa azul: " + mejorSucesor.capa_azul);
////			
//			
//			//return ruta; //devolvemos la ruta
//			return mejorSucesor.accion_padre;
//				
//		}
//		
////		//función para reconstruir la ruta a partir de un nodo
////		public LinkedList<ACTIONS> reconstruirRuta (Nodo nodoFinal){
////			LinkedList<ACTIONS> ruta= new LinkedList<ACTIONS>(); //como arraylist pero se peude añadir al final y al principio
////			Nodo nodoActual=nodoFinal; 
////			//mientras que no lleguemos al primer nodo añadido (sabemos que el padre del primero es null)
////			while (nodoActual.padre!=null) {
////				ruta.addFirst(nodoActual.accion_padre); //añadimos al principio el nodo padre
////				nodoActual=nodoActual.padre;  //actualizamos el nodo actual 
////			}
////			return ruta; 
////		}
//		
//		//función para comprobar si una posicion del tablero es valida (no hay obstaculo y esta dentro del tablero)
//		private boolean esPosicionValida(Nodo nodo, Vector2d pos, StateObservation stateObs) {
//		    
//			
//			//verificamos los límites del mapa
//		    if (pos.x<0 || pos.y<0 || pos.x>= anchura || pos.y>= altura) {
//		        return false;
//		    }
//		    
////	      Obstáculo - Tipo: 3 → 't' (trampa)
////	      Obstáculo - Tipo: 5 → 'w' (muro normal)
////	      Obstáculo - Tipo: 6 → 'b' (muro azul)
////	      Obstáculo - Tipo: 7 → 'r' (muro rojo)
//		    
//		    //convertimos la posicion a la escala del mundo
//		    Vector2d posMundo = new Vector2d(pos.x * fescala.x, pos.y * fescala.y);
//		    
//		    //verificamos si hay obstáculos en esa posición
//		    for (Observation obstaculo : obstaculos) { //como tenemos los obstaculos en una lista
//		        if (obstaculo.position.equals(posMundo)) {
//			    	if (obstaculo.itype == 5) { //si es un muro normal
//			            return false;
//			        }
//			    	else if (obstaculo.itype == 6) { //si es muro rojo
//			        	return nodo.capa_roja; //si el nodo tiene capa roja
//			        }else if (obstaculo.itype == 7) { //si es muro azul
//			        	return nodo.capa_azul; //si el nodo tiene capa azul
//			        }else if (obstaculo.itype == 3) { //si es trampa
//			        	return false;
//			        }
//		        }
//
//		    }
//		    
//		    return true;
//		}
//		
//		public void actualizarCapas(Nodo nodo_actual) {
//			
//			//primero cambiamos la posicion del nodo actual a la escala del mundo
//			Vector2d posMundo = new Vector2d(nodo_actual.posicion.x * fescala.x,nodo_actual.posicion.y * fescala.y);
////			System.out.println("Posición del nodo actual: " + nodo_actual.posicion + 
////					 ", Capa roja: " + nodo_actual.capa_roja + 
////					 ", Capa azul: " + nodo_actual.capa_azul);
//			//si la casilla donde estamos es capa actualizamos las capas del nodo
//			for (Vector2d capa : nodo_actual.capas_azules) { //iteramos por cada capa
//				if (capa.equals(posMundo)) { //si la posicion del nodo es igual a la de la cap
//					nodo_actual.capa_azul=true; 
//					nodo_actual.capa_roja=false; //no se pueden tener las dos a la vez
//					//eliminamos la capa azul de la lista porque se elimina al recogerla
//					nodo_actual.capas_azules.remove(capa);
//					break; //salimos del bucle si ya hemos encontrado la capa (solo hay una capa por posicion)
//				}
//			}
//			
//			for (Vector2d capa : nodo_actual.capas_rojas) { //iteramos por cada capa
//				if (capa.equals(posMundo)) { //si la posicion del nodo es igual a la de la capa
//					nodo_actual.capa_roja=true;
//					nodo_actual.capa_azul=false; //no se pueden tener las dos a la vez
//					//eliminamos la capa roja de la lista porque se elimina al recogerla
//					nodo_actual.capas_rojas.remove(capa);
//					break; //salimos del bucle si ya hemos encontrado la capa (solo hay una capa por posicion)
//				}
//			}
//		}
//		
//		
//		// funcion para calcular la distancia Manhattane entre dos casillas
//		public int distanciaManhattan(Vector2d inicio, Vector2d fin) {
//			//distancia entre dos puntos (x1,y1) y (x2,y2) es |x1-x2|+|y1-y2|
//			int distancia = Math.abs((int)inicio.x - (int)fin.x) + Math.abs((int)inicio.y - (int)fin.y);
//		    return Math.max(distancia, 0); //si es negativo devolvemos 0
//		}
//		
//		
//}

package tracks.singlePlayer.evaluacion.src_CRIBILLES_PEREZ_MARIA;

import java.util.ArrayList;
import java.util.LinkedList;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class RTA extends AbstractPlayer {
	//ArrayList <Nodo> ruta; //ruta con nodos
		LinkedList <ACTIONS> ruta; //ruta con las acciones a seguir
		Vector2d fescala; //numero de pixeles de cada celda
		Vector2d portal;//posicion portal
		Vector2d pos_avatar; //posicion avatar 
		int nodos_expandidos; //contador de nodos expandidos
		ArrayList <Observation> obstaculos; //observacion obstaculos (objetos inmoviles)
		ArrayList <Observation> capas; //observacion capas
		ArrayList<Vector2d> capasIniciales_rojas; //posiciones de las capas iniciales rojas
		ArrayList<Vector2d> capasIniciales_azules; //posiciones de las capas iniciales azules
		
		
		//Constructor
		public RTA(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
			obstaculos=  new ArrayList<>(); //observacion obstaculos (objetos inmoviles)
			capas=  new ArrayList<>(); //observacion capas
			capasIniciales_rojas = new ArrayList<>(); //posiciones de las capas iniciales rojas
			capasIniciales_azules = new ArrayList<>();
			nodos_expandidos=0; 
			ruta = new LinkedList<>();
			
			//Calculamos el factor de escala entre mundos (pixeles -> grid)
		    fescala = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
		    stateObs.getWorldDimension().height / stateObs.getObservationGrid() [0].length);
		    
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
		    	 if (capa.itype == 8) { //si es capa roja 8
		    		capasIniciales_rojas.add(capa.position); //guardamos la posicion de la capa roja
		    	}else if (capa.itype == 9) {
		    		capasIniciales_azules.add(capa.position); //guardamos la posicion de la capa azul
		    	}	
		    }
		
		    //SE PUEDE SUPONER QUE SOLO VA A HABER UN PORTAL
			//Se crea una lista de observaciones de portales, ordenada por cercanía al avatar 
			ArrayList<Observation>[] posiciones = stateObs.getPortalsPositions(stateObs.getAvatarPosition());
			//Seleccionamos el portal mas proximo 
			portal = posiciones[0].get(0).position;  //cogemos el primer portal (suponemos que es el unico)
			//convertimos a posiciones
			portal.x = Math.floor(portal.x / fescala.x);  
			portal.y = Math.floor(portal.y / fescala.y);
		}
		
		@Override
		public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) { //devuelve la proxima accion
			//si no tenemos ruta la calculamos (solo el primer act) 
			if (ruta.isEmpty()) {
				//posicion del avatar (convertimos getAvatarPosition() con el factor de esacala a un vector2d)
			    pos_avatar= new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
			    stateObs.getAvatarPosition().y / fescala.y);
				ruta=alg_rta(stateObs,elapsedTimer, pos_avatar, portal);
				
				 //no se encuentra solucion
		        if (ruta.isEmpty()) {
		            System.out.println("No se encontró camino al portal");
		            return ACTIONS.ACTION_NIL; //acción por defecto
		        }
			}
			return (ruta.removeFirst()); //si la ruta ya esta calculada devolvemos la primera accion
		}
		
		//algoritmo de busqueda RTA*
		public LinkedList<ACTIONS> alg_rta(StateObservation stateObs, ElapsedCpuTimer elapsedTimer, Vector2d posInicial, Vector2d posFinal){
			int antiguedad=0; 
			
			//actual=nodo_inicial
			Nodo inicial = new Nodo(posInicial,null, 0, 0, Types.ACTIONS.ACTION_NIL, false, false, capasIniciales_rojas, capasIniciales_azules, antiguedad); //inicializamos el nodo inicial
	        
			Nodo nodo_actual=inicial; //nodo actual es el nodo inicial
			
			//mientras no lleguemos a la meta
			while(!posInicial.equals(posFinal)) {
				//comprobar si el nodo en el que esta el avatar es el portal
				if (nodo_actual.posicion.equals(posFinal)) {
					ruta=reconstruirRuta(nodo_actual); //PREGUNTAR A MEESSSSSSSEEEEEEEJOOOOOOOOOOOO
					 // Imprimir estadísticas cuando se encuentra la solución
		            System.out.println("=== RESULTADOS FINALES ===");
		            System.out.println("Nodos expandidos totales: " + nodos_expandidos);
		            System.out.println("Tamaño de la ruta calculada: " + ruta.size() + " acciones");
		            System.out.println("============================");
		            
		            break;
				}
				
				nodos_expandidos++; //aumentamos el contador de nodos expandidos
				
				//expandimos los vecinos del nodo actual
				ArrayList<Nodo> sucesores = new ArrayList<Nodo>(); //arraylist de sucesores
				//orden de las expansiones
				ACTIONS[] ordenAcciones = {ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN};
				for (ACTIONS accion: ordenAcciones) { //iteramos por cada accion disponible
					
					//inicializamos sucesor y depsues lo actualizamos
					Nodo sucesor=null;


					if (accion==ACTIONS.ACTION_RIGHT) {				
						//creamos un nuevo nodo con la accion y posicion correspondiente
						Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x+1, nodo_actual.posicion.y);
						if (esPosicionValida(nodo_actual, nuevaPos, stateObs)) {
							sucesor = new Nodo(nuevaPos, nodo_actual, 0, nodo_actual.coste+1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, antiguedad);
							sucesores.add(sucesor);
						}
					}
					else if (accion==ACTIONS.ACTION_LEFT) {				
						//creamos un nuevo nodo con la accion y posicion correspondiente
						Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x-1, nodo_actual.posicion.y);
						if (esPosicionValida(nodo_actual, nuevaPos, stateObs)) {
							sucesor = new Nodo(nuevaPos, nodo_actual, 0, nodo_actual.coste + 1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, antiguedad);
							sucesores.add(sucesor);
						}
					}
					else if (accion==ACTIONS.ACTION_UP) {				
						//creamos un nuevo nodo con la accion y posicion correspondiente
						Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x, nodo_actual.posicion.y-1);
						if (esPosicionValida(nodo_actual, nuevaPos, stateObs)) {
							sucesor = new Nodo(nuevaPos, nodo_actual, 0, nodo_actual.coste + 1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, antiguedad);
							sucesores.add(sucesor);
						}
					}
					else if (accion==ACTIONS.ACTION_DOWN) {				
						//creamos un nuevo nodo con la accion y posicion correspondiente
						Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x, nodo_actual.posicion.y+1);
						if (esPosicionValida(nodo_actual, nuevaPos, stateObs)) {
							sucesor = new Nodo(nuevaPos, nodo_actual, 0, nodo_actual.coste + 1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, antiguedad);
							sucesores.add(sucesor);
						}
					}
				}
				
				
				//calculamos el coste de cada sucesor
				for (Nodo sucesor : sucesores) { //iteramos por cada sucesor
					//actualizamos las capas del nodo
					//actualizarCapas(sucesor);
					
					//f=g+h
					int f; 
					sucesor.heuristica=distanciaManhattan(sucesor.posicion, posFinal);
					//la distancia entre el actual y el sucesor es 1 siempre
					f=1+sucesor.heuristica; //coste + heuristica
				}
				
				//seleccionamos el mejor sucesor en base al coste calculado
				Nodo mejorSucesor = null;
				int mejorCoste = Integer.MAX_VALUE; //inicializamos el coste al maximo posible
				for (Nodo sucesor : sucesores) { //iteramos por cada sucesor
					//si el coste del sucesor es menor que el mejor coste
					if (sucesor.coste < mejorCoste) {
						mejorCoste = sucesor.coste; //actualizamos el mejor coste
						mejorSucesor = sucesor; //actualizamos el mejor sucesor
					}
				}
				
				//determinamos el segundo minimo en coste
				int segundoMinimo = Integer.MAX_VALUE; //inicializamos el segundo minimo al maximo posible
				for (Nodo sucesor : sucesores) { //iteramos por cada sucesor
					//si el coste del sucesor es menor que el segundo minimo y no es el mejor sucesor
					if (sucesor.coste < segundoMinimo && sucesor != mejorSucesor) {
						segundoMinimo = sucesor.coste; //actualizamos el segundo minimo
					}
				}
				
				//actualizamos el valor heuristico del nodo actual (regla de aprendizaje)
				nodo_actual.heuristica = Math.max(nodo_actual.heuristica, segundoMinimo);
				
				//nos movemos al mejor vecino
				nodo_actual=mejorSucesor; //actualizamos el nodo actual
			}
			
			return ruta; //devolvemos la ruta
				
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
		private boolean esPosicionValida(Nodo nodo, Vector2d pos, StateObservation stateObs) {
		    //tamaño mapa
			int anchura = stateObs.getObservationGrid().length;
		    int altura = stateObs.getObservationGrid()[0].length;
		    
			
			//verificamos los límites del mapa
		    if (pos.x<0 || pos.y<0 || pos.x>= anchura || pos.y>= altura) {
		        return false;
		    }
		    
//	      Obstáculo - Tipo: 3 → 't' (trampa)
//	      Obstáculo - Tipo: 5 → 'w' (muro normal)
//	      Obstáculo - Tipo: 6 → 'b' (muro azul)
//	      Obstáculo - Tipo: 7 → 'r' (muro rojo)
		    
		    //convertimos la posicion a la escala del mundo
		    Vector2d posMundo = new Vector2d(pos.x * fescala.x, pos.y * fescala.y);
		    
		    //verificamos si hay obstáculos en esa posición
		    for (Observation obstaculo : obstaculos) { //como tenemos los obstaculos en una lista
		        if (obstaculo.position.equals(posMundo)) {
			    	if (obstaculo.itype == 5) { //si es un muro normal
			            return false;
			        }
			    	else if (obstaculo.itype == 6) { //si es muro rojo
			        	return nodo.capa_roja; //si el nodo tiene capa roja
			        }else if (obstaculo.itype == 7) { //si es muro azul
			        	return nodo.capa_azul; //si el nodo tiene capa azul
			        }else if (obstaculo.itype == 3) { //si es trampa
			        	return false;
			        }
//			        }else {	
//			        	return false; //si no es ninguno de los anteriores no se puede pasar
//			        }
			        	
		        }

		    }
		    
		    return true;
		}
		
		public void actualizarCapas(Nodo nodo_actual) {
			
			//primero cambiamos la posicion del nodo actual a la escala del mundo
			Vector2d posMundo = new Vector2d(nodo_actual.posicion.x * fescala.x,nodo_actual.posicion.y * fescala.y);
//			System.out.println("Posición del nodo actual: " + nodo_actual.posicion + 
//					 ", Capa roja: " + nodo_actual.capa_roja + 
//					 ", Capa azul: " + nodo_actual.capa_azul);
			//si la casilla donde estamos es capa actualizamos las capas del nodo
			for (Vector2d capa : nodo_actual.capas_azules) { //iteramos por cada capa
				if (capa.equals(posMundo)) { //si la posicion del nodo es igual a la de la capa
					//if (capa.itype == 9) {
						nodo_actual.capa_azul=true; 
						nodo_actual.capa_roja=false; //no se pueden tener las dos a la vez
						//eliminamos la capa azul de la lista porque se elimina al recogerla
						nodo_actual.capas_azules.remove(capa);
					//}	
					break; //salimos del bucle si ya hemos encontrado la capa (solo hay una capa por posicion)
				}
			}
			
			for (Vector2d capa : nodo_actual.capas_rojas) { //iteramos por cada capa
				if (capa.equals(posMundo)) { //si la posicion del nodo es igual a la de la capa
					//if (capa.itype == 8) { //si es capa roja 8 (es su tipo)
						nodo_actual.capa_roja=true;
						nodo_actual.capa_azul=false; //no se pueden tener las dos a la vez
						//eliminamos la capa roja de la lista porque se elimina al recogerla
						nodo_actual.capas_rojas.remove(capa);
					
					//}	
					break; //salimos del bucle si ya hemos encontrado la capa (solo hay una capa por posicion)
				}
			}
		}
		
		
		// funcion para calcular la distancia Manhattane entre dos casillas
		public int distanciaManhattan(Vector2d inicio, Vector2d fin) {
			//distancia entre dos puntos (x1,y1) y (x2,y2) es |x1-x2|+|y1-y2|
			int distancia = Math.abs((int)inicio.x - (int)fin.x) + Math.abs((int)inicio.y - (int)fin.y);
		    return Math.max(distancia, 0); //si es negativo devolvemos 0
		}
		
		
}

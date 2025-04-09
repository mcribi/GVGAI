package tracks.singlePlayer.evaluacion.src_CRIBILLES_PEREZ_MARIA;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class AgenteAstar extends AbstractPlayer {
		LinkedList <ACTIONS> ruta; //ruta con las acciones a seguir
		Vector2d fescala; //numero de pixeles de cada celda
		Vector2d portal;//posicion portal
		Vector2d pos_avatar; //posicion avatar 
		int nodos_expandidos=0; //contador de nodos expandidos
		ArrayList <Observation> obstaculos; //observacion obstaculos (objetos inmoviles)
		ArrayList <Observation> capas=  new ArrayList<>(); //observacion capas
		HashSet<Vector2d> capasIniciales_rojas = new HashSet<>(); //posiciones de las capas iniciales rojas
		HashSet<Vector2d> capasIniciales_azules = new HashSet<>(); //posiciones de las capas iniciales azules
		
		//abiertos y cerrados
		//hacemos una cola con prioridad comparando costes para que se ordenen y saque el que menos coste tenga 
		PriorityQueue<Nodo> abiertos = new PriorityQueue<>();
		HashSet<Nodo> cerrados= new HashSet <>();
		int antiguedad;
		int anchura;
	    int altura;
		
		/* * initialize all variables for the agent
		* @param stateObs Observation of the current state.
		* @param elapsedTimer Timer when the action returned is due.
		*/
		public AgenteAstar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
			//inicializamos la ruta
			ruta = new LinkedList<>();
			
			anchura= stateObs.getObservationGrid().length;
			altura=stateObs.getObservationGrid()[0].length;
			//Calculamos el factor de escala entre mundos (pixeles -> grid) 
			Dimension dimensiones=stateObs.getWorldDimension(); 
		    fescala = new Vector2d(dimensiones.width / anchura, dimensiones.height / altura);
		    
			Vector2d pos=stateObs.getAvatarPosition();
			pos_avatar= new Vector2d(pos.x / fescala.x, pos.y / fescala.y);
			
			//Se crea una lista de observaciones de portales, ordenada por cercanía al avatar 
			ArrayList<Observation>[] posiciones = stateObs.getPortalsPositions(pos);
			//Seleccionamos el portal mas proximo 
			portal = posiciones[0].get(0).position;  //cogemos el primer portal (suponemos que es el unico)
			//convertimos a posiciones
			portal.x = Math.floor(portal.x / fescala.x);  
			portal.y = Math.floor(portal.y / fescala.y);
			
			
			
			
			abiertos = new PriorityQueue<>();
	        cerrados = new HashSet<>();
	        antiguedad=0; 
			obstaculos=  new ArrayList<>();
			
			
		   
		    
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

		}
		
		
		@Override
		public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) { //devuelve la proxima accion
			//si no tenemos ruta la calculamos (solo el primer act) 
			if (ruta.isEmpty()) {
				//posicion del avatar (convertimos getAvatarPosition() con el factor de esacala a un vector2d)
//			    pos_avatar= new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
//			    stateObs.getAvatarPosition().y / fescala.y);
				long tInicio = System.nanoTime();
				ruta=a_star(stateObs,elapsedTimer, pos_avatar, portal);
				long tFin = System.nanoTime();
				long tiempoTotalms = (tFin - tInicio)/1000000;
				//System.out.println("Tiempo total " + tiempoTotalms + " en calcular la ruta con Dijkstra");
				
				
				 //no se encuentra solucion
		        if (ruta.isEmpty()) {
		            System.out.println("No se encontró camino al portal");
		            return ACTIONS.ACTION_NIL; //acción por defecto
		        }
			}
			return (ruta.removeFirst()); //si la ruta ya esta calculada devolvemos la primera accion
		}
		
		public LinkedList<ACTIONS> a_star (StateObservation stateObs, ElapsedCpuTimer elapsedTimer, Vector2d posInicial, Vector2d posFinal) {
			
			
			//metemos el primer nodo (nodo raiz) con su heuristica
	        Nodo inicial = new Nodo(posInicial,null, distanciaManhattan(posInicial, posFinal), 0, Types.ACTIONS.ACTION_NIL, false, false, capasIniciales_rojas, capasIniciales_azules, antiguedad); //inicializamos el nodo inicial
	        abiertos.add(inicial);
	        antiguedad++;
		
			
			while (!abiertos.isEmpty()) {
				//antiguedad++;
				//actual: nodo de menor f=g+h en abiertos
				
				Nodo nodo_actual=abiertos.poll(); //coge el primer nodo de abiertos y lo quita
				
				//saltamos si ya ha sido visitado	    
			    while (cerrados.contains(nodo_actual)) {
			        nodo_actual = abiertos.poll();
			        if (nodo_actual == null) break; // por si ya no hay más abiertos
			    }
				
				//sumamos uno a los nodos expandidos
				nodos_expandidos++; //el nodo actual tambien es expandido
				
				
				
				//comprobar si el nodo en el que esta el avatar es el portal
				//si actual==objetivo (para Vector2d se usa equals())
				if (nodo_actual.posicion.equals(posFinal)) {
					cerrados.add(nodo_actual); //metermos el ultimo nodo en cerrados
					ruta=reconstruirRuta(nodo_actual); 
					 // Imprimir estadísticas cuando se encuentra la solución
		            System.out.println("=== RESULTADOS FINALES ===");
		            System.out.println("Nodos expandidos totales: " + nodos_expandidos);
		            System.out.println("Tamaño de la ruta calculada: " + ruta.size() + " acciones");
		            System.out.println("Tamaño del conjunto cerrados: " + cerrados.size());
		            System.out.println("Nodos restantes en abiertos: " + abiertos.size());
		            System.out.println("============================");
		            
		            break;
				}
				
				//quitamos de abiertos y lo metemos en cerrados
				//abiertos.remove(nodo_actual);
				cerrados.add(nodo_actual);
				
				
				
				ArrayList<Nodo> sucesores= new ArrayList<>(); //inicializamos la lista de sucesores
				//expandir sucesores
				//Quiero que el orden exacto de expansion sea: DERECHA, IZQUIERDA, ARRIBA, ABAJO
				ACTIONS[] ordenAcciones = {ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_LEFT, ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN};
				for ( ACTIONS accion: ordenAcciones) { //iteramos por cada accion disponible
					
					//inicializamos sucesor y nuevaPos pero despues se actualizan
					Nodo sucesor=null;

					if (accion==ACTIONS.ACTION_RIGHT) {				
						//creamos un nuevo nodo con la accion y posicion correspondiente
						Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x+1, nodo_actual.posicion.y);
						if (esPosicionValida(nodo_actual, nuevaPos, stateObs)) {
							int nuevoCoste = nodo_actual.coste + 1;
							int heuristica = distanciaManhattan(nuevaPos, posFinal);
							sucesor = new Nodo(nuevaPos, nodo_actual, heuristica, nuevoCoste, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, antiguedad);
							sucesor.f=sucesor.coste + heuristica;
							//antiguedad++;
							sucesores.add(sucesor);
						}
					}
					else if (accion==ACTIONS.ACTION_LEFT) {				
						//creamos un nuevo nodo con la accion y posicion correspondiente
						Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x-1, nodo_actual.posicion.y);
						if (esPosicionValida(nodo_actual, nuevaPos, stateObs)) {
							int nuevoCoste = nodo_actual.coste + 1;
							int heuristica = distanciaManhattan(nuevaPos, posFinal);
							sucesor = new Nodo(nuevaPos, nodo_actual, heuristica, nuevoCoste, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, antiguedad);
							sucesor.f=sucesor.coste + heuristica;
							sucesores.add(sucesor);
							//antiguedad++;
						}
					}
					else if (accion==ACTIONS.ACTION_UP) {				
						//creamos un nuevo nodo con la accion y posicion correspondiente
						Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x, nodo_actual.posicion.y-1);
						if (esPosicionValida(nodo_actual, nuevaPos, stateObs)) {
							int nuevoCoste = nodo_actual.coste + 1;
							int heuristica = distanciaManhattan(nuevaPos, posFinal);
							sucesor = new Nodo(nuevaPos, nodo_actual, heuristica, nuevoCoste, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, antiguedad);
							sucesor.f=sucesor.coste + heuristica;
							sucesores.add(sucesor);
							//antiguedad++;
						}
					}
					else if (accion==ACTIONS.ACTION_DOWN) {				
						//creamos un nuevo nodo con la accion y posicion correspondiente
						Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x, nodo_actual.posicion.y+1);
						if (esPosicionValida(nodo_actual, nuevaPos, stateObs)) {
							int nuevoCoste = nodo_actual.coste + 1;
							int heuristica = distanciaManhattan(nuevaPos, posFinal);
							sucesor = new Nodo(nuevaPos, nodo_actual, heuristica, nuevoCoste, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, antiguedad);
							sucesor.f=sucesor.coste + heuristica;
							sucesores.add(sucesor);
							//antiguedad++;
						}
					}
					

					
					for (Nodo suc : sucesores) { //si el sucesor es valido
					    //calculamos el nuevo coste desde el nodo actual
					    int nuevoG = nodo_actual.coste + 1;	

					    if (cerrados.contains(suc)) {
					       for (Nodo cer:cerrados) {
					    	   if (cer.equals(suc)) {
							    	if (nuevoG < cer.coste) {
							            //hemos encontrado un mejor camino, actualizamos
							            cerrados.remove(cer);
							            suc.padre = nodo_actual;
							            suc.coste = nuevoG;
							            suc.f = suc.coste + distanciaManhattan(suc.posicion, posFinal);
							            actualizarCapas(suc);
							            abiertos.add(suc);
							            antiguedad++;
							        }
							    	break;
							    	
					    	   }
					       }
					    } else if (abiertos.contains(suc)) {
					        for (Nodo ab:abiertos) {
					        	if (ab.equals(suc)) {
						        	if (nuevoG < ab.coste) {
						        		abiertos.remove(ab);
						        		// Ya está en abiertos pero encontramos un camino mejor
							        	suc.padre = nodo_actual;
							        	suc.coste = nuevoG;
							        	suc.f = suc.coste + distanciaManhattan(suc.posicion, posFinal);
							            actualizarCapas(suc);
							            suc.antiguedad = nodo_actual.antiguedad; // Actualizamos la antigüedad
							            actualizarCapas(suc);
							            abiertos.add(suc);
							            antiguedad++; 
							        }
						        	break;
					        	}
					        }
					    } else {
					        // Nodo nuevo, se configura todo y se añade a abiertos
					    	suc.padre = nodo_actual;
					    	suc.coste = nuevoG;
					    	suc.f = suc.coste + distanciaManhattan(suc.posicion, posFinal);
					        actualizarCapas(suc);
					        abiertos.add(suc);
					        antiguedad++;
					    }
					}

					
				}
				
				//por cada sucesor
				
			}
		
		
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
			        }else if (obstaculo.itype == 6) { //si es muro rojo
			        	return nodo.capa_roja; //si el nodo tiene capa roja
			        }else if (obstaculo.itype == 7) { //si es muro azul
			        	return nodo.capa_azul; //si el nodo tiene capa azul
			        }else if (obstaculo.itype == 3) { //si es trampa
			        	return false;
			        }
			        	
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
					nodo_actual.capa_azul=true; 
					nodo_actual.capa_roja=false; //no se pueden tener las dos a la vez
					//eliminamos la capa azul de la lista porque se elimina al recogerla
					nodo_actual.capas_azules.remove(capa);
					break; //salimos del bucle si ya hemos encontrado la capa (solo hay una capa por posicion)
				}
			}
			
			for (Vector2d capa : nodo_actual.capas_rojas) { //iteramos por cada capa
				if (capa.equals(posMundo)) { //si la posicion del nodo es igual a la de la capa
					nodo_actual.capa_roja=true;
					nodo_actual.capa_azul=false; //no se pueden tener las dos a la vez
					//eliminamos la capa roja de la lista porque se elimina al recogerla
					nodo_actual.capas_rojas.remove(capa);
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

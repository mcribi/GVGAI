package tracks.singlePlayer.evaluacion.src_CRIBILLES_PEREZ_MARIA;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class AgenteRTAStar extends AbstractPlayer {
		LinkedList <ACTIONS> ruta; //ruta con las acciones a seguir
		Vector2d fescala; //numero de pixeles de cada celda
		Vector2d portal;//posicion portal
		Vector2d pos_avatar; //posicion avatar 
		int nodos_expandidos; //contador de nodos expandidos
		HashSet <Observation> obstaculos; //observacion obstaculos (objetos inmoviles)
		HashSet <Observation> capas; //observacion capas
		HashSet<String> capasIniciales_rojas; //posiciones de las capas iniciales rojas
		HashSet<String> capasIniciales_azules; //posiciones de las capas iniciales azules
		Nodo nodo_actual;
		long tiempoTotalms; 
		int antiguedad; //para el criterio de desempate
		
	    //tamaño mapa
		int anchura;
	    int altura;
		
		//tabla hash (posicion del vector + heuristica) para almacenar heuristicas de los nodos
		HashMap<String, Integer> tablaHeuristicas; //la posicion del vector la codificamos como un string para poder acceder a el y que no haya duplicados
		
		//para optimizacion: 
	    private boolean[][] esPosicionValida; // true = transitable, false = obstáculo
	    private boolean[][] esMuroRojo; //posiciones de muros rojos
	    private boolean[][] esMuroAzul; //posiciones de muros azules
	    
	    
		//Constructor (inicializamos todas las variables)
		public AgenteRTAStar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
			obstaculos=  new HashSet<>(); //observacion obstaculos (objetos inmoviles)
			capas=  new HashSet<>(); //observacion capas
			capasIniciales_rojas = new HashSet<>(); //posiciones de las capas iniciales rojas
			capasIniciales_azules = new HashSet<>();
			nodos_expandidos=0; 
			ruta = new LinkedList<>();
			tablaHeuristicas= new HashMap<>();
			tiempoTotalms=0; 
			antiguedad=0; 
			
			
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
		
		@Override
		public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) { //devuelve la proxima accion
			//lamamos al algoritmo en tiempo real para que cada vez nos devuelva una accion
		    ACTIONS accion=alg_rta(stateObs,elapsedTimer, pos_avatar, portal);
		    			
			return accion;
		}
		
		//algoritmo de busqueda RTA*: al ser en tiempo real no devolvemos una ruta completa, si no que accion en accion
		public ACTIONS alg_rta(StateObservation stateObs, ElapsedCpuTimer elapsedTimer, Vector2d posInicial, Vector2d posFinal){
			long tInicio = System.nanoTime();
			if (nodo_actual==null) {
				
				//actual=nodo_inicial
				int heuristica_inicial=distanciaManhattan(posInicial, posFinal); 
				nodo_actual = new Nodo(posInicial,null, heuristica_inicial, 0, Types.ACTIONS.ACTION_NIL, false, false, capasIniciales_rojas, capasIniciales_azules, antiguedad); //inicializamos el nodo inicial
				
				//generamos clave para la tabla hash
				String clave=nodo_actual.generarClave();
				tablaHeuristicas.put(clave, nodo_actual.heuristica); //metemos la primera heuristica en la tabla hash
			}
			
			nodos_expandidos++; //aumentamos el contador de nodos expandidos
			
			
			//comprobamos si el nodo en el que esta el avatar es el portal
			if (nodo_actual.posicion.equals(posFinal)) {
				//aqui realmente nunca entra porque el juego acaba antes al llegar al portal
				
				return ACTIONS.ACTION_NIL;
			}
			
			
			
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
					if (esPosicionValida(nodo_actual, nuevaPos)) { //despues actualizamos la heuristica
						sucesor = new Nodo(nuevaPos, nodo_actual, 0, 1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas,nodo_actual.capas_azules, nodo_actual.antiguedad+1);
						actualizarCapas(sucesor);
						sucesores.add(sucesor);
					}
				}
				else if (accion==ACTIONS.ACTION_LEFT) {				
					//creamos un nuevo nodo con la accion y posicion correspondiente
					Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x-1, nodo_actual.posicion.y);
					if (esPosicionValida(nodo_actual, nuevaPos)) {
						sucesor = new Nodo(nuevaPos, nodo_actual, 0, 1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul,nodo_actual.capas_rojas, nodo_actual.capas_azules, nodo_actual.antiguedad+1);
						actualizarCapas(sucesor);
						sucesores.add(sucesor);
					}
				}
				else if (accion==ACTIONS.ACTION_UP) {				
					//creamos un nuevo nodo con la accion y posicion correspondiente
					Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x, nodo_actual.posicion.y-1);
					if (esPosicionValida(nodo_actual, nuevaPos)) {
						sucesor = new Nodo(nuevaPos, nodo_actual, 0, 1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, nodo_actual.antiguedad+1);
						actualizarCapas(sucesor);
						sucesores.add(sucesor);
					}
				}
				else if (accion==ACTIONS.ACTION_DOWN) {				
					//creamos un nuevo nodo con la accion y posicion correspondiente
					Vector2d nuevaPos = new Vector2d(nodo_actual.posicion.x, nodo_actual.posicion.y+1);
					if (esPosicionValida(nodo_actual, nuevaPos)) {
						sucesor = new Nodo(nuevaPos, nodo_actual, 0, 1, accion, nodo_actual.capa_roja, nodo_actual.capa_azul, nodo_actual.capas_rojas, nodo_actual.capas_azules, nodo_actual.antiguedad+1);
						actualizarCapas(sucesor);
						sucesores.add(sucesor);
					}
				}
			}
			
			if (sucesores.isEmpty()) {  //por si no hubiese camino
	            System.out.println("No hay sucesores válidos");
	            return ACTIONS.ACTION_NIL;
	        }
			
			
			//calculamos el coste de cada sucesor y las actualizamos
			for (Nodo sucesor : sucesores) { //iteramos por cada sucesor
				String clave_suc=sucesor.generarClave(); //generamos la clave hash
				if (tablaHeuristicas.containsKey(clave_suc)) { //si ya esta en la tabla
			        sucesor.heuristica = tablaHeuristicas.get(clave_suc);
			    } else { //si es la primera vez es la distancia manhattan, la introducimos
			        sucesor.heuristica = distanciaManhattan(sucesor.posicion, posFinal);
			        String clave_sucesor=sucesor.generarClave();
			        tablaHeuristicas.put(clave_sucesor, sucesor.heuristica); //metemos la heuristica en la tabla hash
			    }
			    sucesor.f = sucesor.coste + sucesor.heuristica; //1 es la distancia entre un nodo y su padre
			}
			
			//comprobamos si alguno de los sucesores es el portal
			for (Nodo sucesor : sucesores) { //para poder imprimir los resultados antes de que el programa termine 
			    if (sucesor.posicion.equals(posFinal)) {
					//imprimimos tiempos y resultados cuando se encuentra la solución
					int pasos = 1;	//contador de pasos (lo inicializamos en 1 porque todavia tenemos que hacer el paso del nodo actual al sucesor)
					Nodo n = nodo_actual;
					while (n.padre != null) {
					    pasos++;
					    n = n.padre;
					}
					
					//resultados
					System.out.println("Tamaño de la ruta total: " + pasos);
					System.out.println("Nodos expandidos totales: " + nodos_expandidos);
					System.out.println("Tiempo total acumulado (ms): " + tiempoTotalms);
					
			    }
			}
			
			//ordenamos los sucesores por la f (compareTo)
			Collections.sort(sucesores);
			
			//seleccionamos el mejor y el segundo mejor (de este solo el valor de la heuristica)
			Nodo mejorSucesor = null;
			Nodo segundoMejor = null;
			int mejorF = Integer.MAX_VALUE; //inicializamos el mejor minimo al maximo posible
			int segundoMejorF = Integer.MAX_VALUE; //inicializamos el segundo minimo al maximo posible
			
			if (sucesores.size() >= 1) { //el mejor valor
			    mejorSucesor = sucesores.get(0); //cogemos el primero que como estan ordenados es el mejor sucesor
			    mejorF = mejorSucesor.f; //guardamos f
			}
			if (sucesores.size() >= 2) { //si hay mas de 2 sucesores (podemos considerar el segundo mejor)
			    segundoMejor = sucesores.get(1);  //el segundo mejor valor (puede ser igual que el mejor)
			    segundoMejorF = segundoMejor.f;
			} else if (sucesores.size() == 1) { //si solo hubiese un sucesor
			    segundoMejorF = mejorF;  //solo hay un sucesor, es ese f
			}
			
			//actualizamos la heurística del nodo actual (la del segundo)
			int nuevaHeuristica;
			String clave_actual=nodo_actual.generarClave(); //generamos clave para tabla hash
			if (sucesores.size() == 1) {
			    //cuando solo hay un sucesor
			    nuevaHeuristica = Math.max(nodo_actual.heuristica, mejorF);
				
			} else {
			    //cuando hay mas de un sucesor
				nuevaHeuristica = Math.max(nodo_actual.heuristica, segundoMejorF);
			}
			tablaHeuristicas.put(clave_actual, nuevaHeuristica); //metemos la nueva heuristica en la tabla hash
			nodo_actual.heuristica = nuevaHeuristica;
			nodo_actual.f=nuevaHeuristica + nodo_actual.coste; //actualizamos el f del nodo actual

			//nos movemos al mejor sucesor si existe
			if (mejorSucesor != null) {
			    nodo_actual = mejorSucesor;
			    actualizarCapas(nodo_actual);
			}
			
			long tFin = System.nanoTime();
			long duracionMs = (tFin - tInicio) / 1000000;
			tiempoTotalms += duracionMs; //acumulamos el tiempo total
			return mejorSucesor.accion_padre; //devolvemos solo una accion
		}

		
		//función para comprobar si una posicion del tablero es valida (no hay obstaculo y esta dentro del tablero)
		private boolean esPosicionValida(Nodo nodo, Vector2d pos) {
			//verificamos los límites del mapa
		    if (pos.x < 0 || pos.y < 0 || pos.x >= anchura || pos.y >= altura) {
		        return false;
		    }
		    
//	    Obstáculo - Tipo: 3 → 't' (trampa)
//	    Obstáculo - Tipo: 5 → 'w' (muro normal)
//	    Obstáculo - Tipo: 7 → 'b' (muro azul)
//	    Obstáculo - Tipo: 6 → 'r' (muro rojo)
		    
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
		
		
		// funcion para calcular la distancia Manhattane entre dos casillas
		public int distanciaManhattan(Vector2d inicio, Vector2d fin) {
			//distancia entre dos puntos (x1,y1) y (x2,y2) es |x1-x2|+|y1-y2|
			int distancia = Math.abs((int)inicio.x - (int)fin.x) + Math.abs((int)inicio.y - (int)fin.y);
		    return Math.max(distancia, 0); //si es negativo devolvemos 0
		}
		
		
}

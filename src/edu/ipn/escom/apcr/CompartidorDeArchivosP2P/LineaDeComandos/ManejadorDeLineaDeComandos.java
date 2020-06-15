package edu.ipn.escom.apcr.CompartidorDeArchivosP2P.LineaDeComandos;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import edu.ipn.escom.apcr.CompartidorDeArchivosP2P.CompartidorDeArchivosP2P;

public class ManejadorDeLineaDeComandos {
	
	public void mostrarMensajeBienvenida() {
		System.out.println();
		System.out.println("****************************************************");
		System.out.println("*                                                  *");
		System.out.println("*     Bienvenido al compartidor de archivos P2P    *");
		System.out.println("*                                                  *");
		System.out.println("* Creado por:                                      *");
		System.out.println("*     Balderas Aceves Lidia Lizbeth                *");
		System.out.println("*     Corona López Emilio Abraham                  *");
		System.out.println("*                                                  *");
		System.out.println("* Grupo: 3CM8                                      *");
		System.out.println("*                                                  *");
		System.out.println("* Materia: Aplicaciones para comunicaciones en red *");
		System.out.println("*                                                  *");
		System.out.println("* Profesora: Perez de los Santos Mondragon Tanibet *");
		System.out.println("*                                                  *");
		System.out.println("* Semestre: 2020-A                                 *");
		System.out.println("*                                                  *");
		System.out.println("* Trabajo: Problema 2                              *");
		System.out.println("*                                                  *");
		System.out.println("****************************************************");
		System.out.println();
	}
	
	public void mostrarMensajeDespedida() {
		System.out.println("Hasta luego! \n");
	}
	
	public void mostrarEstadoConexion(short estadoConexion) {
		System.out.print("Conectado a la red P2P: ");
		if(estadoConexion == CompartidorDeArchivosP2P.ESTADO_CONEXION_CONECTADO_A_RED_P2P) {
			System.out.print(ANSIConsoleColors.GREEN_BOLD+"Si"+ANSIConsoleColors.RESET+"\n");
		}else {
			System.out.print(ANSIConsoleColors.RED_BOLD+"No"+ANSIConsoleColors.RESET+"\n");
		}
	}
	
	public File solicitarRutaParaCompartir() {
		Scanner scn = new Scanner(System.in);
		File directorioParaCompartir = null;
		boolean rutaValida = false;
		while(!rutaValida) {
			System.out.println("");
			System.out.println("Ingrese la ruta que desea compartir en la red P2P: ");
			System.out.println("");
			String rutaParaCompartir = scn.next();
			directorioParaCompartir = new File(rutaParaCompartir);
			if(directorioParaCompartir!=null && directorioParaCompartir.exists() && directorioParaCompartir.isDirectory()) {
				rutaValida = true;
			}else {
				System.out.println("La ruta es invalida");
			}
		}
		return directorioParaCompartir;
	}
	
	public File solicitarRutaDescargas() {
		Scanner scn = new Scanner(System.in);
		File directorioParaDescargas = null;
		boolean rutaValida = false;
		while(!rutaValida) {
			System.out.println("");
			System.out.println("Ingrese la ruta para las descargas: ");
			System.out.println("");
			String rutaParaDescargas = scn.next();
			directorioParaDescargas = new File(rutaParaDescargas);
			if(directorioParaDescargas!=null && directorioParaDescargas.exists() && directorioParaDescargas.isDirectory()) {
				rutaValida = true;
			}else {
				System.out.println("La ruta es invalida");
			}
		}
		return directorioParaDescargas;
	}
	
	public short solicitarOperacion() {
		Scanner scn = new Scanner(System.in);
		System.out.println("");
		System.out.println("Seleccione una operacion: ");
		System.out.println("");
		System.out.println("\t 1) Conectarse a la red P2P");
		System.out.println("\t 2) Desconectarse de la red P2P");
		System.out.println("\t 3) Ver archivos disponibles en la red P2P");
		System.out.println("\t 4) Descargar archivo de la red P2P");
		System.out.println("\t 5) Salir");
		boolean operacionValida = false;
		
		short operacion = 0;
		while(!operacionValida) {
			try {
				operacion = Short.valueOf(scn.next());
				if(operacion>=1 && operacion<=6) {
					operacionValida = true;
				}else {
					System.out.println("La operacion ingresada no es valida");
				}
			}catch(NumberFormatException ex) {
				System.out.println("La operacion ingresada no es valida");
			}	
		}
		
		return operacion;
	}
	
	public void limpiarLineaDeComandos(){
	    System.out.print("\033[H\033[2J");  
	    System.out.flush();  
	}
	
	public String solicitarDireccionIPDeBusqueda() {
		LinkedList<String> direccionesIP = new LinkedList<String>();
		Scanner scn = new Scanner(System.in);
		System.out.println("Seleccione la interfaz de red e IP con la cual desea crear o unirse a la red P2P ");
		System.out.println();
		try {
			Enumeration<NetworkInterface> inferfacesDeRed = NetworkInterface.getNetworkInterfaces();
			int numeroDeOpcion = 1;
			while(inferfacesDeRed.hasMoreElements()){
				NetworkInterface interfazDeRed = inferfacesDeRed.nextElement();
				List<InterfaceAddress> direccionesIPdeInterfaz = interfazDeRed.getInterfaceAddresses();
			    for(InterfaceAddress direccionIPdeInterfaz:direccionesIPdeInterfaz) {
			    	System.out.print(numeroDeOpcion+") ");
			    	System.out.print(ANSIConsoleColors.WHITE_BOLD+""+interfazDeRed+ANSIConsoleColors.RESET+" - ");
			    	System.out.print(direccionIPdeInterfaz.getAddress().getHostAddress()+"/"+direccionIPdeInterfaz.getNetworkPrefixLength()+"\n");
			        direccionesIP.add(direccionIPdeInterfaz.getAddress().getHostAddress()+"/"+direccionIPdeInterfaz.getNetworkPrefixLength());
			        numeroDeOpcion++;
			    }
				
			}
			
			boolean opcionValida = false;
			
		    int opcion = 0;
			while(!opcionValida) {
				try {
					opcion = Integer.valueOf(scn.next());
					if(opcion>=1 && opcion<=numeroDeOpcion) {
						opcionValida = true;
					}else {
						System.out.println("La opcion ingresada no es valida");
					}
				}catch(NumberFormatException ex) {
					System.out.println("La opcion ingresada no es valida");
				}	
			}
			return direccionesIP.get(opcion-1);
		} catch (SocketException e1) {
			e1.printStackTrace();
			return null;
		}
		
	}
	
	public void mostrarArchivosRedP2P(LinkedList<String> nombresArchivos) {
		System.out.println();
		System.out.println("Archivos disponibles en la red P2P: ");
		System.out.println();
		for(String nombreArchivo:nombresArchivos) {
			System.out.println("\t"+nombreArchivo);
		}
	}
	
	public String solicitarNombreArchivoRemoto() {
		System.out.println("Ingrese el nombre del archivo remoto a descargar: ");
		return new Scanner(System.in).next();
	}
	
	public void mostrarDescargandoArchivo(String rutaDescarga) {
		System.out.println("Descargando archivo en: "+rutaDescarga);
	}
	
	public void mostrarArchivoDescargado() {
		System.out.println("Archivo descargado");
	}
	
	public void mostrarBuscandoPares() {
		System.out.println("Buscando pares...");
	}
	
	public void mostrarHostsEscaneados(int hostEscaneado,int hostsTotales) {
		System.out.println("Escaneando host "+hostEscaneado+" de "+hostsTotales);
	}
	
	public void mostrarParesEncontrados(int paresEncontrados) {
		System.out.println("Pares encontrados: "+paresEncontrados);
	}
	
	public void mostrarListandoArchivosRedP2P() {
		System.out.println("Listando archivos de red P2P...");
	}
}

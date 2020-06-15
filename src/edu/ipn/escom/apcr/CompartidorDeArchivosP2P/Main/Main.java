package edu.ipn.escom.apcr.CompartidorDeArchivosP2P.Main;

import java.io.File;

import edu.ipn.escom.apcr.CompartidorDeArchivosP2P.CompartidorDeArchivosP2P;
import edu.ipn.escom.apcr.CompartidorDeArchivosP2P.LineaDeComandos.ManejadorDeLineaDeComandos;

public class Main {
	
	public static final boolean EVER = true;
	
	public static void main(String[] args) {
		
		ManejadorDeLineaDeComandos manejadorDeLineaDeComandos = new ManejadorDeLineaDeComandos();
		manejadorDeLineaDeComandos.mostrarMensajeBienvenida();
		File directorioParaCompartir = manejadorDeLineaDeComandos.solicitarRutaParaCompartir();
		File directorioDescargas = manejadorDeLineaDeComandos.solicitarRutaDescargas();
		CompartidorDeArchivosP2P compartidorDeArchivosP2P = new CompartidorDeArchivosP2P(manejadorDeLineaDeComandos,directorioParaCompartir,directorioDescargas);
		for(;EVER;) {
			manejadorDeLineaDeComandos.mostrarEstadoConexion(compartidorDeArchivosP2P.getEstadoConexionRedP2P());
			short operacion = manejadorDeLineaDeComandos.solicitarOperacion();
			if(operacion == 5) {
				compartidorDeArchivosP2P.ejecutarOperacion((short)2);
				manejadorDeLineaDeComandos.mostrarMensajeDespedida();
				break;
			}else {
				compartidorDeArchivosP2P.ejecutarOperacion(operacion);
			}
		}
	}
}

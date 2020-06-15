package edu.ipn.escom.apcr.CompartidorDeArchivosP2P.util;

import java.io.File;
import java.util.LinkedList;

public class FileUtils {
	
	public static LinkedList<String> listarDirectorio(String prefijo,File directorio){
		LinkedList<String> listadoDirectorio = new LinkedList<String>();
		for(File entradaDirectorio:directorio.listFiles()) {
			if(entradaDirectorio.isDirectory()) {
				listadoDirectorio.addAll(listarDirectorio(prefijo+entradaDirectorio.getName()+"/",entradaDirectorio));
			}else {
				listadoDirectorio.add(prefijo+entradaDirectorio.getName());
			}
		}
		return listadoDirectorio;
	}
}

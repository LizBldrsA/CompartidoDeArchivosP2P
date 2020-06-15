package edu.ipn.escom.apcr.CompartidorDeArchivosP2P;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.net.util.SubnetUtils;

import edu.ipn.escom.apcr.CompartidorDeArchivosP2P.LineaDeComandos.ManejadorDeLineaDeComandos;
import edu.ipn.escom.apcr.CompartidorDeArchivosP2P.util.FileUtils;

public class CompartidorDeArchivosP2P {
	
	public static final boolean EVER = true;
	
	public static final int PUERTO_DE_COMUNICACION_CONTROL = 28273;
	public static final int PUERTO_DE_COMUNICACION_DATOS = 28274;
	
	public static final short OPERACION_CONECTARSE_A_RED_P2P = 1;
	public static final short OPERACION_DESCONECTARSE_DE_RED_P2P = 2;
	public static final short OPERACION_VER_ARCHIVOS_DISPONIBLES_RED_P2P = 3;
	public static final short OPERACION_DESCARGAR_ARCHIVO_RED_P2P = 4;
	
	public static final short ESTADO_CONEXION_CONECTADO_A_RED_P2P = 5;
	public static final short ESTADO_CONEXION_DESCONECTADO_DE_RED_P2P = 6;

	public static final String SOLICITUD_PAR_LISTAR_DIRECTORIO_COMPARTIDO = "LISTAR_DIRECTORIO_COMPARTIDO";
	public static final String SOLICITUD_PAR_DESCARGAR_ARCHIVO = "DESCARGAR_ARCHIVO";
	public static final String SOLICITUD_PAR_DESCONEXION = "DESCONEXION";
	
	private volatile Short estadoConexionRedP2P = ESTADO_CONEXION_DESCONECTADO_DE_RED_P2P;
	
	private volatile List<Socket> socketsControlParesConectados = Collections.synchronizedList(new ArrayList<Socket>());
	private volatile List<Socket> socketsDatosParesConectados = Collections.synchronizedList(new ArrayList<Socket>());
	
	private File directorioParaCompartir;
	private File directorioDescargas;

	private ManejadorDeLineaDeComandos manejadorDeLineaDeComandos;
	
	private ServerSocket socketEsperaParesControl = null;
	private ServerSocket socketEsperaParesDatos = null;
	
	public CompartidorDeArchivosP2P(ManejadorDeLineaDeComandos manejadorDeLineaDeComandos,File directorioParaCompartir,File directorioDescargas){
		this.manejadorDeLineaDeComandos = manejadorDeLineaDeComandos;
		this.directorioParaCompartir = directorioParaCompartir;
		this.directorioDescargas = directorioDescargas;
	}
	
	private void esperarConexionesPares() {
		try {
			socketEsperaParesControl = new ServerSocket(PUERTO_DE_COMUNICACION_CONTROL);
			socketEsperaParesDatos = new ServerSocket(PUERTO_DE_COMUNICACION_DATOS);
			for(;EVER;) {
				Socket socketParControl = socketEsperaParesControl.accept();
				Socket socketParDatos = socketEsperaParesDatos.accept();
				new Thread(new Runnable() {
					@Override
					public void run() {
						atenderPar(socketParControl,socketParDatos);
					}
				}).start();
				synchronized (socketsControlParesConectados) {
					socketsControlParesConectados.add(socketParControl);
				}
				synchronized (socketsDatosParesConectados) {
					socketsDatosParesConectados.add(socketParDatos);
				}
				synchronized (estadoConexionRedP2P) {
					if(estadoConexionRedP2P == ESTADO_CONEXION_DESCONECTADO_DE_RED_P2P) {
						socketEsperaParesControl.close();
						socketEsperaParesDatos.close();
						break;
					}
				}
			}
		} catch (IOException e) {
			
		}
	}
	
	private void atenderPar(Socket socketParControl,Socket socketParDatos) {
		try {
			DataInputStream flujoEntradaDatosParControl = new DataInputStream(socketParControl.getInputStream());
			DataOutputStream flujoSalidaDatosParControl = new DataOutputStream(socketParControl.getOutputStream());
			
			DataOutputStream flujoSalidaDatosParDatos = new DataOutputStream(socketParDatos.getOutputStream());
			DataInputStream flujoEntradaDatosParDatos = new DataInputStream(socketParDatos.getInputStream());
			
			for(;EVER;) {
				String solicitudPar = flujoEntradaDatosParControl.readUTF();
				if(solicitudPar.equals(SOLICITUD_PAR_LISTAR_DIRECTORIO_COMPARTIDO)) {
					flujoSalidaDatosParDatos.writeUTF(Arrays.toString(FileUtils.listarDirectorio("",directorioParaCompartir).toArray()).replaceAll("\\[","").replaceAll("\\]",""));
				}else if(solicitudPar.equals(SOLICITUD_PAR_DESCARGAR_ARCHIVO)) {
					String nombreArchivoDescargar = flujoEntradaDatosParDatos.readUTF();
					File archivoAEnviar = new File(directorioParaCompartir.getAbsolutePath()+"/"+nombreArchivoDescargar);
					FileInputStream fileInputStream = new FileInputStream(archivoAEnviar);
					long tamanoArchivo = archivoAEnviar.length();
					flujoSalidaDatosParDatos.writeLong(tamanoArchivo);
					int bytesLeidos = 0;
					byte[] buffer = new byte[4092];
					while (tamanoArchivo > 0 && (bytesLeidos = fileInputStream.read(buffer, 0, (int)Math.min(buffer.length, tamanoArchivo))) != -1){
						socketParDatos.getOutputStream().write(buffer,0,bytesLeidos);
					  tamanoArchivo -= bytesLeidos;
					}
					fileInputStream.close();
				}else if(solicitudPar.equals(SOLICITUD_PAR_DESCONEXION)) {

					flujoSalidaDatosParControl.writeUTF(SOLICITUD_PAR_DESCONEXION); 
					synchronized (socketsControlParesConectados) {
						socketsControlParesConectados.remove(socketParControl);
					}
					socketParControl.close();
					
					synchronized (socketsDatosParesConectados) {
						socketsDatosParesConectados.remove(socketParDatos);
					}
					socketParDatos.close();
					break;
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public void ejecutarOperacion(short operacion) {
		if(operacion == OPERACION_CONECTARSE_A_RED_P2P) {
			manejadorDeLineaDeComandos.limpiarLineaDeComandos();
			conectarseARedP2P();
			manejadorDeLineaDeComandos.limpiarLineaDeComandos();
		}else if (operacion == OPERACION_DESCONECTARSE_DE_RED_P2P) {
			manejadorDeLineaDeComandos.limpiarLineaDeComandos();
			desconectarseDeRedP2P();
			manejadorDeLineaDeComandos.limpiarLineaDeComandos();
		}else if (operacion == OPERACION_VER_ARCHIVOS_DISPONIBLES_RED_P2P) {
			manejadorDeLineaDeComandos.limpiarLineaDeComandos();
			manejadorDeLineaDeComandos.mostrarListandoArchivosRedP2P();
			LinkedList<String> listaArchivosRedP2P = obtenerArchivosCompartidosRedP2P();
			manejadorDeLineaDeComandos.limpiarLineaDeComandos();
			manejadorDeLineaDeComandos.mostrarArchivosRedP2P(listaArchivosRedP2P);
		}else if (operacion == OPERACION_DESCARGAR_ARCHIVO_RED_P2P) {
			String nombreArchivoRemoto = manejadorDeLineaDeComandos.solicitarNombreArchivoRemoto();
			manejadorDeLineaDeComandos.mostrarDescargandoArchivo(directorioDescargas.getAbsolutePath()+"/"+nombreArchivoRemoto.split("/")[nombreArchivoRemoto.split("/").length-1]);
			descargarArchivoCompartidoRedP2P(nombreArchivoRemoto,nombreArchivoRemoto.split("/")[nombreArchivoRemoto.split("/").length-1]);
			manejadorDeLineaDeComandos.mostrarArchivoDescargado();
		}
	}
	
	public void descargarArchivoCompartidoRedP2P(String nombreArchivoRemoto,String nombreArchivoLocal) {
		String direccionIPpar = nombreArchivoRemoto.split("/")[0];
		String nombreArchivoPar = nombreArchivoRemoto.substring(nombreArchivoRemoto.indexOf("/")+1,nombreArchivoRemoto.length());
		synchronized (socketsControlParesConectados) {
			synchronized(socketsDatosParesConectados) {
				Iterator<Socket> iteradorParesConectados = socketsControlParesConectados.iterator();
				int i = 0;
			    while (iteradorParesConectados.hasNext()) {
			    	try {
			    		Socket socketParControl = iteradorParesConectados.next();
			    		Socket socketParDatos = socketsDatosParesConectados.get(i);
						if(socketParControl.getInetAddress().getHostAddress().equals(direccionIPpar)) {
							
							DataOutputStream flujoSalidaDatosParControl = new DataOutputStream(socketParControl.getOutputStream());

							DataOutputStream flujoSalidaDatosParDatos = new DataOutputStream(socketParDatos.getOutputStream());
							DataInputStream flujoEntradaDatosParDatos = new DataInputStream(socketParDatos.getInputStream());
							
							flujoSalidaDatosParControl.writeUTF(SOLICITUD_PAR_DESCARGAR_ARCHIVO); 
							
							flujoSalidaDatosParDatos.writeUTF(nombreArchivoPar);
							FileOutputStream fileOutputStream = new FileOutputStream(directorioDescargas.getAbsolutePath()+"/"+nombreArchivoLocal);
							long tamanoArchivo = flujoEntradaDatosParDatos.readLong();
							int bytesLeidos = 0;
							byte[] buffer = new byte[4092];
							while (tamanoArchivo > 0 && (bytesLeidos = socketParDatos.getInputStream().read(buffer, 0, (int)Math.min(buffer.length, tamanoArchivo))) != -1){
								fileOutputStream.write(buffer,0,bytesLeidos);
							  tamanoArchivo -= bytesLeidos;
							}
							fileOutputStream.close();
						}
			    	  } catch (IOException e) {
						e.printStackTrace();
					}
			    	i++;
			    }
			}
		}
	}
	
	public LinkedList<String> obtenerArchivosCompartidosRedP2P() {
		synchronized (socketsControlParesConectados) {
			synchronized(socketsDatosParesConectados) {
				LinkedList<String> archivosCompartidosRedP2P = new LinkedList<String>();
				
				Iterator<Socket> iteradorParesConectados = socketsControlParesConectados.iterator();
			    int i = 0;
				while (iteradorParesConectados.hasNext()) {
			    	try {
			    		Socket socketParControl = iteradorParesConectados.next();
			    		Socket socketParDatos = socketsDatosParesConectados.get(i);
			    		
						DataOutputStream flujoSalidaDatosParControl = new DataOutputStream(socketParControl.getOutputStream());
						DataInputStream flujoEntradaDatosParDatos = new DataInputStream(socketParDatos.getInputStream());
						
						flujoSalidaDatosParControl.writeUTF(SOLICITUD_PAR_LISTAR_DIRECTORIO_COMPARTIDO); 
						
						
						String[] listadoDirectorioPar = flujoEntradaDatosParDatos.readUTF().split(",");
						for(String archivoPar:listadoDirectorioPar) {
							archivosCompartidosRedP2P.add(socketParControl.getInetAddress().getHostAddress().trim()+"/"+archivoPar.replaceFirst(" ",""));
						}
			    	  } catch (IOException e) {
						e.printStackTrace();
					}
			    	i++;
			    }
			    
			    return archivosCompartidosRedP2P;
			}
		}
	}
	
	private void conectarseARedP2P() {
		if(estadoConexionRedP2P == ESTADO_CONEXION_DESCONECTADO_DE_RED_P2P) {
			String ipDeBusqueda = manejadorDeLineaDeComandos.solicitarDireccionIPDeBusqueda();
			SubnetUtils utilidadesDeSubredes = new SubnetUtils(ipDeBusqueda);
			String[] hostsDeSubred = utilidadesDeSubredes.getInfo().getAllAddresses();
			for(int numeroHostEscaneado = 0;numeroHostEscaneado<hostsDeSubred.length;numeroHostEscaneado++) {
				try {
					manejadorDeLineaDeComandos.limpiarLineaDeComandos();
					manejadorDeLineaDeComandos.mostrarBuscandoPares();
					manejadorDeLineaDeComandos.mostrarHostsEscaneados(numeroHostEscaneado+1, hostsDeSubred.length);
					synchronized (socketsControlParesConectados) {
						manejadorDeLineaDeComandos.mostrarParesEncontrados(socketsControlParesConectados.size());
					}
					Socket socketParControl = new Socket ();
					socketParControl.connect(new InetSocketAddress(hostsDeSubred[numeroHostEscaneado], PUERTO_DE_COMUNICACION_CONTROL), 100);
					if(socketParControl.isConnected()) {
						Socket socketParDatos = new Socket();
						socketParDatos.connect(new InetSocketAddress(hostsDeSubred[numeroHostEscaneado], PUERTO_DE_COMUNICACION_DATOS), 100);
						if(socketParDatos.isConnected()) {
							new Thread(new Runnable() {
								@Override
								public void run() {
									atenderPar(socketParControl,socketParDatos);
								}
							}).start();
							synchronized (socketsControlParesConectados) {
								socketsControlParesConectados.add(socketParControl);
							}
							synchronized (socketsDatosParesConectados) {
								socketsDatosParesConectados.add(socketParDatos);
							}
						}
					}
				} catch (IOException e) {}
			}
			synchronized (estadoConexionRedP2P) {
				estadoConexionRedP2P = ESTADO_CONEXION_CONECTADO_A_RED_P2P;
			}
			new Thread(new Runnable() {
				@Override
				public void run() {
					esperarConexionesPares();
				}
			}).start();
		}
	}
	
	private void desconectarseDeRedP2P() {
		synchronized (estadoConexionRedP2P) {
			if(estadoConexionRedP2P == ESTADO_CONEXION_CONECTADO_A_RED_P2P) {
				estadoConexionRedP2P = ESTADO_CONEXION_DESCONECTADO_DE_RED_P2P;
				synchronized (socketsControlParesConectados) {
					Iterator<Socket> iteradorParesConectados = socketsControlParesConectados.iterator();
					while (iteradorParesConectados.hasNext()) {
				    	try {
				    		Socket socketPar = iteradorParesConectados.next();
				    		DataOutputStream flujoSalidaDatosParControl = new DataOutputStream(socketPar.getOutputStream());
							flujoSalidaDatosParControl.writeUTF(SOLICITUD_PAR_DESCONEXION); 
				    	  } catch (IOException e) {
							e.printStackTrace();
						}
				    }
				}
				try {
					socketEsperaParesControl.close();
					socketEsperaParesDatos.close();
				} catch (IOException e) {}
			}
		}	
	}
	
	public short getEstadoConexionRedP2P() {
		synchronized (estadoConexionRedP2P) {
			return estadoConexionRedP2P;
		}
	}
	
	public int getNumeroDeParesConectados() {
		synchronized (socketsControlParesConectados) {
			return socketsControlParesConectados.size();
		}
	}
}

package middleware;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Puerto de la conexi�n socket
 * @author Popbl6
 *
 */
public class PsPort {
	
	final static int MAXLENGHT = 100;
	final static int ENCABEZADOMENSAJE = 2;
	final static String SEPARADORMENSAJE = "=";
	final static int PUERTO = 0;
	final static int ID = 1;
	final static int GRUPO = 2;
	final static int LONGITUD = 3;
	final static int INTFALLO = -1;

	MulticastSocket conexion;
	int port;
	
	ArrayList<String> ipMulticast;
	ArrayList<String> datos;
	ArrayList<Integer> dataLenght;
	ArrayList<InetAddress> grupoMulticast;

	boolean exit;
		
	/**
	 * Constructor PsPort
	 * @param direccionFichero direccion del fichero de configuracion
	 */
	PsPort(String direccionFichero){
		
		dataLenght = new ArrayList<Integer>(Collections.nCopies(60, 0));
		ipMulticast = new ArrayList<String>(Collections.nCopies(60, ""));
		datos = new ArrayList<String>(Collections.nCopies(60, ""));
		grupoMulticast = new ArrayList<InetAddress>(Collections.nCopies(60, null));
		inicializarConfiguracion(direccionFichero);		
	}
	
	/**
	 * Crea la conexion
	 */
	public boolean start(){
		boolean conexionIniciada = false;
		exit = false;
		conexionIniciada = crearConexion(port);
		return conexionIniciada;
	}

	/**
	 * Cierra la conexion
	 */
	public void close(){
		exit = true;
		conexion.close();
	}

	/**
	 * Publica un dato
	 * @param idData id del dato que se publica
	 * @param data dato que se publica
	 * @param len longitud del dato que se publica
	 * @return enviado: true  no-enviado: false
	 */
	public boolean publish(int idData, byte data[], int len){
		boolean enviado;
		
		try {
			InetAddress grupoMulticast = InetAddress.getByName(ipMulticast.get(idData));
			byte mensaje[] = crearMensaje(idData, data);
			
			DatagramPacket paquete = new DatagramPacket(mensaje, (len+ENCABEZADOMENSAJE), grupoMulticast , port);
			conexion.send(paquete);
			enviado = true;

		} catch (IllegalArgumentException | IOException e) {
			enviado = false;
		}
		
		return enviado;
	}
	
	/**
	 * Recoge ultimo dato publicado
	 * @param idData id del dato que se quiere
	 * @param len longitud del dato que se quiere
	 * @return
	 */
	public String getLastSample(int idData, int len){
		//System.out.println(datos.get(idData).length());
		//if(datos.get(idData).length() == len){
			return datos.get(idData);
		//}else{
		//	return "-1";
		//}
	}
	
	/**
	 * Crear el mensaje que se va a publicar con el formato adecuado
	 * @param idData id del dato que se va a publicar
	 * @param data dato que se va a publicar
	 * @return el mensaje combinado que se va a publicar
	 */
	public byte[] crearMensaje(int idData, byte[] data) {
		byte [] mensaje;
		String id = String.valueOf(idData) + SEPARADORMENSAJE;
		mensaje = id.getBytes();

		byte[] combined = new byte[data.length + mensaje.length];

		System.arraycopy(mensaje,0,combined,0,mensaje.length);
		System.arraycopy(data,0,combined,mensaje.length,data.length);
		
		return combined;
	}

	/**
	 * Crea la conexion MulticastSocket
	 */
	public boolean crearConexion(int puertoConexion) {
		boolean conexionCreada = false;
		try {
			conexion = new MulticastSocket(puertoConexion);
			conexionCreada = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return conexionCreada;
	}
	
	/**
	 * Inicializa el sistema con la configuracion del fichero
	 * @param direccionFichero fichero de configuracion
	 */
	public void inicializarConfiguracion(String direccionFichero) {
		int id = INTFALLO;
		int cont = 0;
		int longitud = 0;
		String ip;
		String line;
	    String split[];
	    
		try (BufferedReader br = new BufferedReader(new FileReader(direccionFichero))) {
		    while ((line = br.readLine()) != null) {
		    	try{
		    		 split = line.split(SEPARADORMENSAJE);
		    		 switch(cont){
				    	case PUERTO:
				    		port = Integer.valueOf(split[1]);
				    		cont = ID;
				    		break;
				    	case ID:
				    		id = Integer.valueOf(split[1]);
				    		cont = GRUPO;
				    		break;
				    	case GRUPO:
				    		ip = split[1];
				    		ipMulticast.set(id, ip);
				    		cont = LONGITUD;
				    		break;
				    	case LONGITUD:
				    		longitud = Integer.valueOf(split[1]);
				    		dataLenght.add(id, longitud);
				    		cont = ID;
				    		break;
				    	default:
				    		break;
				    	}
		    	}catch(ArrayIndexOutOfBoundsException e){
		    		e.printStackTrace();
		    	}		    	
		    }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Escucha los datos que se estan publicando
	 */
	public void escuchar() {
		DataReader dataReader = new DataReader(this, conexion, MAXLENGHT, SEPARADORMENSAJE);
		dataReader.start();
	}
	
	/**
	 * Se suscribe a un dato
	 * @param idDato id del dato al que se quiere suscribir
	 */
	public void suscribirADato(int idDato) {
		InetAddress ip = null;
		try {
			
			ip = InetAddress.getByName(ipMulticast.get(idDato));
			grupoMulticast.set(idDato, ip);
			conexion.joinGroup(grupoMulticast.get(idDato));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Guarda el dato para posteriormente poder leerlo
	 * @param idDato id del dato que se va a guardar
	 * @param mensaje el dato que se va a guardar
	 */
	public void guardarDato(int idDato, String mensaje) {
		datos.set(idDato, mensaje);
	}
	
	
	
}


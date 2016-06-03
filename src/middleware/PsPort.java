package middleware;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

/**
 * Puerto de la conexi�n socket
 * @author Popbl6
 *
 */
public class PsPort {
	
    static final int MAXLENGHT = 500;
    static final String SEPARADORMENSAJE = "=";
    static final int INTFALLO = -1;
    private final static Logger LOGGER = Logger.getLogger(PsPort.class.getName());
    FileHandler fh;
    
    MulticastSocket conexion;
    int port, id = INTFALLO;
    String keyString;
	
    ArrayList<String> ipMulticast;
    ArrayList<String> datos;
    ArrayList<Integer> dataLenght;
    ArrayList<InetAddress> grupoMulticast;
	
    String logPath;

    boolean exit;
		
    /**
     * Constructor PsPort
     * @param direccionFichero direccion del fichero de configuracion
     */
    PsPort(String direccionFichero){
    	initiliceLogger();
        dataLenght = new ArrayList<>(Collections.nCopies(60, 0));
        ipMulticast = new ArrayList<>(Collections.nCopies(60, ""));
        datos = new ArrayList<>(Collections.nCopies(60, ""));
        grupoMulticast = new ArrayList<>(Collections.nCopies(60, null));
        inicializarConfiguracion(direccionFichero);		
    }
    
    /**
     * Inicializa el logger que creara los logs y los guardara en ficheros
     */
    private void initiliceLogger() {
    	String logFilePath = logPath+"PsPort.log";
    	try {  
            fh = new FileHandler(logFilePath);
            LOGGER.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);
        } catch (SecurityException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    }

	/**
     * Crea la conexion
     */
    public boolean start(){
        boolean conexionIniciada;
        exit = false;
        conexionIniciada = crearConexion(port);
        return conexionIniciada;
    }

    /**
     * 	 * Cierra la conexion
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
    public boolean publish(int idData, byte[] data){
        boolean enviado = false;
        byte[] mensaje;
        
        try {
            InetAddress grupoMulti = InetAddress.getByName(ipMulticast.get(idData));
            mensaje = crearMensaje(idData, data);
            byte[] mensajeEncriptado = encriptarDesencriptarMensaje(mensaje, Cipher.ENCRYPT_MODE);
            
            System.out.println(mensajeEncriptado.hashCode());
            
            DatagramPacket paquete = new DatagramPacket(mensajeEncriptado, mensajeEncriptado.length, grupoMulti , port);
            
            conexion.send(paquete);
            enviado = true;
            
        } catch (NullPointerException | IllegalArgumentException | IOException e) {
            LOGGER.info("El mensaje no se ha podido publicar");
        }
        
        return enviado;
    }
	
	

    /**
     * Encriptar o desencriptar mensaje
     * @param mensajeInicial byte[] que se quiere encriptar o desencriptar
     * @param mode 1=encriptar 2=desencriptar
     * @return mensaje encriptado o desencriptado
     */
    public byte[] encriptarDesencriptarMensaje(byte[] mensajeInicial, int mode) {
        byte [] mensajeCifradoDescifrado;
        Cipher cipher;
        SecretKey clave;
              
        clave = crearClaveCifrado(keyString);
        System.out.println("Clave: " + clave);
        cipher = inicializarCipher(mode, clave);
        System.out.println("Cipher: " + cipher);
        mensajeCifradoDescifrado = cifradorDescifradorBytes(mensajeInicial, cipher);
        
        System.out.println("Mensaje inicial: " + mensajeInicial);
        System.out.println("Mensaje cifrado: " + mensajeCifradoDescifrado);
        
        return mensajeCifradoDescifrado;
    }

    /**
     * 
     * @param mensaje byte[] que se quiere des/cifrar
     * @param cipher cifrador que se va a utilizar para des/cifrar
     * @return mensaje cifrado o descifrado
     */
    private byte[] cifradorDescifradorBytes(byte[] mensaje, Cipher cipher) {
        byte[] mensajeCifradoDescifrado = null;
        
        try {
            mensajeCifradoDescifrado = cipher.doFinal(mensaje);
        } catch (IllegalStateException | IllegalBlockSizeException | BadPaddingException e) {
            LOGGER.info("Error al cifrar el mensaje, compruebe la clave utilizada");
        }
        return mensajeCifradoDescifrado;
    }

    /**
     * Crear e inicializar Cipher
     * @param encript_mode 1=encriptar 2=desencriptar
     * @param clave SecretKey que usa el cipher para des/encriptar
     * @return cipher creado e inicializado
     */
    private Cipher inicializarCipher(int encriptMode, SecretKey clave) {
        Cipher cipher = null;
        
        try {
            cipher = Cipher.getInstance("DESede");
            cipher.init(encriptMode, clave);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
                LOGGER.info("Error al inicializar el cifrador, comprueve la clave");
            }
        
        return cipher;
    }
    
    /**
     * Crear clave cifrado
     * @param keyString string utilizado para crear la clave del cifrado
     * @return
     */
    private SecretKey crearClaveCifrado(String keyString) {
        SecretKey clave = null;
        SecretKeyFactory factory;
        
        try {
            factory = SecretKeyFactory.getInstance("DESede");
            clave = factory.generateSecret(new DESedeKeySpec(keyString.getBytes()));
            } catch (InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException e) {
                LOGGER.info("Error al crear la clave del cifrador, comprueve la clave");
            }
        
        return clave;
    }
    
    /**
     * Recoge ultimo dato publicado
     * @param idData id del dato que se quiere
     * @return
     */
    public String getLastSample(int idData){
        return datos.get(idData);
    }
    
    /**
     * Crear el mensaje que se va a publicar con el formato adecuado
     * @param idData id del dato que se va a publicar
     * @param data dato que se va a publicar
     * @return el mensaje combinado que se va a publicar
     */
    public byte[] crearMensaje(int idData, byte[] data) {
        int hash;
        String hashString;
        String combinedIdDataString;
        String combinedIdDataHashString;
        String id = String.valueOf(idData);
        
        combinedIdDataString = id + SEPARADORMENSAJE + byteArraytoString(data);
        hash = combinedIdDataString.hashCode();
        hashString = String.valueOf(hash);
        combinedIdDataHashString = combinedIdDataString +SEPARADORMENSAJE+ hashString;
        
        return combinedIdDataHashString.getBytes();
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
            LOGGER.info("No se ha podido crear la conexion, compruebe el puerto en el fichero de configuracion");
        }
        return conexionCreada;
    }
    
    /**
     * Inicializa el sistema con la configuracion del fichero
     * @param direccionFichero fichero de configuracion
     */
    public void inicializarConfiguracion(String direccionFichero) {
        String line;
        
        try (BufferedReader br = new BufferedReader(new FileReader(direccionFichero))) {
            while ((line = br.readLine()) != null) {
                inicializarVariablesFichero(line);
            }
        } catch (IOException e) {
            LOGGER.info("Error al inicializar la configuracion, compruebe que el fichero");
        }
    } 
    
    private void inicializarVariablesFichero(String line) {
        int longitud = 0;
        String ip;
        String[] split;
        
        try{
        split = line.split(SEPARADORMENSAJE);
        switch(split[0]){
	        case "puerto":
	            port = Integer.valueOf(split[1]);
	            break;
	        case "clave":
	            keyString = split[1];
	            break;
	        case "log":
	        	logPath = split[1];
	            break;
	        case "id":
	            id = Integer.valueOf(split[1]);
	            break;
	        case "grupo":
	            ip = split[1];
	            ipMulticast.set(id, ip);
	            break;
	        case "long":
	            longitud = Integer.valueOf(split[1]);
	            dataLenght.set(id, longitud);
	            break;
	        default:
	            break;
	        }
        }catch(ArrayIndexOutOfBoundsException e){
        	LOGGER.info("ATENCION: Uno o mas datos del fichero de configuracion estan mal introducidos");
        }		  
    }
    
    /**
     * Inicia el hilo que escucha los datos que se estan publicando
     */
    public void escuchar() {
        DataReader dataReader = new DataReader(this, conexion, MAXLENGHT, SEPARADORMENSAJE, logPath);
        dataReader.start();
    }
    
    /**
     * Se suscribe a un dato
     * @param idDato id del dato al que se quiere suscribir
     */
    public boolean suscribirADato(int idDato) {
        InetAddress ip = null;
        boolean adecuadamenteSuscrito = false;
        
        try {
		    ip = InetAddress.getByName(ipMulticast.get(idDato));
		    grupoMulticast.set(idDato, ip);
		    conexion.joinGroup(grupoMulticast.get(idDato));
		    adecuadamenteSuscrito = true;
        } catch (IOException e) {
		    LOGGER.info("No se ha podido suscribir al dato " +idDato+ " correctamente");
        }
        return adecuadamenteSuscrito;
    }
    
    /**
     * Guarda el dato para posteriormente poder leerlo
     * @param idDato id del dato que se va a guardar
     * @param mensaje el dato que se va a guardar
     */
    public void guardarDato(byte [] datoByte) {
        String mensajeCompletoString;
        String mensaje;
        int idDato;
        String [] arrayMensaje;
        byte[] datoDescifrado;
        int hashRecibido;
        int hashCalculado;
        String combinadoIdMensaje;
        
        datoDescifrado = encriptarDesencriptarMensaje(datoByte, Cipher.DECRYPT_MODE);
        System.out.println(byteArraytoString(datoDescifrado));
        mensajeCompletoString = byteArraytoString(datoDescifrado);
        arrayMensaje = separarString(mensajeCompletoString, SEPARADORMENSAJE);
        idDato = leerIdDato(arrayMensaje);
        mensaje = leerMensaje(arrayMensaje);
        hashRecibido = leerHashMensaje(arrayMensaje);
        combinadoIdMensaje = idDato +SEPARADORMENSAJE+ mensaje;
        hashCalculado = combinadoIdMensaje.hashCode();
        System.out.println("calculado = " + hashCalculado);
        System.out.println("recibido = " + hashRecibido);
        if(hashRecibido == hashCalculado){
            datos.set(idDato, mensaje);
        }
    }
    
    /**
     * Convierte un byteArray en String
     * @param datoByte el byteArray que se va a convertir en String
     * @return el mensaje convertido a String
     */
    private String byteArraytoString(byte [] datoByte) {
        String mensaje = null;
        
        try {
            mensaje = new String (datoByte, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.info("Error al crear el mensaje");
        }
        return mensaje;
    }
    
    /**
     * Separa el dato del array de strings
     * @param arrayMensaje el array que contiene el idDato y el dato
     * @return el dato del array
     */
    private String leerMensaje(String [] arrayMensaje) {
        return arrayMensaje[1];
    }
    
    /**
     * Separa el hash del array de strings
     * @param arrayMensaje el array que contiene el idDato, el dato y el hash
     * @return el dato del array
     */
    private int leerHashMensaje(String[] arrayMensaje) {
        return Integer.valueOf(arrayMensaje[2]);
    }
    
    /**
     * Separa el idDato del array de strings
     * @param arrayMensaje el array que contiene el idDato y el dato
     * @return el idDato del array convertido a integer
     */
    private static int leerIdDato(String [] arrayMensaje) {
        return Integer.valueOf(arrayMensaje[0]);
    }
    
    /**
     * Separa el un String en un array de String diviendolo con el separador
     * @param dato el String que se quiere dividir
     * @param separadormensaje el caracter que va a dividir las partes del String
     * @return String [] con los Strings separados
     */
    private String[] separarString(String dato, String separadormensaje) {
        String [] mensaje;
        
        mensaje = dato.split(separadormensaje);
        
        return mensaje;
    }
}
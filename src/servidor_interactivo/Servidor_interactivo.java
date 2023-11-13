/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidor_interactivo;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Federico Cirett Galán
 */
public class Servidor_interactivo {

    /**
     * @param args the command line arguments
     */
   
    /**
     * @param args the command line arguments
     */
    private static DataOutputStream dout = null;
    private static DataInputStream din = null;
    public static void main(String[] args) throws IOException {
        try {
            //
            String folder = ".";
            String ext    = "jpg";
            // Abrimos conexion
            ServerSocket servidor = new ServerSocket(4242);
            Socket socket = servidor.accept();
            DataInputStream dinMain = new DataInputStream(socket.getInputStream());
            DataOutputStream doutMain=new DataOutputStream(socket.getOutputStream());
            //BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
            String enviar=""; String recibido="";
            System.out.println("Servidor acepta conexiones");
            List<File> lista_archivos;
            while(!recibido.equals("salir")) {
                recibido = dinMain.readUTF();
                System.out.println("Cliente dice:"+recibido);
                enviar = "Mensaje '"+recibido+"' recibido";
                if (recibido.equals("enviar")) {
                    System.out.println("Petición de envío de archivo");
                    escucharyRecibirArchivo();
                }
                String[] mensaje = recibido.split(",");
                System.out.println("mensaje 0:"+mensaje[0]);
                if (mensaje[0].equals("listar")) {
                    folder = mensaje[1];
                    ext    = mensaje[2];
                    System.out.println("listando archivos");
                    lista_archivos = 
                            obtenListaArchivos(folder,ext);
                    System.out.println("Enviando lista");
                    enviar = obtenCadenaDesdeLista(lista_archivos);
                }
                // enviamos archivo al cliente
                if (mensaje[0].equals("solicitar")) {
                    lista_archivos = 
                            obtenListaArchivos(folder,ext);
                    int idx = Integer.parseInt(mensaje[1]);
                    String ipCliente = "localhost";
                    enviarArchivoACliente(
                            lista_archivos.get(idx).toString(),
                            ipCliente );
                }
                doutMain.writeUTF(enviar); doutMain.flush();
            }
            dinMain.close();doutMain.close(); socket.close();servidor.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
    }
    public static void enviarArchivoACliente(String archivo,
                                             String ipCliente) {
        try (Socket socket = new Socket(ipCliente, 900)) {
            dout = new DataOutputStream(socket.getOutputStream());
            System.out.println("Enviando archivo a servidor");
            if (enviarArchivo(archivo)) {
                System.out.println("Archivo enviado con exito");
            } else {
                System.out.println("Falla en envío de archivo");
            }
            dout.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    public static String obtenCadenaDesdeLista(List<File> lista) {
        String cadena = "";
        int i= 0;
        for (File archivo : lista) {
            cadena += i + ":"+archivo.toString()+"\n";  
            i++;
        }
        return cadena;
    }
    public static List<File> obtenListaArchivos(String ruta,
                String ext) {
        List<File> lista_archivos = new ArrayList<>();
        File directorio = new File(ruta);
        File[] lista = directorio.listFiles();
        for (File archivo : lista) {
            String nombre_archivo = archivo.toString();
            int indice = nombre_archivo.lastIndexOf(".");
            if (indice > 0) {
                String extension = nombre_archivo.substring(indice+1);
                if(extension.equals(ext)) {
                    lista_archivos.add(archivo);
                }
            }
        }
        return lista_archivos;
    }
    private static void escucharyRecibirArchivo() {
        int puerto = 800;
        try ( ServerSocket server = new ServerSocket(puerto)) {
            System.out.println("Iniciamos Server en puerto"+puerto);
            Socket socket = server.accept();
            din = new DataInputStream(socket.getInputStream());
            dout= new DataOutputStream(socket.getOutputStream());
            if (recibeArchivo()) {
                System.out.println("Archivo recibido");
            }
            din.close();dout.close();socket.close();server.close();
            System.out.println("Fin");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    private static boolean recibeArchivo() {
        boolean exito = false;
        try {
            int bytes = 0;
            String fileName = din.readUTF();
            FileOutputStream fos = new FileOutputStream(fileName);
            long size = din.readLong();
            byte[] buffer = new byte[4*1024];
            while ( size > 0 && ( bytes = din.read(buffer,0,
                   (int)Math.min(buffer.length, size))) !=-1) 
            {
                fos.write(buffer, 0, bytes);
                size = size - bytes;
            }
            fos.close();
            exito = true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return exito;
    }
    private static boolean enviarArchivo(String fileName) {
        boolean exito = false;
        try {
            int bytes = 0;
            File file = new File(fileName);
            FileInputStream fis = new FileInputStream(file);
            dout.writeUTF(fileName);
            dout.writeLong(file.length()); // enviamos longitud 
            byte[] buffer = new byte[4 * 1024];
            while ((bytes = fis.read(buffer)) != -1) {
                dout.write(buffer,0, bytes); //enviamos en partes
                dout.flush();
            }
            fis.close(); 
            exito = true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return exito;
    }
}

package Negocio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.stereotype.Component;

import Datos.PathImplDAO;
import Entities.TaSicWSectEntities;

@Component
public class FlujoEfectivo {

	private static final Logger log = Logger.getLogger(FlujoEfectivo.class.getName());
	private Properties prop = new Properties();

	@Autowired
	private PathImplDAO pathImplDAO;

	public void procCopiadoCtoC() {
		try {
			distribucionTipo2();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void procDisper() {
		try {
			distribucionTipo2();
			String resourceName = "h2h.properties";
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			InputStream stream = loader.getResourceAsStream(resourceName);
			prop.load(stream);
			Comandos.execCommand(prop.getProperty("comDesencrip"));
			// log.info("Desencriptando......");
			// Thread.sleep(10000);
			distribucion();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void distribucion() throws Exception {
		String query = null;
		try {
			ArrayList<File> archivos = new ArrayList<File>();
			log.info("Entre a Distribucion Tipo1");
			// Consulta la configuracion para los archivos
			String resourceName = "h2h.properties";
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			InputStream stream = loader.getResourceAsStream(resourceName);
			prop.load(stream);
			query = prop.getProperty("querydDist");
			log.info(query);
			List<TaSicWSectEntities> paths = pathImplDAO.getPath(query);
			log.info(paths.size());
			for (TaSicWSectEntities path : paths) {
				log.info("ruta origen: " + path.getRutaOrigen());
				log.info("id Row: " + path.getIdRuta());
				log.info("----------------------------");
				// File carpeta = new File(rutas.getString(2));
				archivos = traerArchivos(path.getRutaOrigen());
				log.info(archivos.size());
				if (!archivos.isEmpty() && archivos.size() > 0)
					moverArchivos(path.getRutaOrigen(), path.getRutaDestino(), archivos, path.getIdBanco(),
							path.getExtensionArchivo(), path);
				log.info("----------------------------");
			}
		} catch (Exception e) {
			log.error("error", e);
			e.printStackTrace();
		}
	}

	public void distribucionTipo2() {
		String query = null;
		try {
			ArrayList<File> archivos = new ArrayList<File>();
			log.info("Entre a Distribucion Tipo2");
			// Consulta la configuracion para los archivos
			String resourceName = "h2h.properties";
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			InputStream stream = loader.getResourceAsStream(resourceName);
			prop.load(stream);
			query = prop.getProperty("queriCTC");
			List<TaSicWSectEntities> paths = pathImplDAO.getPath(query);
			log.info(paths.size());
			for (TaSicWSectEntities path : paths) {
				log.info(path.getRutaOrigen());
				log.info("id Row: " + path.getIdRuta());
				log.info("----------------------------");
				archivos = traerArchivos(path.getRutaOrigen());
				log.info(archivos.size());
				if (!archivos.isEmpty() && archivos.size() > 0)
					moverArchivosT2(path.getRutaOrigen(), path.getRutaDestino(), archivos, path.getExtensionArchivo(),
							path);
				log.info("----------------------------");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void moverArchivosT2(String origen, String destino, ArrayList<File> archivos, String extencion,
			TaSicWSectEntities path) throws IOException {
		log.info("Origenorigen " + origen);
		// crea los apuntadores de los archivos
		String antesDas = "", despuesDas = "";
		int punto = extencion.indexOf(".");
		antesDas = extencion.substring(8, punto);
		despuesDas = extencion.substring(punto, extencion.length() - 1);
		if (antesDas.equals("*"))
			antesDas = "";
		if (despuesDas.equals(".*"))
			despuesDas = "";

		for (File archivo : archivos) {
			log.info("RutaOrigen : " + archivo.getAbsolutePath());
			log.info("RutaDestino : " + destino + File.pathSeparator + archivo.getName());
			log.info("EsCarperta: " + archivo.isDirectory());
			if (archivo.isDirectory()) {
				log.info("++++++++++Entre a Recurcion++++++++++++++");
				ArrayList<File> archivosInt = traerArchivos(archivo.getAbsolutePath());
				log.info("Origen Carpeta; " + origen);
				moverArchivosT2(origen + File.pathSeparator + archivo.getName() + File.pathSeparator, destino,
						archivosInt, extencion, path);
				log.info("++++++++++Sali de Recurcion++++++++++++++");
			} else {
				if (!despuesDas.equals("") && !antesDas.equals("")) {
					if (archivo.getName().indexOf(despuesDas) >= 0 && archivo.getName().indexOf(antesDas) >= 0) {
						log.info("encontre 3 coincidencias");
						copiar(origen, destino, archivo, path);
					}
				}
				if (despuesDas.equals("")) {
					log.info("ArchivoOriginal: antes " + archivo.getName());
					String nomArch = archivo.getName().substring(0, antesDas.length());

					log.info("entre a if de antes de punto");
					log.info("archivo: " + nomArch);
					log.info("antesDas: " + antesDas);

					if (nomArch.equals(antesDas)) {
						log.info("entre a if para crear archivo; ---");
						copiar(origen, destino, archivo, path);
					}
				}
				if (antesDas.equals("")) {
					log.info("archivoOriginal: " + archivo.getAbsolutePath());
					String nomArch = archivo.getName().substring(archivo.getName().length() - despuesDas.length(),
							archivo.getName().length());
					log.info("entre a if de despues de punto");
					log.info("archivo: " + nomArch);
					log.info("DespuesDas: " + despuesDas);

					if (nomArch.equals(despuesDas)) {
						log.info("entre a if para crear archivo");
						copiar(origen, destino, archivo, path);
					}
				}
			}
		}
	}

	private void moverCarpetaToCarpeta(String origen, String destino, TaSicWSectEntities path) {
		try {
			log.info("URL" + origen);
			ArrayList<File> archivos = traerArchivos(origen);
			log.info("Numero de Archivos" + archivos.size());
			for (File archivo : archivos) {
				copiar(origen, destino, archivo, path);
				log.info("");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void moverArchivos(String origen, String destino, ArrayList<File> archivos, String chequera,
			String extencion, TaSicWSectEntities path) throws IOException {

		log.info("Origenorigen " + origen);
		// crea los apuntadores de los archivos
		String antesDas = "", despuesDas = "";
		int punto = extencion.indexOf(".");
		antesDas = extencion.substring(8, punto);
		despuesDas = extencion.substring(punto, extencion.length() - 1);
		if (antesDas.equals("*"))
			antesDas = "";
		if (despuesDas.equals(".*"))
			despuesDas = "";

		log.info("antes: " + antesDas + "\n" + "despues: " + despuesDas);
		for (File archivo : archivos) {
			log.info("RutaOrigen : " + archivo.getPath());
			log.info("EsCarperta: " + archivo.isDirectory());
			if (archivo.isDirectory()) {
				log.info("++++++++++Entre a Recurcion++++++++++++++");
				ArrayList<File> archivosInt = traerArchivos(archivo.getAbsolutePath());
				log.info("Origen Carpeta; " + origen);
				moverArchivos(archivo.getAbsolutePath() + File.pathSeparator, destino, archivosInt, chequera, extencion,
						path);
				log.info("++++++++++Sali de Recurcion++++++++++++++");
			} else {
				if (!despuesDas.equals("") && !antesDas.equals("")) {
					if (archivo.getName().indexOf(despuesDas) >= 0 && archivo.getName().indexOf(antesDas) >= 0
							&& (archivo.getName().indexOf(chequera)) >= 0) {
						log.info("encontre 3 coincidencias");
						copiar(origen, destino, archivo, path);
					}
				}
				if (despuesDas.equals("")) {
					log.info("ArchivoOriginal: antes " + archivo.getName());
					String nomArch = archivo.getName().substring(0, antesDas.length());
					int coinciden = archivo.getName().indexOf(chequera);
					log.info("encontrado:  " + coinciden);
					log.info("entre a if de antes de punto");
					log.info("archivo: " + nomArch);
					log.info("antesDas: " + antesDas);
					log.info("chequera: " + chequera);
					if (nomArch.equals(antesDas) && (archivo.getName().indexOf(chequera)) >= 0) {
						log.info("entre a if para crear archivo; ---");
						copiar(origen, destino, archivo, path);
					}
				}
				if (antesDas.equals("")) {
					log.info("archivoOriginal: " + archivo.getName());
					String nomArch = archivo.getName().substring(archivo.getName().length() - despuesDas.length(),
							archivo.getName().length());
					log.info("entre a if de despues de punto");
					log.info("archivo: " + nomArch);
					log.info("DespuesDas: " + despuesDas);
					log.info("chequera: " + chequera);
					if (nomArch.equals(despuesDas) && (archivo.getName().indexOf(chequera)) >= 0) {
						log.info("entre a if para crear archivo");
						copiar(origen, destino, archivo, path);
					}
				}
			}
		}
		log.info("\n");
	}

	private void copiar(String origen, String destino, File archivo, TaSicWSectEntities path)
			throws IOException {
		log.info("++++++++++++++++++++++++++\n\n\n");
		log.info("origen: " + archivo.getAbsolutePath());
		log.info("destino " + destino);

		if (!path.getIpDestino().equals("10.60.20.35")) {
			final GenericXmlApplicationContext context = new GenericXmlApplicationContext();
			context.getEnvironment().getSystemProperties().put("host", path.getIpDestino());
			context.getEnvironment().getSystemProperties().put("shareAndDir", path.getRutaDestino());
			context.getEnvironment().getSystemProperties().put("username", path.getUsuarioDestino());
			context.getEnvironment().getSystemProperties().put("password", path.getPasswordDestino());
			context.getEnvironment().getSystemProperties().put("dominio", path.getDominioDestino());

			context.load("classpath:spring/xml/spring-integration-context.xml");
			context.registerShutdownHook();
			context.refresh();
			context.registerShutdownHook();
			SmbSessionFactory smbSessionFactory = context.getBean("smbSession", SmbSessionFactory.class);
			log.info("Polling from Share: " + smbSessionFactory.getUrl());
			smbSessionFactory.getSession().isOpen();
			smbSessionFactory.getSession().append(archivo.toURI().toURL().openStream(), path.getRutaDestino());

		} else {
			Path origenArchivo = Paths.get(archivo.getAbsolutePath());
			Path destinoArchivo = Paths.get(destino + File.pathSeparator + archivo.getName());
			Files.copy(origenArchivo, destinoArchivo, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private ArrayList<File> traerArchivos(String url) {
		log.info("urlDe la carpeta: " + url);
		File directoriOrigen = new File(url);
		// obtiene el nombre de cada archivo que esta contenido en la carpeta
		ArrayList<File> listFile = (ArrayList<File>) Arrays.asList(directoriOrigen.listFiles());
		return listFile;
	}
}
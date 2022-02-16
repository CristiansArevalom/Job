package com.test.crucewindows.model;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.test.crucewindows.exceptions.InventoryException;

public class CruceWindows {

	private static String windowsFilePath = "";
	private static String ucmdbFilePath = "";

	public static boolean checkIpAddressOK(String text) {
		String regexCode = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
				+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
		Pattern pattern = Pattern.compile(regexCode);
		Matcher matcher = pattern.matcher(text);
		return (matcher.find() ? true : false);
	}
	public static boolean checkIpAddressWithError(String text) {
		String regexCode = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
				+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\|";
		Pattern pattern = Pattern.compile(regexCode);
		Matcher matcher = pattern.matcher(text);
		return (matcher.find() ? true : false);
	}
	public static String checkAndFormatIpAddress(String text) {
		String ipAdress ="";
		if(checkIpAddressOK(text)) {
			ipAdress = text;
		}else if(checkIpAddressWithError(text)) {
			ipAdress=text.replace("|", "");
		}else {
			throw new InventoryException("La Ip "+text+" no es valida");
		}

		return ipAdress;
	}

	public static boolean checkServCodeWithError(String text) {
		String regexCode = ("(([A-Z]{3}[0-9]{4}|[A-Z]{5}[0-9]{2}|[A-Z]{4}[0-9]{3})|([A-Z]{4}[0-9]{4}|[A-Z]{5}[0-9]{3}))"
				+ "(\\_\\37\\w.*)|((\\ _ \\w*|\\ _\\w*|\\_ \\w*)|(\\ - \\w*|\\- \\w*|\\-\\w*)|\\ -\\w*)"); // codserv _hostname
		Pattern pattern = Pattern.compile(regexCode);
		Matcher matcher = pattern.matcher(text);
		return (matcher.find() ? true : false);
	}

	public static boolean checkServCodeAndHostnameOK(String text) {
		String regexCodeHostname = ("^(([A-Z]{3}[0-9]{4}|[A-Z]{5}[0-9]{2}|[A-Z]{4}[0-9]{3})|([A-Z]{4}[0-9]{4}|[A-Z]{5}[0-9]{3}))(\\_\\w.*)");// codserv_hostname
		Pattern pattern = Pattern.compile(regexCodeHostname);
		Matcher matcher = pattern.matcher(text);
		return (matcher.find() ? true : false);
	}
	//Mira si el codigoy hostname cumple con estandar de guionbajo, si no, en los casos que se pueda lo ajusta y retorna
	public static String checkAndFormatServCodeAndHostname(String text){
		String ServCodeAndHostnameOK="";
		if(checkServCodeAndHostnameOK(text)) { // si cumple con codigo de servicio y nombre, retorna el texto
			ServCodeAndHostnameOK = text;
		}else if(checkServCodeWithError(text)) {// si entrra, es que tiene algun guin bajo
			ServCodeAndHostnameOK =  text.replace("\37", "")  
					.replaceFirst(" _ ", "_")
					.replaceFirst(" _", "_")
					.replaceFirst("_ ", "_")
					.replaceFirst(" - ", "_")
					.replaceFirst("- ", "_")
					.replaceFirst(" -", "_")
					.replaceFirst("-", "_");
		}else {
			throw new InventoryException("El valor "+text+" No cumple con estandar codserv_hostame");
		}
		//ARROJJAR ERROR, NO CUMPLE ESTANDAR
		return ServCodeAndHostnameOK;
	}



	public static void main(String[] args) {
		windowsFilePath = "C:\\Users\\I5-9600K\\Downloads\\Cruce Windows DC 2022.xlsx";
		ucmdbFilePath = "C:\\Users\\I5-9600K\\Downloads\\ucmdb2018.xlsx";
		HashMap<String, CiWindows> inventoryUcmdb = readUcmdbInventory(ucmdbFilePath);
		readWindowsInventary(windowsFilePath,inventoryUcmdb);
	}
	public static String getStringCellValue(Cell cell) {
		String cellValue ="";
		switch (cell.getCellType()) {
		case STRING:
			cellValue = cell.getStringCellValue();
			break;
		case NUMERIC:
			cellValue = cell.getNumericCellValue() + "";
			break;
		case BLANK:
			cellValue = "*";
			break;
		case BOOLEAN:
			cellValue = cell.getBooleanCellValue() + "";
		default:
			break;
		}
		return cellValue;
	}

	public static HashMap<String, CiWindows> readUcmdbInventory(String ucmdbFilePath) {
		HashMap<String, CiWindows> mapCodHostIp = new HashMap<>();
		try {// ver si realmente al invocar el metodo crear el archivo. porque lo creria y
			// cerraria 1k veces
			FileInputStream file = new FileInputStream(new File(ucmdbFilePath));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet ucmdbSheet = workbook.getSheetAt(0);
			Iterator<Row> rowUcmdbIterator = ucmdbSheet.iterator();
			ArrayList<String> header = new ArrayList<String>();
			while (rowUcmdbIterator.hasNext()) { // recorre filas
				CiWindows ciwindows = new CiWindows();
				Row rowUcmdb = rowUcmdbIterator.next();
				String displayLabel = "", serviceCode = "", ipManagement = "", ipWmi = ""; // VER MEJOR FORMA DE
				// GUARDAR. PUEDE
				// SER MATRIZ DE
				// [3X3]
				short cellCount = rowUcmdb.getLastCellNum();
				for (short currentCell = 0; currentCell < cellCount; currentCell++) { // recorre las columbas del inv
					String cellValue = "";
					cellValue = getStringCellValue(rowUcmdb.getCell(currentCell));
					if (rowUcmdb.getRowNum() == 0) { // Guarda el encabezado
						header.add(cellValue);
						//System.out.print(cellValue+"|");
					} else {
						switch (header.get(currentCell)) { // Busca en el encabezado los titulos y guarda el valor a la
						// columna que se necesita.
						case "[Windows] : Display Label":
							ciwindows.setDisplayLabel(cellValue);
							displayLabel = cellValue;
							break;
						case "[Windows] : Onyx ServiceCodes":
							ciwindows.setOnyxServiceCodes(cellValue);
							serviceCode = cellValue;
							break;
						case "[Windows] : Ip Gestion":
							ciwindows.setIpGestion(cellValue);
							ipManagement = cellValue;
							break;
						case "[Windows] : IpAddress":
							ciwindows.setIpAddress(cellValue);
							ipWmi = cellValue;
							break;
						}
					}
				}
				if(ipManagement.isEmpty()){
					if((!ipWmi.isEmpty() && checkIpAddressOK(ipWmi))) {
						ipManagement = ipWmi;						
					}else {
						ipManagement = "Sin ip gestión valida en UCMDB para hacer cruce";
					}
				}
				//System.out.print(""); ///añadir el servicecode_idplsaylabel y como valor el objeto de windows.
				//
				mapCodHostIp.put(serviceCode + "_" + displayLabel.toUpperCase(), ciwindows);
				//System.out.println(serviceCode + "_" + displayLabel + "|" + ipManagement + "|" + ipWmi);
				/*
				 * buscar en que columna esta el campo "[Windows] : Display Label",[Windows] :
				 * Service Code,[Windows] : IP Gestión,[Windows] : Ip WMI y retornar un key map
				 * con servicecode_dislay label, ip gestión
				 */
			}
			/*
			System.out.println("");
			for (Entry<String, CiWindows> elem : mapCodHostIp.entrySet()) {
				System.out.println(elem.getKey() + "|" + elem.getValue().getIpGestion());
			}
			*/
			System.out.println("CANTIDAD REGISTROS EN UCMDB 2020 "+mapCodHostIp.size());

			file.close();
			workbook.close();

		} catch (Exception ex) {
			System.out.println(ex);
		}
		return mapCodHostIp;
	}

	public static void readWindowsInventary(String windowsFilePath, HashMap<String, CiWindows> inventoryUcmdb ) {

		try {
			FileInputStream fileWindows = new FileInputStream(new File(windowsFilePath));
			XSSFWorkbook workbookWindows = new XSSFWorkbook(fileWindows);
			XSSFSheet windowsSheet = workbookWindows.getSheetAt(0);
			Iterator<Row> rowIterator = windowsSheet.iterator();
			ArrayList<String> header = new ArrayList<String>();
			String cellWindowsValue = "", windowsServHost = "", windowsIP = "",windowsClient="", statusMatch="";
			short cellCount=0;
			while (rowIterator.hasNext()) { // recorre filas de archvowindows
				Row row = rowIterator.next();
				Iterator<Cell> cellWindowsIterator = row.cellIterator();
				boolean checkserv = false, checkIP = false;
				cellCount = row.getLastCellNum();
				for (short currentCell = 0; currentCell < cellCount; currentCell++) { // recorre las columbas del inv					
					cellWindowsValue = getStringCellValue(cellWindowsIterator.next());
					if (row.getRowNum() == 0) {
						header.add(cellWindowsValue);
						//System.out.print(cellWindowsValue + "|"); // si la fila es cero, imprime el totulo
					}else {
						switch (header.get(currentCell)) { // Busca en el encabezado los titulos y guarda el valor a la	columna que se necesita.
						case "COD_HOSTNAME":
							windowsServHost = checkAndFormatServCodeAndHostname(cellWindowsValue);
							//PDT ELUNIT SEPRATOR, LO ETECTA EN LA REGEX PERO AL REEMPLAZAR NO LO DETECTA
							break;
						case "IP_GESTION":
							windowsIP = checkAndFormatIpAddress(cellWindowsValue);
							break;
						case "PUERTO":
							break;
						case "CLIENTE":
							windowsClient=cellWindowsValue;
							break;
						}
					}
				}

				CiWindows ciWindows = inventaryContainsCodHostAndIP(windowsServHost.toUpperCase(),windowsIP,inventoryUcmdb);
				if (ciWindows != null) {
					statusMatch = "OK en UCMDB"+"|"+ciWindows.getOnyxServiceCodes()+"_"+ciWindows.getDisplayLabel()+"|"+ciWindows.getIpGestion();					
				}else {
					statusMatch = "PDT";
				}
				/*
				//valida si el registro actual esta dentro del hash del inventario de UCMDB
	System.out.print(windowsServHost+"|"+windowsIP+"|");

				if(inventaryContainsCodHostAndIP(windowsServHost,windowsIP,inventoryUcmdb)) {
					statusMatch = "OK en UCMDB";
					//en caso de que coincida se debe marquillar como ok en ucmdb y traer los datos de hostname e ip del inv ucmdb

				}else if (inventaryContainsIP(windowsServHost,windowsIP,inventoryUcmdb)) {
					statusMatch = "Coincide ip en inv ucmdb 2020";
				}
				 */
				System.out.println(windowsServHost+"|"+windowsIP+"|"+statusMatch+"|");
			}		
			fileWindows.close();
			workbookWindows.close();
		} catch (Exception ex) {
			System.err.println(ex);
		}
		// trabajar con StringBuilder.
		// como no se tiene excel, toca a punta de txt o csv
	}
	public static CiWindows inventaryContainsCodHostAndIP(String windowsServHost,String windowsIP, HashMap<String, CiWindows> inventoryUCMDB) {		
		CiWindows ciWindows = null;
		if (inventoryUCMDB.size()>0) {
			if((inventoryUCMDB.containsKey(windowsServHost))
					&& (inventoryUCMDB.get(windowsServHost).getIpGestion().equals(windowsIP)
							|| inventoryUCMDB.get(windowsServHost).getIpGestion().contains(windowsIP)
					|| (inventoryUCMDB.get(windowsServHost).getIpAddress().contains(windowsIP)))) {
				ciWindows = inventoryUCMDB.get(windowsServHost);	
			}else {
				//COINCIDE HOSTNAME PERO LA IP DE GESTIÓN NO, POR ESO NO HACE MATCH
			}
		}else if (inventoryUCMDB.containsValue(windowsIP)) { //DEBE BUSCAR LA WINDOWS IP EN TODO EL HASH DE LAS 
			inventoryUCMDB.get(windowsServHost);
		}
		return ciWindows;
	}




	public static boolean inventaryContainsCodHost (String windowsServHost,String windowsIP, HashMap<String, String> inventoryUCMDB) {
		boolean inventaryContainsCodHostAndIP = false;
		if(inventoryUCMDB.size()>0 && (inventoryUCMDB.containsKey(windowsServHost))) {

		}
		return inventaryContainsCodHostAndIP;
	}
	/*
	public static boolean inventaryContainsIP (String windowsServHost,String windowsIP, HashMap<String, CiWindows> inventoryUCMDB) {
		boolean inventaryContainsCodHostAndIP = false;
		if(inventoryUCMDB.size()>0 && (inventoryUCMDB.containsValue(CiWindows))) {
			inventoryUCMDB.get(windowsServHost);
			inventaryContainsCodHostAndIP =  true;
		}
		return inventaryContainsCodHostAndIP;
	}	*/


}

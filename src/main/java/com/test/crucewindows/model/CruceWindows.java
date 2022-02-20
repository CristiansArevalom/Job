package com.test.crucewindows.model;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
//import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
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


	public static void main(String[] args) {
		try {
			windowsFilePath = "C:\\Users\\I5-9600K\\Downloads\\inv torre.xlsx";
			ucmdbFilePath = "C:\\Users\\I5-9600K\\Downloads\\inv ucmdb.xlsx";

			Scanner sc = new Scanner (System.in);
			byte option=1;
			System.out.println("1 = Cruce de inventario torre vs UCMDB");
			System.out.println("2 = Cruce de UCMDB vs Inventario torre");
			System.out.println("0 = Salir");

			while (sc.hasNext() && (option>0 && option<3)) {
				option =Byte.parseByte(sc.nextLine());

				if(option == 1) {
					inventaryWindowsContainsCodHostAndIP(readWindowsInventary(windowsFilePath), readUcmdbInventory(ucmdbFilePath));
					//PDT validar duplicados de inv ucmdb y torre
					
				}else if (option ==2) {
					//PDT CRUCE DE ucmdb a inv torre.
				}

			}
		}catch (Exception ex) {
			System.out.println("Error" + ex);
		}
	}


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

	public static ArrayList<CiUcmdbWindows> readUcmdbInventory(String ucmdbFilePath) {
		ArrayList<CiUcmdbWindows> ucmdbWindowsInventary = new ArrayList<>();

		try {// ver si realmente al invocar el metodo crear el archivo. porque lo creria y
			// cerraria 1k veces
			FileInputStream file = new FileInputStream(new File(ucmdbFilePath));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet ucmdbSheet = workbook.getSheetAt(0);
			Iterator<Row> rowUcmdbIterator = ucmdbSheet.iterator();
			ArrayList<String> header = new ArrayList<String>();


			while (rowUcmdbIterator.hasNext()) { // recorre filas
				CiUcmdbWindows ciUcmdbWindows = new CiUcmdbWindows();
				Row rowUcmdb = rowUcmdbIterator.next();
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
							ciUcmdbWindows.setDisplayLabel(cellValue);
							break;
						case "[Windows] : Onyx ServiceCodes":
							ciUcmdbWindows.setOnyxServiceCodes(cellValue);
							break;
						case "[Windows] : Ip Gestion":
							ciUcmdbWindows.setIpGestion(cellValue);
							break;
						case "[Windows] : IpAddress":
							ciUcmdbWindows.setIpAddress(cellValue);
							break;
						}
					}
				}

				if(ciUcmdbWindows.getDisplayLabel()!=null){
					if(ciUcmdbWindows.getIpGestion().isEmpty()){
						if((!ciUcmdbWindows.getIpAddress().isEmpty() && checkIpAddressOK(ciUcmdbWindows.getIpAddress()))) {
							//ciUcmdbWindows.setIpGestion(ciUcmdbWindows.getIpAddress());					
						}else {
							//ciUcmdbWindows.setIpGestion("Sin ip gestión valida en UCMDB para hacer cruce");
						}
					}
					if(ciUcmdbWindows.getDisplayLabel()!=null){
						ucmdbWindowsInventary.add(ciUcmdbWindows);
					}
				}
			}
			System.out.println("CANTIDAD REGISTROS EN UCMDB 2020 "+ucmdbWindowsInventary.size());

			file.close();
			workbook.close();

		} catch (Exception ex) {
			System.out.println(ex);
		}
		return ucmdbWindowsInventary;
	}

	public static ArrayList<CiWindows> readWindowsInventary(String windowsFilePath) {
		//

		ArrayList<CiWindows> windowsInventary= new ArrayList<>();
		try {
			FileInputStream fileWindows = new FileInputStream(new File(windowsFilePath));
			XSSFWorkbook workbookWindows = new XSSFWorkbook(fileWindows);
			XSSFSheet windowsSheet = workbookWindows.getSheetAt(0);
			Iterator<Row> rowIterator = windowsSheet.iterator();
			ArrayList<String> header = new ArrayList<String>();
			String cellWindowsValue = "";
			//String cellWindowsValue = "", windowsServHost = "", windowsIP = "",windowsClient="", statusMatch="";
			short cellCount=0;			
			while (rowIterator.hasNext()) { // recorre filas de archvowindows
				Row row = rowIterator.next();
				Iterator<Cell> cellWindowsIterator = row.cellIterator();

				CiWindows ciWindows = new CiWindows();

				cellCount = row.getLastCellNum();
				for (short currentCell = 0; currentCell < cellCount; currentCell++) { // recorre las columbas del inv					
					cellWindowsValue = getStringCellValue(cellWindowsIterator.next());
					if (row.getRowNum() == 0) {
						header.add(cellWindowsValue);
						//System.out.print(cellWindowsValue + "|"); // si la fila es cero, imprime el totulo
					}else {

						switch (header.get(currentCell)) { // Busca en el encabezado los titulos y guarda el valor a la	columna que se necesita.
						case "COD_HOSTNAME":
							ciWindows.setCodHostname(checkAndFormatServCodeAndHostname(cellWindowsValue));
							break;
						case "IP_GESTION":
							ciWindows.setIpGestion(checkAndFormatIpAddress(cellWindowsValue));
							break;
						case "PUERTO":
							ciWindows.setPuerto(cellWindowsValue);
							break;
						case "CLIENTE":
							ciWindows.setCliente(cellWindowsValue);
							break;
						default :
							break;
						}
					}
				}
				if(ciWindows.getCodHostname()!=null){
					windowsInventary.add(ciWindows);
				}
				/*
				if (compararInventarios) {
					CiUcmdbWindows ciWindows = inventaryContainsCodHostAndIP(windowsServHost.toUpperCase(),windowsIP,inventoryUcmdb);
					if (ciWindows != null) {
						statusMatch = "OK en UCMDB"+"|"+ciWindows.getOnyxServiceCodes()+"_"+ciWindows.getDisplayLabel()+"|"+ciWindows.getIpGestion();					
					}else {
						statusMatch = "PDT";
					}
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
				//System.out.println(windowsServHost+"|"+windowsIP+"|"+statusMatch+"|");
			}
			/*
			System.out.println("");
			for (Entry<String, CiWindows> elem : windowsInventary.entrySet()) {
				System.out.println(elem.getKey() + "|" + elem.getValue().getIpGestion());
			}

			 */
			System.out.println("CANTIDAD REGISTROS EN INVENTARIO TORRE "+windowsInventary.size());

			fileWindows.close();
			workbookWindows.close();
		} catch (Exception ex) {
			System.err.println("ERROR: "+ex);
		}
		return windowsInventary;
	}

	//PROBLEMA CON EL COINTAINS DE IP, TRAE 172.27.218.11 A 172.27.218.1 ; validar como validar con el ultimo substring
	public static CiUcmdbWindows inventaryWindowsContainsCodHostAndIP(ArrayList<CiWindows> windowsInventary, ArrayList<CiUcmdbWindows> inventoryUCMDB) {		
		if(windowsInventary.size()==0 && inventoryUCMDB.size()==0) {
			throw new InventoryException("El inventario se encuentra vacio"); 
		}else {
			HashMap<String, CiUcmdbWindows> inventoryUCMDBhm = new HashMap<>();
			for (CiUcmdbWindows ciUcmdbWindows : inventoryUCMDB) {
				inventoryUCMDBhm.put(ciUcmdbWindows.getOnyxServiceCodes()+"_"+ciUcmdbWindows.getDisplayLabel().toUpperCase(), ciUcmdbWindows);
			}

			for ( int i = 0 ; i<windowsInventary.size();i++) {
				CiWindows ciWindows = windowsInventary.get(i);

				if(inventoryUCMDBhm.containsKey(ciWindows.getCodHostname().toUpperCase())){
					if(inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getIpGestion().equals(ciWindows.getIpGestion())
							|| (inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getIpGestion().contains(ciWindows.getIpGestion()+","))
							|| ((inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getIpGestion().endsWith(ciWindows.getIpGestion())))
							|| (inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getIpAddress().contains(ciWindows.getIpGestion()+","))
							|| (inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getIpAddress().endsWith(ciWindows.getIpGestion()))
							
							){

						System.out.println(i+"|"+ciWindows.getCodHostname()+
								"|"+ciWindows.getIpGestion()+"|"+inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getDisplayLabel()+
								"|"+inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getOnyxServiceCodes()+
								"|"+inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getIpGestion()+
								"|"+inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getIpAddress()+
								"|OK");

					}else {
						System.out.println(i+"|"+ciWindows.getCodHostname()+
								"|"+ciWindows.getIpGestion()+
								"|"+inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getDisplayLabel()+
								"|"+inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getOnyxServiceCodes()+
								"|"+inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getIpGestion()+
								"|"+inventoryUCMDBhm.get(ciWindows.getCodHostname().toUpperCase()).getIpAddress()+
								"|Validar, No coincide ip de gestion dada por inv torre");
					}

				}
				else {
					boolean match=false;
					//busca por ip
					for (CiUcmdbWindows ciUcmdbWindows : inventoryUCMDB) {
						String errorMatch ="";
						if (ciUcmdbWindows.getIpGestion().equals(ciWindows.getIpGestion())
								|| ciUcmdbWindows.getIpGestion().contains(ciWindows.getIpGestion()+",")
								|| ciUcmdbWindows.getIpGestion().endsWith(ciWindows.getIpGestion())
								|| ciUcmdbWindows.getIpAddress().contains(ciWindows.getIpGestion()+",")
								|| ciUcmdbWindows.getIpAddress().endsWith(ciWindows.getIpGestion())
								){
							//1 obtener codigo de servicio , primer _ que encuentre antes del codhostname
							//2 obtener displayLabel, es lo que este despues de _ 
							ciWindows.getCodHostname().indexOf('_');
							String ciWindowsServCode =  ciWindows.getCodHostname().substring(0, ciWindows.getCodHostname().indexOf('_'));
							String ciWindowsDispLabel=  ciWindows.getCodHostname().substring(ciWindows.getCodHostname().indexOf('_'),ciWindows.getCodHostname().length());
							if((!ciUcmdbWindows.getDisplayLabel().equalsIgnoreCase(ciWindowsDispLabel)) && (ciUcmdbWindows.getOnyxServiceCodes().equalsIgnoreCase(ciWindowsServCode))) {
								errorMatch = "y cod de servicio pero no coincide displayLabeL ";
							}else if ((ciUcmdbWindows.getDisplayLabel().equalsIgnoreCase(ciWindowsDispLabel)) && (!ciUcmdbWindows.getOnyxServiceCodes().equalsIgnoreCase(ciWindowsServCode))) {
								errorMatch = "y displayLabeL pero no coincide codigo de servicio ";
							}else {
								errorMatch = " y no coincide codigo de servicio ni displayLabel";
							}
							System.out.println(i+"|"+ciWindows.getCodHostname()+
									"|"+ciWindows.getIpGestion()+
									"|"+ciUcmdbWindows.getDisplayLabel()+
									"|"+ciUcmdbWindows.getOnyxServiceCodes()+
									"|"+ciUcmdbWindows.getIpGestion()+
									"|"+ciUcmdbWindows.getIpAddress()+
									"|Validar, Coincide Ip de gestion "+errorMatch
									);
							match=true;
							break;
						}
					}
					if(!match) {
						System.out.println(i+"|"+ciWindows.getCodHostname()+
								"|"+ciWindows.getIpGestion()+"|||||PDT");
					}
					
				}
			}

			/*
			for (Entry<String, CiWindows> ciWindows : windowsInventary.entrySet()) {
				//System.out.println(elem.getKey() + "|" + elem.getValue().getIpGestion());
				if (inventoryUCMDB.size()>0) {
					if((inventoryUCMDB.containsKey(ciWindows.getKey()))
							&& (inventoryUCMDB.get(ciWindows.getKey()).getIpGestion().equals(ciWindows.getValue().getIpGestion())
									|| inventoryUCMDB.get(ciWindows.getKey()).getIpGestion().contains(ciWindows.getValue().getIpGestion())
									|| (inventoryUCMDB.get(ciWindows.getKey()).getIpAddress().contains(ciWindows.getValue().getIpGestion())))) {
						//ciWindows = inventoryUCMDB.get(ciWindows.getKey(.getOnyxServiceCodes()+"_"+inventoryUCMDB.get(ciWindows.getKey()).getDisplayLabel()
								+"|"+inventoryUCMDB.get(ciWindows.getKey()).getIpGestion()+"|"+inventoryUCMDB.get(ciWindows.getKey()).getIpAddress() +"|OK");
					}else {));
						System.out.println(ciWindows.getKey()+"|"+ciWindows.getValue().getIpGestion()+
								"|"+inventoryUCMDB.get(ciWindows.getKey())
						System.out.println(ciWindows.getKey()+"|"+ciWindows.getValue().getIpGestion()+"|PDT");
						//COINCIDE HOSTNAME PERO LA IP DE GESTIÓN NO, POR ESO NO HACE MATCH
					}

				}else if (inventoryUCMDB.containsValue(windowsIP)) { //DEBE BUSCAR LA WINDOWS IP EN TODO EL HASH y traer el hostname asociado 
					inventoryUCMDB.get(windowsServHost);
				}
				}
			}*/
		}
		return null;
	}



}

package com.test.crucewindows.model;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.test.crucewindows.exceptions.InventoryException;

public class CruceWindows {

	public static boolean checkIpAddress(String text) {
		String regexCode = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
				+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
		Pattern pattern = Pattern.compile(regexCode);
		Matcher matcher = pattern.matcher(text);
		return (matcher.find() ? true : false);
	}

	public static boolean checkServCode(String text) {
		String regexCode = ("(([A-Z]{3}[0-9]{4}|[A-Z]{5}[0-9]{2}|[A-Z]{4}[0-9]{3})|([A-Z]{4}[0-9]{4}|[A-Z]{5}[0-9]{3}))"
				+ "(\\_\\37\\w.*)|((\\ _ \\w*|\\ _\\w*|\\_ \\w*)|(\\ - \\w*|\\- \\w*|\\-\\w*)|\\ -\\w*)"); // codserv _hostname
		Pattern pattern = Pattern.compile(regexCode);
		Matcher matcher = pattern.matcher(text);
		return (matcher.find() ? true : false);
	}

	public static boolean checkServCodeAndHostnameOK(String text) {
		String regexCodeHostname = ("(([A-Z]{3}[0-9]{4}|[A-Z]{5}[0-9]{2}|[A-Z]{4}[0-9]{3})|([A-Z]{4}[0-9]{4}|[A-Z]{5}[0-9]{3}))(\\_\\w.*)");// codserv_hostname
		Pattern pattern = Pattern.compile(regexCodeHostname);
		Matcher matcher = pattern.matcher(text);
		return (matcher.find() ? true : false);
	}
	//Mira si el codigoy hostname cumple con estandar de guionbajo, si no, en los casos que se pueda lo ajusta y retorna
	public static String checkAndFormatServCodeAndHostname(String text){
		String ServCodeAndHostnameOK="";
			if(checkServCodeAndHostnameOK(text)) { // si cumple con codigo de servicio y nombre, retorna el texto
				ServCodeAndHostnameOK = text;
			}else if(checkServCode(text)) {// si entrra, es que tiene algun guin bajo
				ServCodeAndHostnameOK =  text.replace("\\u241F", "\\0")
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
			String windowsFilePath = "C:\\Users\\I5-9600K\\Downloads\\Cruce Windows DC 2022.xlsx";
			String ucmdbFilePath = "C:\\Users\\I5-9600K\\Downloads\\ucmdb2018.xlsx";
			readWindowsInventary(windowsFilePath);
			// readUcmdbInventory(ucmdbFilePath, "prueba");
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

		public static HashMap<String, String> readUcmdbInventory(String ucmdbFilePath, String codeHostname) {
			try {// ver si realmente al invocar el metodo crear el archivo. porque lo creria y
				// cerraria 1k veces
				FileInputStream file = new FileInputStream(new File(ucmdbFilePath));
				XSSFWorkbook workbook = new XSSFWorkbook(file);
				XSSFSheet ucmdbSheet = workbook.getSheetAt(0);
				Iterator<Row> rowUcmdbIterator = ucmdbSheet.iterator();
				ArrayList<String> header = new ArrayList<String>();

				while (rowUcmdbIterator.hasNext()) { // recorre filas
					Row rowUcmdb = rowUcmdbIterator.next();
					String displayLabel = null, serviceCode = null, ipManagement = null, ipWmi = null; // VER MEJOR FORMA DE
					// GUARDAR. PUEDE
					// SER MATRIZ DE
					// [3X3]
					// Iterator<Cell> cellIterator = row.cellIterator();
					short cellCount = rowUcmdb.getLastCellNum();
					for (short currentCell = 0; currentCell < cellCount; currentCell++) { // recorre las columbas del inv
						String cellValue = "";
						cellValue = getStringCellValue(rowUcmdb.getCell(currentCell));
						if (rowUcmdb.getRowNum() == 0) { // Guarda el encabezado
							header.add(cellValue);
							// System.out.print(cellValue+"|");
						} else {
							switch (header.get(currentCell)) { // Busca en el encabezado los titulos y guarda el valor a la
							// columna que se necesita.
							case "[Windows] : Display Label":
								displayLabel = cellValue;
								break;
							case "[Windows] : Service Code":
								serviceCode = cellValue;
								break;
							case "[Windows] : IP Gestión":
								ipManagement = cellValue;
								break;
							case "[Windows] : Ip WMI":
								ipWmi = cellValue;
								break;
							}
						}
					}
					System.out.println(serviceCode + "_" + displayLabel + "|" + ipManagement + "|" + ipWmi);
					/*
					 * buscar en que columna esta el campo "[Windows] : Display Label",[Windows] :
					 * Service Code,[Windows] : IP Gestión,[Windows] : Ip WMI y retornar un key map
					 * con servicecode_dislay label, ip gestión
					 */
				}
				file.close();
				workbook.close();

			} catch (Exception ex) {
				System.out.println(ex);
			}
			return null;
		}

		public static void readWindowsInventary(String windowsFilePath) {
			try {
				FileInputStream fileWindows = new FileInputStream(new File(windowsFilePath));
				XSSFWorkbook workbookWindows = new XSSFWorkbook(fileWindows);
				XSSFSheet windowsSheet = workbookWindows.getSheetAt(0);
				Iterator<Row> rowIterator = windowsSheet.iterator();
				ArrayList<String> header = new ArrayList<String>();
				HashMap<String, String> mapCodHostIp = new HashMap<>();

				while (rowIterator.hasNext()) { // recorre filas de archvowindows
					Row row = rowIterator.next();
					Iterator<Cell> cellWindowsIterator = row.cellIterator();
					String cellWindowsValue = "", windowsServHost = "", windowsIP = "";

					boolean checkserv = false, checkIP = false;
					
					short cellCount = row.getLastCellNum();
					for (short currentCell = 0; currentCell < cellCount; currentCell++) { // recorre las columbas del inv					
						cellWindowsValue = getStringCellValue(cellWindowsIterator.next());
						if (row.getRowNum() == 0) {
							header.add(cellWindowsValue);
							System.out.print(cellWindowsValue + "|"); // si la fila es cero, imprime el totulo
						}
						
						else {
							switch (header.get(currentCell)) { // Busca en el encabezado los titulos y guarda el valor a la	columna que se necesita.
							case "COD_HOSTNAME":
								windowsServHost = checkAndFormatServCodeAndHostname(cellWindowsValue);//.replace('\u241F','\0');
								break;
							case "IP_GESTION":
								//windowsIP = checkIpAddress(cellWindowsValue);
								break;
							default:
								throw new InventoryException("La columna "+header.get(currentCell)+"No esta contemplada");
							}
						}
						/*	
						else if(header.get(0).equals("COD_HOSTNAME")) {
							checkAndFormatServCodeAndHostname(cellWindowsValue);
							
							// SE VALIDA SI CUMPLE CON CODIGO DE SERVIICO Y SI ES ASI, SE AÑADA AL MAP
							

												

						if (checkServCodeAndHostnameOK(cellWindowsValue)) {
							// AQUI COLOCAR PROCESO DE BUSCAR EL CELWINDOWSVALUE EN INVENTARIO UCMDB
							/*PDT SOLUCUONAR QUE EL VERIFICADOR DE SINTAXIS NO CONTROLA ESTOS 3
							AGD0216_PIPES-AGD0216	172.18.249.105 ?
				//llee el tx, si no esta mal lo retorna normal, si esta mal lo ajusta y lo retorna. 

							windowsServHost = cellWindowsValue;
							checkserv = true;

						} else if (checkServCode(cellWindowsValue)) {// checar si cumple con codigo de servico.
							windowsServHost = cellWindowsValue.replaceFirst(" _ ", "_").replaceFirst(" _", "_")
									.replaceFirst("_ ", "_").replaceFirst(" - ", "_").replaceFirst("- ", "_")
									.replaceFirst(" -", "_").replaceFirst("-", "_");
							checkserv = true;

						} else if (checkIpAddress(cellWindowsValue)) {
							windowsIP = cellWindowsValue;
							checkIP = true;

							// System.out.print(windowsServHost+"|"+windowsIP+"|");//ya se guardaron las
							// variables widowsservhost y windowsIP
						}*/

						}


					if (checkserv && checkIP) { // si luego de validar, la sintaxis de codhostname e ip son correctas, se
						// agregan al hashmap
						System.out.println("");
						System.out.print(windowsServHost + "|" + windowsIP);
						// mapCodHostIp.put(windowsServHost, windowsIP);
					}

					// AQUI DEBO HACER LA VALIDACIÓN DE SI EL WINDOWS SERVER HOST E IP ACTUAL ESTA
					// EN EL INV DE UCMDB
					// System.out.print(windowsServHost+"|"+windowsIP+"|");
				}
				System.out.println("");
				for (Map.Entry<String, String> elem : mapCodHostIp.entrySet()) {
					System.out.println(elem.getKey() + "|" + elem.getValue());
				}
				System.out.println(mapCodHostIp.size());
				System.out.println(mapCodHostIp); 

				fileWindows.close();
				workbookWindows.close();

			} catch (Exception ex) {
				System.err.println(ex);
			}

			// trabajar con StringBuilder.
			// como no se tiene excel, toca a punta de txt o csv
		}

		/// leer excelm

	}

package com.nexiilabs.autolifecycle.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.nexiilabs.autolifecycle.releases.ReleaseListModel;
import com.nexiilabs.autolifecycle.releases.ReleaseService;

@RestController
@CrossOrigin(origins = "*", allowCredentials = "true", allowedHeaders = "*")
@RequestMapping("/excel")
public class ReleaseExcelSheet {
	@Autowired
	ReleaseService releaseService;

	@RequestMapping(value = "/download", method = RequestMethod.GET)
	public ResponseEntity<?> exportExcelForReleases(HttpServletRequest request, HttpServletResponse response) {
		List<ReleaseListModel> releaseModel = null;
		OutputStream fos = null;
		XSSFWorkbook workbook = null;
		File file = null;
		try {
			releaseModel = releaseService.getAllReleases();
			String fileName = "/home/nexii/Releases.xlsx";
			file = new File(fileName);
			workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet();
			List<String> fieldNames = getFieldNamesForClass(releaseModel.get(0).getClass());
			int rowCount = 0;
			int columnCount = 0;
			XSSFCellStyle my_style = workbook.createCellStyle();
			XSSFFont my_font = workbook.createFont();
			my_font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
			my_style.setFont(my_font);
			Row row = sheet.createRow(rowCount++);
			for (String fieldName : fieldNames) {
				Cell cell = row.createCell(columnCount++);
				cell.setCellValue(fieldName);
				cell.setCellStyle(my_style);
			}
			Class<? extends Object> classz = releaseModel.get(0).getClass();
			for (ReleaseListModel t : releaseModel) {
				row = sheet.createRow(rowCount++);
				columnCount = 0;
				for (String fieldName : fieldNames) {
					Cell cell = row.createCell(columnCount);
					Method method = null;
					try {
						method = classz.getMethod("get" + capitalize(fieldName));
					} catch (NoSuchMethodException nme) {
						method = classz.getMethod("get" + fieldName);
					}
					Object value = method.invoke(t, (Object[]) null);
					if (value != null) {
						if (value instanceof String) {
							cell.setCellValue((String) value);
						} else if (value instanceof Long) {
							cell.setCellValue((Long) value);
						} else if (value instanceof Integer) {
							cell.setCellValue((Integer) value);
						} else if (value instanceof Double) {
							cell.setCellValue((Double) value);
						}
					}
					columnCount++;
				}
			}
			fos = new FileOutputStream(file);
			workbook.write(fos);
			fos.flush();
			if (file.exists()) {
				InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
				String mediaType = Files.probeContentType(file.toPath());
				System.out.println("mediaType: " + mediaType);
				return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
						.contentType(MediaType.parseMediaType(mediaType)).contentLength(file.length()).body(resource);
			} else {
				return new ResponseEntity<String>("File not found to download", HttpStatus.OK);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (fos != null) {
					fos.close();
					if (workbook != null) {
						workbook.close();
					}
				}
			} catch (IOException e) {
			}
		}

		return new ResponseEntity<String>("", HttpStatus.ACCEPTED);

	}

	// retrieve field names from a POJO class
	private static List<String> getFieldNamesForClass(Class<?> clazz) throws Exception {
		List<String> fieldNames = new ArrayList<String>();
		Field[] fields = clazz.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
				fieldNames.add(fields[i].getName());
				if(fields[i].getName().equals("files")){
					fieldNames.remove(fields[i].getName());
				}
		}
		return fieldNames;
	}

	// capitalize the first letter of the field name for retriving value of the
	// field later
	private static String capitalize(String s) {
		if (s.length() == 0)
			return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
}

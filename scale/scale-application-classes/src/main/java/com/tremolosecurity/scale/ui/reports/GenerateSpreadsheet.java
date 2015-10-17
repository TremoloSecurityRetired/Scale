/*******************************************************************************
 * Copyright 2015 Tremolo Security, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.tremolosecurity.scale.ui.reports;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.tremolosecurity.provisioning.service.util.ReportGrouping;

public class GenerateSpreadsheet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		resp.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
		resp.setHeader("Pragma", "no-cache");
		
		
		ReportViewer scaleReport = (ReportViewer) req.getSession().getAttribute("scaleReportCached");
		
		Workbook wb = new XSSFWorkbook();
		
		Font font = wb.createFont();
		font.setBold(true);
		
		Font titleFont = wb.createFont();
		titleFont.setBold(true);
		titleFont.setFontHeightInPoints((short) 16);
		
		Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(scaleReport.getReportInfo().getName()));
		
		//Create a header
		Row row = sheet.createRow(0);
		Cell cell = row.createCell(0);
		
		RichTextString title = new XSSFRichTextString(scaleReport.getReportInfo().getName());
		title.applyFont(titleFont);
		
		sheet.addMergedRegion(new CellRangeAddress(0,0,0,3));
		
		
		cell.setCellValue(title);
		
		row = sheet.createRow(1);
		cell = row.createCell(0);
		cell.setCellValue(scaleReport.getReportInfo().getDescription());
		
		sheet.addMergedRegion(new CellRangeAddress(1,1,0,3));
		
		row = sheet.createRow(2);
		cell = row.createCell(0);
		cell.setCellValue(scaleReport.getRunDateTime());
		
		sheet.addMergedRegion(new CellRangeAddress(2,2,0,3));
		
		row = sheet.createRow(3);
		
		int rowNum = 4;
		
		if (scaleReport.getResults().getGrouping().isEmpty()) {
			row = sheet.createRow(rowNum);
			cell = row.createCell(0);
			cell.setCellValue("There is no data for this report");
		} else {
			
			for (ReportGrouping group : scaleReport.getResults().getGrouping()) {
				for (String colHeader : scaleReport.getResults().getHeaderFields()) {
					row = sheet.createRow(rowNum);
					cell = row.createCell(0);
					
					RichTextString rcolHeader = new XSSFRichTextString(colHeader);
					rcolHeader.applyFont(font);
					
					cell.setCellValue(rcolHeader);
					cell = row.createCell(1);
					cell.setCellValue(group.getHeader().get(colHeader));
					
					rowNum++;
				}
				
				row = sheet.createRow(rowNum);
				
				int cellNum = 0;
				for (String colHeader : scaleReport.getResults().getDataFields()) {
					cell = row.createCell(cellNum);
					
					RichTextString rcolHeader = new XSSFRichTextString(colHeader);
					rcolHeader.applyFont(font);
					cell.setCellValue(rcolHeader);
					cellNum++;
				}
				
				rowNum++;
				
				for (Map<String,String> dataRow : group.getData()) {
					cellNum = 0;
					row = sheet.createRow(rowNum);
					for (String colHeader : scaleReport.getResults().getDataFields()) {
						cell = row.createCell(cellNum);
						cell.setCellValue(dataRow.get(colHeader));
						cellNum++;
					}
					rowNum++;
				}
				
				row = sheet.createRow(rowNum);
				rowNum++;
			}
			
		}
		
		resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		wb.write(resp.getOutputStream());
		
		
	}
	
}

package top.jach.tes.app.jhkt.chenjiali;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
/**
 * God Component异味计算
 */
public class GodComponentMain {
    public static void main(String[] args) throws IOException {
        final String inputFilename = "D:\\data\\CP\\MSdata.xls";
        final String outputFilename = "D:\\data\\CP\\OutMSdata.xls";
        Workbook wb = new HSSFWorkbook(new FileInputStream(inputFilename));
        Sheet sheet = wb.getSheetAt(0);
        for(int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            double first = row.getCell(1).getNumericCellValue();
            double second = row.getCell(2).getNumericCellValue();
            row.createCell(3).setCellValue((first > 30.0 || second > 27000) ? 1.0 : 0.0);
        }
        Cell cell = sheet.getRow(0).createCell(3);
        cell.setCellValue("god_component");
        wb.write(new FileOutputStream(outputFilename));
    }
}

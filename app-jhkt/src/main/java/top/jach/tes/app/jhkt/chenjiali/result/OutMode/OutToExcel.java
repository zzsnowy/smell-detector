package top.jach.tes.app.jhkt.chenjiali.result.OutMode;

import lombok.var;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import top.jach.tes.app.jhkt.chenjiali.result.OutputDataStruct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author:AdminChen
 * @date:2020/10/13
 * @description:
 */

public class OutToExcel {
    String output_path;
    List<OutputDataStruct> alldata;

    public void WriteToExcel() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        for(OutputDataStruct data:alldata){
            XSSFSheet sheet=workbook.createSheet(data.getVersion());
            int rownum=0;
            for(String rowdata0:data.getComponent_smellcount().keySet()){
                XSSFRow row=sheet.createRow(rownum++);
                XSSFCell cell0=row.createCell(0);
                cell0.setCellValue(rowdata0);
                XSSFCell cell1=row.createCell(1);
                cell1.setCellValue(data.getComponent_smellcount().get(rowdata0));
            }
        }
        // 将工作表写入文件
        try {
            File file = new File(output_path);
            if (!file.exists())
                file.createNewFile();
            OutputStream stream = new FileOutputStream(file);
            workbook.write(stream);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getOutput_path() {
        return output_path;
    }

    public void setOutput_path(String output_path) {
        this.output_path = output_path;
    }

    public List<OutputDataStruct> getAlldata() {
        return alldata;
    }

    public void setAlldata(ArrayList<OutputDataStruct> alldata) {
        this.alldata = alldata;
    }
}

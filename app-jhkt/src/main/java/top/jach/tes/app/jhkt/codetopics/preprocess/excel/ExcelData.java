package top.jach.tes.app.jhkt.codetopics.preprocess.excel;

import com.alibaba.excel.annotation.ExcelProperty;

public class ExcelData {
    @ExcelProperty(index = 0)
    private String path;

    public ExcelData() {}

    public ExcelData(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

package top.jach.tes.app.jhkt.codetopic.preprocess.excel;

import com.alibaba.excel.annotation.ExcelProperty;

public class ServiceData {
    @ExcelProperty(index = 0)
    private String name;
    @ExcelProperty(index = 1)
    private String word;
    @ExcelProperty(index = 2)
    private String path;

    public ServiceData() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

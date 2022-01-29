package top.jach.tes.app.jhkt.codetopics.preprocess.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import java.util.ArrayList;
import java.util.List;

public class ExcelListener extends AnalysisEventListener<ExcelData> {

    private List<String> pathList = new ArrayList<>();

    public List<String> getPathList() {
        return pathList;
    }

    @Override
    public void invoke(ExcelData excelData, AnalysisContext analysisContext) {
        pathList.add(excelData.getPath());
        System.out.println(excelData.getPath());
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        System.out.println("Excel解析完成，共读取到" + pathList.size() + "条路径数据");
    }
}

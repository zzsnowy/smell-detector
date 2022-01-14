package top.jach.tes.app.jhkt.chenjiali;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import top.jach.tes.app.jhkt.chenjiali.result.ResultAll;
import top.jach.tes.app.jhkt.chenjiali.result.ResultForAllMs;
import top.jach.tes.app.jhkt.lijiaqi.result.Result;
import top.jach.tes.app.jhkt.lijiaqi.result.ResultForMs;
import top.jach.tes.app.mock.Environment;
import top.jach.tes.core.api.domain.action.Action;
import top.jach.tes.app.mock.InfoTool;
import top.jach.tes.app.mock.InputInfoProfiles;
import top.jach.tes.app.mock.TaskTool;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.api.domain.info.InfoProfile;
import top.jach.tes.core.api.exception.ActionExecuteFailedException;
import top.jach.tes.core.impl.domain.element.Element;
import top.jach.tes.core.impl.domain.element.ElementsValue;
import top.jach.tes.core.impl.domain.relation.PairRelation;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.app.dev.DevApp;
import top.jach.tes.core.impl.domain.relation.Relation;
import top.jach.tes.plugin.jhkt.DataAction;
import top.jach.tes.plugin.jhkt.InfoNameConstant;
import top.jach.tes.plugin.jhkt.analysis.MicroserviceAttrsInfo;
import top.jach.tes.plugin.jhkt.arcan.cyclic.CyclicArcanAction;
import top.jach.tes.plugin.jhkt.arcan.hublink.HublinkArcanAction;
import top.jach.tes.plugin.jhkt.arcan.ud.UdArcanAction;
import top.jach.tes.plugin.jhkt.arcsmell.ArcSmellAction;
import top.jach.tes.plugin.jhkt.arcsmell.cyclic.CyclicAction;
import top.jach.tes.plugin.jhkt.arcsmell.hublink.HublinkAction;
import top.jach.tes.plugin.jhkt.arcsmell.ud.UdAction;
import top.jach.tes.plugin.jhkt.microservice.Microservice;
import top.jach.tes.plugin.jhkt.microservice.MicroservicesInfo;
import top.jach.tes.plugin.tes.code.git.version.Version;
import top.jach.tes.plugin.tes.code.git.version.VersionsInfo;
import top.jach.tes.plugin.tes.code.repo.ReposInfo;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * 修改后的依赖相关的AS的计算，包括hublike,cyclic,unstable三个异味及relations结果。
 * 直接从json文件里读取出每个版本所包含的所有依赖关系及每条依赖关系的权重
 * 每条依赖关系对应多条调用关系，每条调用关系对应一对request和response，每条依赖关系包含的调用关系的数量就是依赖关系的权重
 */
public class DpendencyRelatedMain extends DevApp {
    public static void main(String[] args) throws ActionExecuteFailedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {

        Context context = Environment.contextFactory.createContext(Environment.defaultProject);
        //注意excel读取，17版的excel及以后的要用XSSFWorkbook，17版以前的用HSSFWorkbook。versionRelations是HSSFWorkbook的
        List<String> microserviceNames = new ArrayList<>();
        Map<String,List<PairRelation>> vRelations=readRelations("D:\\data\\dop.xls",microserviceNames);//读取5个版本带权重的依赖
        String vmm = "x_3c9_x_95d.x_893.x_893.x_e09d_";
        List<PairRelation> rels=new ArrayList<>();
        HashSet set = new HashSet(microserviceNames);
        microserviceNames.clear();
        microserviceNames.addAll(set);
        rels.addAll(vRelations.get(vmm));
        MicroservicesInfo microservices = new MicroservicesInfo();
        for(int i=0;i<microserviceNames.size();i++)
        {
            microservices.addMicroservice(new Microservice().setElementName(microserviceNames.get(i)));
        }
        PairRelationsInfo pairRelationsInfoWithWeight = microservices.callRelationsInfoByTopicWithJsonNew(true,rels);
        pairRelationsInfoWithWeight.setName(InfoNameConstant.MicroserviceExcludeSomeCallRelation);
        InfoTool.saveInputInfos(pairRelationsInfoWithWeight);
        //就算不算权重，但不能直接deweight到把一旦存在重复的就删除的地步，至少1,2,3存在重复，只保留1这一条是可以的，不deweight
        PairRelationsInfo pairRelationsInfoWithoutWeight = microservices.callRelationsInfoByTopicWithJsonNew(false,rels);
        pairRelationsInfoWithoutWeight.setName(InfoNameConstant.MicroserviceExcludeSomeCallRelation);
        InfoTool.saveInputInfos(pairRelationsInfoWithoutWeight);
        ElementsValue hublike_no_weight = HublinkAction.calculateHublike(pairRelationsInfoWithoutWeight);
        ElementsValue hublike_weight = HublinkAction.calculateHublike(pairRelationsInfoWithWeight);
        ElementsValue hublike_weight_in=HublinkAction.calculateHublikeIn(pairRelationsInfoWithWeight);
        ElementsValue hublike_weight_out=HublinkAction.calculateHublikeOut(pairRelationsInfoWithWeight);
        ElementsValue hublike_no_weight_in=HublinkAction.calculateHublikeIn(pairRelationsInfoWithoutWeight);
        ElementsValue hublike_no_weight_out=HublinkAction.calculateHublikeOut(pairRelationsInfoWithoutWeight);
        ElementsValue hub_weight = new ElementsValue();
        ElementsValue hub_no_weight = new ElementsValue();
        ElementsValue cyclicResult = CyclicAction.CalculateCyclic(context, microservices, pairRelationsInfoWithWeight);
        ElementsValue unstable_no_weight= UdAction.calculateUdNew(microservices,pairRelationsInfoWithoutWeight);
        ElementsValue unstable_weight= UdAction.calculateUdNew(microservices,pairRelationsInfoWithWeight);
        //Arcan
        ElementsValue cyclicArcanResult = CyclicArcanAction.CalculateCyclic(context, microservices, pairRelationsInfoWithWeight);
        ElementsValue unstable_no_weight_arcan = UdArcanAction.calculateUdNew(microservices,pairRelationsInfoWithoutWeight);
        ElementsValue unstable_weight_arcan = UdArcanAction.calculateUdNew(microservices,pairRelationsInfoWithWeight);
        ElementsValue hub_weight_arcan = HublinkArcanAction.calculateHub(microservices, hublike_weight_in, hublike_weight_out, hublike_weight);
        ElementsValue hub_no_weight_arcan = HublinkArcanAction.calculateHub(microservices, hublike_no_weight_in, hublike_no_weight_out, hublike_no_weight);

        ResultForAllMs resultForMs = new ResultForAllMs();
        ResultAll result=new ResultAll();
        result.put(vmm, resultForMs);//zx
        resultForMs.setMicroservice(microserviceNames);
        resultForMs.setHublike_weight(hublike_weight.getValueMap());
        resultForMs.setHublike_weight_in(hublike_weight_in.getValueMap());
        resultForMs.setHublike_weight_out(hublike_weight_out.getValueMap());
        resultForMs.setHublike_no_weight(hublike_no_weight.getValueMap());
        resultForMs.setHublike_no_weight_in(hublike_no_weight_in.getValueMap());
        resultForMs.setHublike_no_weight_out(hublike_no_weight_out.getValueMap());
        resultForMs.setHub_no_weight(hub_weight.getValueMap());
        resultForMs.setHub_weight(hub_no_weight.getValueMap());
        resultForMs.setCyclic(cyclicResult.getValueMap());
        resultForMs.setUnstable_weight(unstable_weight.getValueMap());
        resultForMs.setUnstable_no_weight(unstable_no_weight.getValue());
        resultForMs.setRelationWeight(Lists.newArrayList(pairRelationsInfoWithWeight.getRelations().iterator()));
        resultForMs.setRelationNoWeight(Lists.newArrayList(pairRelationsInfoWithoutWeight.getRelations().iterator()));
        //Arcan输出
        ResultAll resultArcan=new ResultAll();
        ResultForAllMs resultForArcanMs = new ResultForAllMs();
        resultArcan.put("arcan",resultForArcanMs);
        resultForArcanMs.setMicroservice(microserviceNames);
        resultForArcanMs.setHublike_weight(hublike_weight.getValueMap());
        resultForArcanMs.setHublike_weight_in(hublike_weight_in.getValueMap());
        resultForArcanMs.setHublike_weight_out(hublike_weight_out.getValueMap());
        resultForArcanMs.setHublike_no_weight(hublike_no_weight.getValueMap());
        resultForArcanMs.setHublike_no_weight_in(hublike_no_weight_in.getValueMap());
        resultForArcanMs.setHublike_no_weight_out(hublike_no_weight_out.getValueMap());
        resultForArcanMs.setHub_no_weight(hub_weight_arcan.getValueMap());
        resultForArcanMs.setHub_weight(hub_no_weight_arcan.getValueMap());
        resultForArcanMs.setCyclic(cyclicArcanResult.getValueMap());
        resultForArcanMs.setUnstable_weight(unstable_weight_arcan.getValueMap());
        resultForArcanMs.setUnstable_no_weight(unstable_no_weight_arcan.getValue());
        resultForArcanMs.setRelationWeight(Lists.newArrayList(pairRelationsInfoWithWeight.getRelations().iterator()));
        resultForArcanMs.setRelationNoWeight(Lists.newArrayList(pairRelationsInfoWithoutWeight.getRelations().iterator()));

        exportExcel(result,"D:\\data\\versions5\\nearstData\\hubcyclicunstableOutput.xls");
        exportExcel(resultArcan,"D:\\data\\versions5\\nearstData\\ArcanOutput.xls");

    }

    public static Map<String,List<PairRelation>> readRelations(String path, List<String> microserviceNames) throws IOException {
        Map<String,List<PairRelation>> res=new HashMap<>();
        File file=new File(path);
        InputStream is=new FileInputStream(file.getAbsoluteFile());
        HSSFWorkbook workbook=new HSSFWorkbook(is);
        // zx for(int i=0;i<5;i++)
        {//5个版本，对应5个sheet页
            List<PairRelation> rels=new ArrayList<>();
            // HSSFSheet sheet=workbook.getSheetAt(i);
            HSSFSheet sheet=workbook.getSheetAt(0);
            String vname=sheet.getSheetName();
            int rows=sheet.getLastRowNum();
            for(int j=1;j<=rows;j++){
                Row row=sheet.getRow(j);
                PairRelation pr=new PairRelation(row.getCell(0).getStringCellValue(),row.getCell(1).getStringCellValue());
                pr.setValue(row.getCell(2).getNumericCellValue());
                    microserviceNames.add(row.getCell(0).getStringCellValue());
                    microserviceNames.add(row.getCell(1).getStringCellValue());
                rels.add(pr);
            }
            res.put(vname,rels);
        }
        return res;
    }

    public static void exportExcel(ResultAll results,String path) throws IOException {
        Workbook workbook=new HSSFWorkbook();
        for(String version:results.getResults().keySet()){
            ResultForAllMs resultForAllMs=results.getResults().get(version);
            List<String> microserviceList=resultForAllMs.getMicroservice();
            Sheet sheet=workbook.createSheet(version);//一个版本一个sheet
            Row row=sheet.createRow(0);
            Cell cell1=row.createCell(1);
            cell1.setCellValue("hublike_weight");
            Cell cell2=row.createCell(2);
            cell2.setCellValue("hublike_weight_in");
            Cell cell3=row.createCell(3);
            cell3.setCellValue("hublike_weight_out");
            Cell cell4=row.createCell(4);
            cell4.setCellValue("hublike_no_weight");
            Cell cell5=row.createCell(5);
            cell5.setCellValue("hublike_no_weight_in");
            Cell cell6=row.createCell(6);
            cell6.setCellValue("hublike_no_weight_out");
            Cell cell7=row.createCell(7);
            cell7.setCellValue("hub_weight");
            Cell cell8=row.createCell(8);
            cell8.setCellValue("hub_no_weight");
            Cell cell9=row.createCell(9);
            cell9.setCellValue("cyclic");
            Cell cell10=row.createCell(10);
            cell10.setCellValue("ud_weight");
            Cell cell11=row.createCell(11);
            cell11.setCellValue("ud_no_weight");
            int rowCount=1;
            for(String ms:microserviceList){
                Row r=sheet.createRow(rowCount);
                r.createCell(0).setCellValue(ms);
                Double hbw=resultForAllMs.getHublike_weight().get(ms);
                r.createCell(1).setCellValue(hbw==null?0.0:hbw);
                Double hbwi=resultForAllMs.getHublike_weight_in().get(ms);
                r.createCell(2).setCellValue(hbwi==null?0.0:hbwi);
                Double hbwo=resultForAllMs.getHublike_weight_out().get(ms);
                r.createCell(3).setCellValue(hbwo==null?0.0:hbwo);
                Double hbnw=resultForAllMs.getHublike_no_weight().get(ms);
                r.createCell(4).setCellValue(hbnw==null?0.0:hbnw);
                Double hbnwi=resultForAllMs.getHublike_no_weight_in().get(ms);
                r.createCell(5).setCellValue(hbnwi==null?0.0:hbnwi);
                Double hbnwo=resultForAllMs.getHublike_no_weight_out().get(ms);
                r.createCell(6).setCellValue(hbnwo==null?0.0:hbnwo);
                Double hw=resultForAllMs.getHub_weight().get(ms);
                r.createCell(7).setCellValue(hw==null?0.0:hw);
                Double hnw=resultForAllMs.getHub_no_weight().get(ms);
                r.createCell(8).setCellValue(hnw==null?0.0:hnw);
                Double cy=resultForAllMs.getCyclic().get(ms);
                r.createCell(9).setCellValue(cy==null?0.0:cy);
                Double uw=resultForAllMs.getUnstable_weight().get(ms);
                r.createCell(10).setCellValue(uw==null?0.0:uw);
                Double unw=resultForAllMs.getUnstable_no_weight().get(ms);
                r.createCell(11).setCellValue(unw==null?0.0:unw);
                rowCount++;
            }
            rowCount=rowCount+3;
            Row rw=sheet.createRow(rowCount);
            rw.createCell(0).setCellValue("versionRelations");
            Row rww=sheet.createRow(++rowCount);
            rww.createCell(0).setCellValue("sourceMicro");
            rww.createCell(1).setCellValue("targetMicro");
            rww.createCell(2).setCellValue("value");
            rowCount++;
            for(PairRelation pr:resultForAllMs.getRelationWeight()){
                Row rr=sheet.createRow(rowCount);
                rr.createCell(0).setCellValue(pr.getSourceName());
                rr.createCell(1).setCellValue(pr.getTargetName());
                rr.createCell(2).setCellValue(pr.getValue());
                rowCount++;
            }

        }
        File file=new File(path);
        OutputStream outputStream=new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();
    }

}
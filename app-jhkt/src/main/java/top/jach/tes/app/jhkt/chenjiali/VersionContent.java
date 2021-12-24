package top.jach.tes.app.jhkt.chenjiali;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import top.jach.tes.app.dev.DevApp;
import top.jach.tes.app.jhkt.chenjiali.result.ResultAll;
import top.jach.tes.app.mock.Environment;
import top.jach.tes.app.mock.InfoTool;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.plugin.jhkt.DataAction;
import top.jach.tes.plugin.jhkt.InfoNameConstant;
import top.jach.tes.plugin.jhkt.microservice.Microservice;
import top.jach.tes.plugin.jhkt.microservice.MicroservicesInfo;
import top.jach.tes.plugin.tes.code.git.version.Version;
import top.jach.tes.plugin.tes.code.git.version.VersionsInfo;
import top.jach.tes.plugin.tes.code.repo.ReposInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author:AdminChen
 * @date:2020/9/2
 * @description:
 */
public class VersionContent extends DevApp {
    public static void main(String[] args) throws IOException {
        Context context = Environment.contextFactory.createContext(Environment.defaultProject);

        ReposInfo reposInfo = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.TargetSystem, ReposInfo.class);

        VersionsInfo versionsInfoForRelease = DataAction.queryLastInfo(context, InfoNameConstant.VersionsForRelease, VersionsInfo.class);

        Map<String,List<String>> res=new HashMap<>();
        //ResultAll result = new ResultAll();
        for (int i = 0; i < versionsInfoForRelease.getVersions().size()-1; i++) {
            Version version = versionsInfoForRelease.getVersions().get(i);
            // 查询version版本下的所有微服务
            MicroservicesInfo microservices = DataAction.queryLastMicroservices(context, reposInfo.getId(), null, version);

            //存储单个版中所有微服务名称
            List<String> microserviceNames = microservices.microserviceNames();
            PairRelationsInfo pairRelationsInfoWithoutWeight = microservices.callRelationsInfoByTopicWithJson(false,"D:\\data\\relations.json").deWeight();
            pairRelationsInfoWithoutWeight.setName(InfoNameConstant.MicroserviceExcludeSomeCallRelation);
            InfoTool.saveInputInfos(pairRelationsInfoWithoutWeight);

            res.put(version.getVersionName(),microserviceNames);
        }
        export("D://data//versionAndRelation.xls",res);
    }

    public static void export(String path,Map<String,List<String>> microserviceVersion) throws IOException {
        Workbook workbook=new HSSFWorkbook();
        for(Map.Entry<String,List<String>> en:microserviceVersion.entrySet()){
            Sheet sheet=workbook.createSheet(en.getKey());//一个版本一个sheet
            Row row=sheet.createRow(0);
            row.createCell(0).setCellValue("versionName");
            row.createCell(1).setCellValue("microservices");
            for(int i=0;i<en.getValue().size();i++){
                Row r=sheet.createRow(i+1);
                if(i==0){
                    r.createCell(0).setCellValue(en.getKey());
                }
                r.createCell(1).setCellValue(en.getValue().get(i));
            }
            Row rr=sheet.createRow(en.getValue().size()+1);
            rr.createCell(0).setCellValue("MsNumber:");
            rr.createCell(1).setCellValue(en.getValue().size());
        }
        File file=new File(path);
        OutputStream outputStream=new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();
    }
}

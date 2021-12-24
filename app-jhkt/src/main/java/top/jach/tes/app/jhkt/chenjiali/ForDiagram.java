package top.jach.tes.app.jhkt.chenjiali;

import org.apache.commons.compress.utils.Lists;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import top.jach.tes.app.dev.DevApp;
import top.jach.tes.app.mock.Environment;
import top.jach.tes.app.mock.InfoTool;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.api.exception.ActionExecuteFailedException;
import top.jach.tes.core.impl.domain.relation.PairRelation;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.plugin.jhkt.DataAction;
import top.jach.tes.plugin.jhkt.InfoNameConstant;
import top.jach.tes.plugin.jhkt.git.commit.GitCommitsForMicroserviceInfo;
import top.jach.tes.plugin.jhkt.microservice.Microservice;
import top.jach.tes.plugin.jhkt.microservice.MicroservicesInfo;
import top.jach.tes.plugin.tes.code.git.commit.GitCommit;
import top.jach.tes.plugin.tes.code.git.version.Version;
import top.jach.tes.plugin.tes.code.git.version.VersionsInfo;
import top.jach.tes.plugin.tes.code.repo.ReposInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author:AdminChen
 * @date:2020/9/11
 * @description:
 */
public class ForDiagram extends DevApp {
    public static void main(String[] args) throws ActionExecuteFailedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        Context context = Environment.contextFactory.createContext(Environment.defaultProject);
//InfoTool对某些查询基本操作重复较多的进行统一封装，把基本操作封装起来，供调用
        ReposInfo reposInfo = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.TargetSystem, ReposInfo.class);

        VersionsInfo versionsInfoForRelease = DataAction.queryLastInfo(context, InfoNameConstant.VersionsForRelease, VersionsInfo.class);
        //每个版本的所有微服务对应的GitCommit的集合
        Map<String,Map<String, List<GitCommit>>> versionMicroGitCommits=new HashMap<>();
        Map<String,List<String>> versionMicros=new HashMap<>();
        Map<String,List<PairRelation>> versionRelations=new HashMap<>();
        for (int i = 0; i < versionsInfoForRelease.getVersions().size(); i++) {
            Version version = versionsInfoForRelease.getVersions().get(i);//每一轮循环代表一个sheet页
            // 查询version版本下的所有微服务
            MicroservicesInfo microservices = DataAction.queryLastMicroservices(context, reposInfo.getId(), null, version);
            Map<String, List<GitCommit>> gitCommitsForMicroserviceInfoMap = new HashMap<>();
            for(Microservice microservice: microservices) {//由于MicroserviceInfo类实现了Iterator方法，因此可以这样遍历
                GitCommitsForMicroserviceInfo gitCommitsForMicroserviceInfo = DataAction.queryLastGitCommitsForMicroserviceInfo(context, reposInfo.getId(), microservice.getElementName(), version);
               if(gitCommitsForMicroserviceInfo!=null){
                   gitCommitsForMicroserviceInfoMap.put(microservice.getElementName(), gitCommitsForMicroserviceInfo.getGitCommits());
               }
            }
            versionMicroGitCommits.put(version.getVersionName(),gitCommitsForMicroserviceInfoMap);

            List<String> microserviceNames = microservices.microserviceNames();
            versionMicros.put(version.getVersionName(),microserviceNames);
            PairRelationsInfo pairRelationsInfoWithWeight = microservices.callRelationsInfoByTopicWithJson(true,"D:\\data\\relations.json");
            pairRelationsInfoWithWeight.setName(InfoNameConstant.MicroserviceExcludeSomeCallRelation);
            //InfoTool.saveInputInfos(pairRelationsInfoWithWeight);
            List<PairRelation> relations = Lists.newArrayList(pairRelationsInfoWithWeight.getRelations().iterator());
            versionRelations.put(version.getVersionName(),relations);
            }
 /*       Workbook wb=new HSSFWorkbook();
        for(Map.Entry<String,Map<String,List<GitCommit>>> entry:versionMicroGitCommits.entrySet()){
            Sheet sheet=wb.createSheet(entry.getKey());
            Row r=sheet.createRow(0);
            r.createCell(0).setCellValue("microservice");
            r.createCell(1).setCellValue("commitsCount");
            int j=1;
            int allcount=0;
            for(Map.Entry<String,List<GitCommit>> enn:entry.getValue().entrySet()){
                Row err=sheet.createRow(j);
                err.createCell(0).setCellValue(enn.getKey());
                err.createCell(1).setCellValue(enn.getValue().size());
                allcount+=enn.getValue().size();
                j++;
            }
            Row rre=sheet.createRow(j);
            j++;
            rre.createCell(0).setCellValue("all commits count:");
            rre.createCell(1).setCellValue(allcount);
        }
        File file1=new File("D:\\data\\versionGitcommits1.xls");
        OutputStream outputStream1=new FileOutputStream(file1);
        wb.write(outputStream1);
        outputStream1.close();
        wb.close();*/

        //export("D\\micros.xls",versionMicros);

        Workbook workbook=new HSSFWorkbook();
        for(Map.Entry<String,List<PairRelation>> en:versionRelations.entrySet()){
            Sheet sheet=workbook.createSheet(en.getKey());
            Row row=sheet.createRow(0);
            row.createCell(0).setCellValue("sourceMicro");
            row.createCell(1).setCellValue("targetMicro");
            row.createCell(2).setCellValue("value");
            int i=1;
            for(PairRelation pr:en.getValue()){
                Row r=sheet.createRow(i);
                r.createCell(0).setCellValue(pr.getSourceName());
                r.createCell(1).setCellValue(pr.getTargetName());
                r.createCell(2).setCellValue(pr.getValue());
                i++;
            }
        }
        File file=new File("D:\\data\\versionRelations1.xls");
        OutputStream outputStream=new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();

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

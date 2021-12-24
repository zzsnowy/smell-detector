package top.jach.tes.app.jhkt.chenjiali;

//import com.sun.tools.javac.resources.version;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.eclipse.jgit.api.Git;
import top.jach.tes.app.dev.DevApp;
import top.jach.tes.app.jhkt.chenjiali.result.ResultAll;
import top.jach.tes.app.mock.Environment;
import top.jach.tes.app.mock.InfoTool;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.api.exception.ActionExecuteFailedException;
import top.jach.tes.core.impl.domain.element.ElementsValue;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.plugin.jhkt.DataAction;
import top.jach.tes.plugin.jhkt.InfoNameConstant;
import top.jach.tes.plugin.jhkt.arcsmell.hublink.HublinkAction;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvAction;
import top.jach.tes.plugin.jhkt.arcsmell.ud.UdAction;
import top.jach.tes.plugin.jhkt.arcsmell.ui.UiResult;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author:AdminChen
 * @date:2020/9/2
 * @description:输出unstable dependency异味的检测结果
 */
public class OutputUiMain extends DevApp {
  /*  public static void main(String[] args) throws IOException {
        Context context = Environment.contextFactory.createContext(Environment.defaultProject);

        ReposInfo reposInfo = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.TargetSystem, ReposInfo.class);

        VersionsInfo versionsInfoForRelease = DataAction.queryLastInfo(context, InfoNameConstant.VersionsForRelease, VersionsInfo.class);

        Map<String,Map<String,Double>> outres=new HashMap<>();

        Map<String,Map<String,Integer>> microFileCount=new HashMap<>();//每个版本对应微服务包含的文件数
        for (int i = 0; i<versionsInfoForRelease.getVersions().size(); i++) {
            Version version = versionsInfoForRelease.getVersions().get(i);
            // 查询version版本下的所有微服务
            MicroservicesInfo microservices = DataAction.queryLastMicroservices(context, reposInfo.getId(), null, version);

            //存储单个版中所有微服务名称
            List<String> microserviceNames = microservices.microserviceNames();
            //新的不带权重的relation不去重
            PairRelationsInfo pairRelationsInfoWithWeight = microservices.callRelationsInfoByTopicWithJson(true,"D:\\data\\relations.json");
            PairRelationsInfo pairRelationsInfoWithoutWeight = microservices.callRelationsInfoByTopicWithJson(false,"D:\\data\\relations.json").deweight();
            pairRelationsInfoWithWeight.setName(InfoNameConstant.MicroserviceExcludeSomeCallRelation);
            InfoTool.saveInputInfos(pairRelationsInfoWithWeight);

          *//*  //查询version版本下所有微服务的commit信息
            //Map<String, GitCommitsForMicroserviceInfo> gitCommitsForMicroserviceInfoMap = new HashMap<>();
            Map<String,Integer> microFile=new HashMap<>();
           // List<GitCommit> gct=new ArrayList<>();
            for(Microservice microservice: microservices){//由于MicroserviceInfo类实现了Iterator方法，因此可以这样遍历
                GitCommitsForMicroserviceInfo gitCommitsForMicroserviceInfo = DataAction.queryLastGitCommitsForMicroserviceInfo(context, reposInfo.getId(), microservice.getElementName(), version);
                //gitCommitsForMicroserviceInfoMap.put(microservice.getElementName(),gitCommitsForMicroserviceInfo);
                int count=0;
                for(GitCommit gc:gitCommitsForMicroserviceInfo.getGitCommits()){
                    count=count+gc.getDiffFiles().size();
                }
                *//**//*if(gitCommitsForMicroserviceInfo==null){
                    //System.out.println("GitCommitsForMicroserviceInfo  "+microservice.getElementName()+"  "+version.getVersionName());
                    continue;
                }
                gct.addAll(gitCommitsForMicroserviceInfo.getGitCommits());*//**//*
            }*//*


            Map<String,Double> rel=UdAction.calInstability(microservices,pairRelationsInfoWithWeight);
            outres.put(version.getVersionName(),rel);
        }
        Workbook workbook=new HSSFWorkbook();
        for(Map.Entry<String,Map<String,Double>> en:outres.entrySet()){
            Sheet sheet=workbook.createSheet(en.getKey());
            Row row=sheet.createRow(0);
            row.createCell(0).setCellValue("microservice");
            row.createCell(1).setCellValue("instability");
            int rr=1;
            for(Map.Entry<String,Double> enn:en.getValue().entrySet()){
                Row rrr=sheet.createRow(rr);
                rrr.createCell(0).setCellValue(enn.getKey());
                rrr.createCell(1).setCellValue(enn.getValue());
                rr++;
            }
        }
        File file=new File("D:\\data\\versions5\\instabilityWithWeight.xls");
        OutputStream outputStream=new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();
    }*/
    public static void main(String[] args) throws ActionExecuteFailedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        Context context = Environment.contextFactory.createContext(Environment.defaultProject);

        ReposInfo reposInfo = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.TargetSystem, ReposInfo.class);

        VersionsInfo versionsInfoForRelease = DataAction.queryLastInfo(context, InfoNameConstant.VersionsForRelease, VersionsInfo.class);

        //ResultAll result=new ResultAll();
        //versionsInfoForRelease.getVersions().size()
        Map<String,List<UiResult>> result=new HashMap<>();
        List<String> vname=new ArrayList<>();
        Map<String,List<ElementsValue>> ress=new HashMap<>();
        Map<String,Map<String,Double>> vermicroIns=new HashMap<>();
        for (int i = 0; i<versionsInfoForRelease.getVersions().size(); i++) {
            Version version = versionsInfoForRelease.getVersions().get(i);
            List<ElementsValue> see=new ArrayList<>();
            vname.add(version.getVersionName());
            // 查询version版本下的所有微服务
            MicroservicesInfo microservices = DataAction.queryLastMicroservices(context, reposInfo.getId(), null, version);

            //存储单个版中所有微服务名称
            List<String> microserviceNames = microservices.microserviceNames();
            //新的不带权重的relation不去重
            PairRelationsInfo pairRelationsInfoWithWeight = microservices.callRelationsInfoByTopicWithJson(true,"D:\\data\\relations.json");
            PairRelationsInfo pairRelationsInfoWithoutWeight = microservices.callRelationsInfoByTopicWithJson(false,"D:\\data\\relations.json").deWeight();
            pairRelationsInfoWithoutWeight.setName(InfoNameConstant.MicroserviceExcludeSomeCallRelation);
            InfoTool.saveInputInfos(pairRelationsInfoWithoutWeight);

            //查询version版本下所有微服务的commit信息
            Map<String, GitCommitsForMicroserviceInfo> gitCommitsForMicroserviceInfoMap = new HashMap<>();
            List<GitCommit> gct=new ArrayList<>();
            for(Microservice microservice: microservices){//由于MicroserviceInfo类实现了Iterator方法，因此可以这样遍历
                GitCommitsForMicroserviceInfo gitCommitsForMicroserviceInfo = DataAction.queryLastGitCommitsForMicroserviceInfo(context, reposInfo.getId(), microservice.getElementName(), version);
                gitCommitsForMicroserviceInfoMap.put(microservice.getElementName(),gitCommitsForMicroserviceInfo);
                if(gitCommitsForMicroserviceInfo==null){
                    //System.out.println("GitCommitsForMicroserviceInfo  "+microservice.getElementName()+"  "+version.getVersionName());
                    continue;
                }
                gct.addAll(gitCommitsForMicroserviceInfo.getGitCommits());
            }
            //给gitCommits去重
            List<GitCommit> gitCommits=gct.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.getReposId() + "#" + o.getRepoName() + "#" + o.getSha()))),ArrayList::new));;
            Collections.sort(gitCommits);
            List<Double> inValues=new ArrayList<>();
            Map<String,Double> ttt=HublinkAction.calculateHublikeIn(pairRelationsInfoWithoutWeight).getValueMap();
            for(Map.Entry<String,Double> enn:HublinkAction.calculateHublikeIn(pairRelationsInfoWithoutWeight).getValueMap().entrySet()){
                inValues.add(enn.getValue());
            }
            Collections.sort(inValues);
            double impact=inValues.get(inValues.size()/2);//impact取后百分之50作为影响大的微服务
            //直接impact改成1
            List<UiResult> res=calUi(gitCommits,5,microservices.getMicroservices(),pairRelationsInfoWithoutWeight,impact);

            for(UiResult ur:res){
                if(ur.getUiFiles()!=null){
                    System.out.println(ur.getUiFiles().size()+"ssssssss"+ur.getUiMicroservices().size());
                }
            }
            //System.out.println(res.size());*//*
            ElementsValue ele_ud_count=UdAction.calculateUdNew(microservices,pairRelationsInfoWithoutWeight);
            ElementsValue ele_ud_ratio=UdAction.calculateUd(microservices,pairRelationsInfoWithoutWeight);
            see.add(ele_ud_count);
            see.add(ele_ud_ratio);
            ress.put(version.getVersionName(),see);//保存ud的计算结果
            result.put(version.getVersionName(),res);//保存ui的计算结果
            //保存每个微服务的Instability值
            vermicroIns.put(version.getVersionName(),UdAction.calInstability(microservices,pairRelationsInfoWithoutWeight));

        }
        //到这里位置result里的数据是正确的
        //exportUi("D:\\data\\UiOutput.xls",result,vname);
        Workbook workbook=new HSSFWorkbook();
        for(Map.Entry<String,List<ElementsValue>> en:ress.entrySet()){
            Sheet sheet=workbook.createSheet(en.getKey());
            Row row=sheet.createRow(0);
            row.createCell(0).setCellValue("microservice");
            row.createCell(1).setCellValue("udCount");
            row.createCell(2).setCellValue("udRatio");
            int rr=1;
            Map<String,Double> ud1=en.getValue().get(0).getValueMap();
            Map<String,Double> ud2=en.getValue().get(1).getValueMap();
            for(String micr:ud1.keySet()){
                Row rrr=sheet.createRow(rr);
                rrr.createCell(0).setCellValue(micr);
                rrr.createCell(1).setCellValue(ud1.get(micr));
                rrr.createCell(2).setCellValue(ud2.get(micr));
                rr++;
            }

        }
        File file=new File("D:\\data\\versions5\\udWithoutWeight.xls");
        //File file=new File("D:\\data\\versions5\\udWithWeight.xls");
        OutputStream outputStream=new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();

    }

    public static void exportUi(String path,Map<String,List<UiResult>> results,List<String> vname) throws IOException {
        Workbook workbook=new HSSFWorkbook();
        for(String version:vname){
            Sheet sheet=workbook.createSheet(version);//一个版本一个sheet
            Row row=sheet.createRow(0);
            row.createCell(0).setCellValue("microservice");
            row.createCell(1).setCellValue("msFileCount");
            row.createCell(2).setCellValue("inflFilesCount");
            row.createCell(3).setCellValue("inflMicrosCount");
            row.createCell(4).setCellValue("inflMicroservices");
            row.createCell(5).setCellValue("inflCount");
            int rr=1;
            for(UiResult ur:results.get(version)){
                Row r=sheet.createRow(rr);
                r.createCell(0).setCellValue(ur.getMicroservice());
                r.createCell(1).setCellValue(ur.getMsFiles().size());
                if(ur.getUiMicroservices()==null){
                    rr++;
                    continue;
                }
                //r.createCell(2).setCellValue(ur.getMsFiles().size());
                r.createCell(2).setCellValue(ur.getUiFiles().size());
                r.createCell(3).setCellValue(ur.getUiMicroservices().size());
                rr++;
                for(Map.Entry<String,Integer> e:ur.getUiMicroservices().entrySet()){
                    Row kr=sheet.createRow(rr++);
                    kr.createCell(4).setCellValue(e.getKey());
                    kr.createCell(5).setCellValue(e.getValue());
                }
            }
        }

        File file=new File(path);
        OutputStream outputStream=new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();
    }

    public static ElementsValue calUiElement(List<UiResult> uiResults,List<Microservice> microservices){
        Map<String,Double> res=new HashMap<>();
        for(Microservice microservice:microservices){
            String name=microservice.getElementName();
            double value=0.0;
            for(UiResult ur:uiResults){
                if(ur.getUiMicroservices()!=null&&ur.getUiMicroservices().keySet().contains(name)){
                    value=value+1.0;
                }
            }
            res.put(name,value);
        }
        ElementsValue element=ElementsValue.createInfo();
        for(Map.Entry<String,Double> enn:res.entrySet()){
            element.put(enn.getKey(),enn.getValue());
        }
        return element;
    }
    public static List<UiResult> calUi(List<GitCommit> gitCommits, int len, List<Microservice> microservices, PairRelationsInfo pairRelationsInfo, double impact){
        List<UiResult> result=new ArrayList<>();
        //这里写ui的输出逻辑，主要基于hublike和mv的结果做一层输出形式更改然后输出就可以了
        ElementsValue inElement= HublinkAction.calculateHublikeIn(pairRelationsInfo);
        Map<String,Double> inLinks=inElement.getValueMap();
        Map<String, Map<String,Integer>> mvFiles= MvAction.detectMvResult(gitCommits,len,microservices).getResultFiles();
        Map<String,Map<String,Integer>> mvMicros=MvAction.detectMvResultForUi(gitCommits,len,microservices).getResultFiles();
        Map<String,Set<String>> mcroFiles=MvAction.findMicroFiles(gitCommits,microservices);
        for(Microservice microservice:microservices){
            String name=microservice.getElementName();
            UiResult res=new UiResult();
            Map<String,Integer> Ufiles=new HashMap<>();
            //Map<String,Integer> Umicros=new HashMap<>();
            res.setMicroservice(name);//对于每个微服务，若不满足条件，则uiresult后两个属性不设值，为空
            res.setMsFiles(mcroFiles.get(name));
            //在get(name)这一步有可能得到null，共21个微服务最后Hubike得到的是17个的值
            if(inLinks.containsKey(name)){
                if(inLinks.get(name)>impact){
                    for(String file:mvFiles.keySet()){
                        String prefix = MvAction.getMicroserviceNameByFilePath(file, microservices);//file对应的微服务名
                        if(name.equals(prefix)){
                            for(Map.Entry<String,Integer> en:mvFiles.get(file).entrySet()){
                                Ufiles.put(en.getKey(),en.getValue());
                            }
                            //Ufiles.putAll(mvFiles.get(file));
                        }
                    }

                    res.setUiFiles(Ufiles);//当前微服务所包含文件所影响的文件集合
                    res.setUiMicroservices(mvMicros.get(name));//当前微服务影响的微服务集合
                }
            }

            result.add(res);
        }
        //可参考自己原来写的calculateUi代码
        return result;
    }
}

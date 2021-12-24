package top.jach.tes.app.jhkt.chenjiali;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.eclipse.jgit.api.Git;
import top.jach.tes.app.dev.DevApp;
import top.jach.tes.app.jhkt.lijiaqi.result.Mv;
import top.jach.tes.app.jhkt.lijiaqi.result.ResultForCommit;
import top.jach.tes.app.jhkt.lijiaqi.result.ResultForMs;
import top.jach.tes.app.mock.Environment;
import top.jach.tes.app.mock.InfoTool;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.api.exception.ActionExecuteFailedException;
import top.jach.tes.plugin.jhkt.DataAction;
import top.jach.tes.plugin.jhkt.InfoNameConstant;
import top.jach.tes.plugin.jhkt.analysis.MicroserviceAttrsInfo;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvAction;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvCommit;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvCommitInfo;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvResult;
import top.jach.tes.plugin.jhkt.git.commit.GitCommitsForMicroserviceInfo;
import top.jach.tes.plugin.jhkt.metrics.MetricsInfo;
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author:AdminChen
 * @date:2020/8/1
 * @description:计算并输出有关mv的所有结果及中间结果————已废
 */
public class systest extends DevApp {
    public static void main(String[] args) throws ActionExecuteFailedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        Context context = Environment.contextFactory.createContext(Environment.defaultProject);
//InfoTool对某些查询基本操作重复较多的进行统一封装，把基本操作封装起来，供调用
        ReposInfo reposInfo = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.TargetSystem, ReposInfo.class);

        VersionsInfo versionsInfoForRelease = DataAction.queryLastInfo(context, InfoNameConstant.VersionsForRelease, VersionsInfo.class);
        for(int i=0;i<versionsInfoForRelease.getVersions().size()-1;i++){
            System.out.println(versionsInfoForRelease.getVersions().get(i).getVersionName());
        }
        boolean wetherWeight = true;//是否按照权重计算hublink依赖
        List<MicroserviceAttrsInfo> microserviceAttrsInfos = new ArrayList<>();//创建excel表格基本信息的源数据
        List<CorrelationDataInfo> correlationDataInfos = new ArrayList<>();//创建excel表格分析数据信息的源数据
        List<MetricsInfo> metricsInfos = new ArrayList<>();//创建excel表格可维护性数据信息的源数据
        ResultForCommit result=new ResultForCommit();
        Map<String,List<GitCommit>> gitCommitWithVersion=new LinkedHashMap<>();//保持有序，每个版本的commit对应result每个版本
        List<String> vname=new ArrayList<>();
        for (int i = 0; i < versionsInfoForRelease.getVersions().size(); i++) {
            Version version = versionsInfoForRelease.getVersions().get(i);//每一轮循环代表一个sheet页
            vname.add(version.getVersionName());
            //查询version name
            String n_version = version.getVersionName();

            // 查询version版本下的所有微服务
            MicroservicesInfo microservices = DataAction.queryLastMicroservices(context, reposInfo.getId(), null, version);
           /* for(Microservice ms:microservices.getMicroservices()){
               System.out.println( ms.getElementName());
            }*/
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

            MvCommitInfo mvCommitInfo= MvAction.detectMvCommitForFiles(gitCommits,5,microservices.getMicroservices());
            MvResult mvResult=MvAction.detectMvResult(gitCommits,5,microservices.getMicroservices());

            //System.out.println(mvCommitInfo.getMvCommits());
            result.put(n_version,mvCommitInfo);

            gitCommitWithVersion.put(n_version,gitCommits);
        }
        exportmvFiles("D:\\data\\mvOutput.xls",result,vname);
    }

    private static String getMethodName(String fildeName){
        byte[] items = fildeName.getBytes();
        if((char) items[0] >= 'A' && (char) items[0]<='Z'){
            return "get"+fildeName;
        }
        items[0] = (byte) ((char) items[0] - 'a' + 'A');
        return "get"+ new String(items);
    }

    public static String getCommitContent(GitCommit gitCommit) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        StringBuilder sb=new StringBuilder();
        Field[] fields = GitCommit.class.getDeclaredFields();
        for(Field field:fields){
            String fieldName = field.getName();
            if(fieldName.equals("_data_struct_version")||fieldName.equals("cherriedFromShas")||fieldName.equals("parentShas")||fieldName.equals("statisticDiffFiles")||fieldName.equals("diffFiles")) continue;
            sb.append(fieldName+":");
            Method m = gitCommit.getClass().getMethod(getMethodName(fieldName));
            Object val=m.invoke(gitCommit);
            if (val != null) {
                sb.append(val);
            }else{
                sb.append("null");
            }
            sb.append(',');

        }
        return sb.toString();
    }

    public static void exportmvFiles(String path, ResultForCommit results,List<String> vname) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
      /*  if(!dir.exists()){
            dir.mkdirs();
        }else{
            FileUtils.cleanDirectory(dir);
        }*/
        Workbook workbook=new HSSFWorkbook();
        for(String version:vname){//每个版本一页
            /*File file = new File(dir.getAbsolutePath()+"/"+version+".csv");//按每个版本创建一个csv文件
            StringBuilder sb = new StringBuilder();*/
            Sheet sheet=workbook.createSheet(version);//一个版本一个sheet
            Row row=sheet.createRow(0);
            Cell cell1=row.createCell(0);
            cell1.setCellValue("sourceFile");
            Cell cell2=row.createCell(1);
            cell2.setCellValue("sourceCommitIndex");
            Cell cell3=row.createCell(2);
            cell3.setCellValue("targetFile");
            Cell cell4=row.createCell(3);
            cell4.setCellValue("targetCommitIndex");
            Cell cell5=row.createCell(4);
            cell5.setCellValue("sourceCommit");
            Cell cell6=row.createCell(5);
            cell6.setCellValue("targetCommit");

            List<GitCommit> commitList=results.getResults().get(version).getCommitList();
            Map<String, Map<MvCommit,Integer>> mvCommits=results.getResults().get(version).getMvCommits();
            int rowCount=1;//两层循环得到的行数已经超出workbook可承受的最大行数
            boolean isMax=false;
            for(Map.Entry<String,Map<MvCommit,Integer>> entry:mvCommits.entrySet()){
                if(isMax){
                    break;
                }
                for(MvCommit mc:entry.getValue().keySet()){
                    if(rowCount==65536){
                        isMax=true;
                        break;
                    }
                    Row r=sheet.createRow(rowCount++);
                    Cell c0=r.createCell(0);
                    c0.setCellValue(mc.getSourceFile());
                    Cell c1=r.createCell(1);
                    c1.setCellValue(mc.getSourceCommitIndex());
                    Cell c2=r.createCell(2);
                    c2.setCellValue(mc.getTargetFile());
                    Cell c3=r.createCell(3);
                    c3.setCellValue(mc.getTargetCommitIndex());
                    Cell c4=r.createCell(4);
                    c4.setCellValue(getCommitContent(commitList.get(mc.getSourceCommitIndex())));
                    Cell c5=r.createCell(5);
                    c5.setCellValue(getCommitContent(commitList.get(mc.getTargetCommitIndex())));
                }

            }
        }
        File file=new File(path);
        OutputStream outputStream=new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();

    }

    public static void exportCommits(File dir,Map<String,List<GitCommit>> gitCommitWithVersion) throws IOException {
        if(!dir.exists()){
            dir.mkdirs();
        }else{
            FileUtils.cleanDirectory(dir);
        }
        for(Map.Entry<String,List<GitCommit>> entry:gitCommitWithVersion.entrySet()){

        }
    }
}

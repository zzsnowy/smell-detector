package top.jach.tes.app.jhkt.chenjiali;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import top.jach.tes.app.dev.DevApp;
import top.jach.tes.app.jhkt.chenjiali.result.ResultForAllMs;
import top.jach.tes.app.jhkt.lijiaqi.result.Mv;
import top.jach.tes.app.jhkt.lijiaqi.result.Result;
import top.jach.tes.app.jhkt.lijiaqi.result.ResultForMs;
import top.jach.tes.app.mock.Environment;
import top.jach.tes.app.mock.InfoTool;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.api.exception.ActionExecuteFailedException;
import top.jach.tes.core.impl.domain.element.ElementsValue;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.plugin.jhkt.DataAction;
import top.jach.tes.plugin.jhkt.InfoNameConstant;
import top.jach.tes.plugin.jhkt.analysis.MicroserviceAttrsInfo;
import top.jach.tes.plugin.jhkt.arcsmell.cyclic.CyclicAction;
import top.jach.tes.plugin.jhkt.arcsmell.hublink.HublinkAction;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvDetail;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvResult;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvValue;
import top.jach.tes.plugin.jhkt.dts.DtssInfo;
import top.jach.tes.plugin.jhkt.git.commit.GitCommitsForMicroserviceInfo;
import top.jach.tes.plugin.jhkt.maintain.MainTain;
import top.jach.tes.plugin.jhkt.maintain.MainTainsInfo;
import top.jach.tes.plugin.jhkt.microservice.Microservice;
import top.jach.tes.plugin.jhkt.microservice.MicroservicesInfo;
import top.jach.tes.plugin.tes.code.git.commit.GitCommit;
import top.jach.tes.plugin.tes.code.git.version.Version;
import top.jach.tes.plugin.tes.code.git.version.VersionsInfo;
import top.jach.tes.plugin.tes.code.repo.ReposInfo;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author:AdminChen
 * @date:2020/12/21
 * @description:mv异味计算
 */
public class CommitRelatedMain extends DevApp {

    public static void main(String[] args) throws ActionExecuteFailedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        Context context = Environment.contextFactory.createContext(Environment.defaultProject);

        ReposInfo reposInfo = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.TargetSystem, ReposInfo.class);

        VersionsInfo versionsInfoForRelease = DataAction.queryLastInfo(context, InfoNameConstant.VersionsForRelease, VersionsInfo.class);


        Result result = new Result();
        for (int i = 0; i < versionsInfoForRelease.getVersions().size(); i++) {
            Version version = versionsInfoForRelease.getVersions().get(i);

            // 查询version版本下的所有微服务
            MicroservicesInfo microservices = DataAction.queryLastMicroservices(context, reposInfo.getId(), null, version);

            //存储单个版中所有微服务名称
            List<String> microserviceNames = microservices.microserviceNames();

            // 计算并存储微服务间的调用关系，用于后续架构异味的计算


            // 查询version版本下问题单数据
            DtssInfo dtssInfo = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.BugDts, DtssInfo.class);

            // 查询version版本下问题单和微服务关系的数据
            PairRelationsInfo bugMicroserviceRelations = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.RelationBugAndMicroservice, PairRelationsInfo.class);

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
           List<Mv> mvs = Mv.CalculateMvs(new int[]{5},new int[]{10},new int[]{5},new double[]{0.5},gitCommits, microservices.getMicroservices());



            ResultForMs resultForMs = new ResultForMs();
            result.put(version.getVersionName(), resultForMs);
            resultForMs.setMicroservice(microserviceNames);
            resultForMs.setMvs(mvs);
            //resultForMs.setUnstableDependency(udResult.getValueMap());
            //resultForMs.setUnstableInterface(uiResult.getValueMap());
            //resultForMs.setSloppys(sdResult.getValueMap());

            MainTainsInfo mainTainsInfo = MainTainsInfo.newCreateInfo(DataAction.DefaultReposId,
                    microservices,
                    gitCommitsForMicroserviceInfoMap,
                    dtssInfo,
                    bugMicroserviceRelations,
                    version.getVersionName()
            );
            Map<String, MainTain> maintainMap = mainTainsInfo.nameMainTainMap();
            for (String mn :
                    microserviceNames) {
                MainTain mainTain = maintainMap.get(mn);
                resultForMs.addMainTain(mainTain);
            }
        }
        // 数据导出
//        exportCSV(microserviceAttrsInfos, new File("D:\\data\\tes\\analysis\\csv"));
//        exportExcel(microserviceAttrsInfos,correlationDataInfos,metricsInfos, new File("F:\\data\\tes\\analysis"));
        //exportCsv(result, new File("D:/data/tes/analysis1/csv"));

       exportMv(result,"D:\\data\\versions5\\nearstData\\mvOutput.xls");

        //exportMvBasic(result,"D:\\data\\versions5\\nearstData\\mvBasic.xls");

      /*  for (Map.Entry<String, ResultForMs> entry:
                result.getResults().entrySet()) {
            String version = entry.getKey();
            ResultForMs resultForMs = entry.getValue();
            for (String microservice:
                    resultForMs.getMicroservice()) {
                resultForMs.getMvs().get(0);
            }
        }*/
    }

    public static void exportMvBasic(Result result,String path) throws IOException {
        Workbook workbook=new HSSFWorkbook();
        for(String vName:result.getResults().keySet()){
            ResultForMs rms=result.getResults().get(vName);
            Sheet sheet=workbook.createSheet(vName);//一个版本一个sheet
            Row row=sheet.createRow(0);
            row.createCell(0).setCellValue("microservice");
            row.createCell(1).setCellValue("mvDepens");
            row.createCell(2).setCellValue("mvFiles");
            row.createCell(3).setCellValue("commitCount");
            row.createCell(4).setCellValue("commitLineCount");
            row.createCell(5).setCellValue("CommitOverlapRatio");
            row.createCell(6).setCellValue("CommitFilesetOverlapRatio");
            row.createCell(7).setCellValue("PairwiseCommitterOverlap");
            int rowNum=1;
            for(String ms:rms.getMicroservice()){
                Row r=sheet.createRow(rowNum);
                r.createCell(0).setCellValue(ms);
                r.createCell(1).setCellValue(rms.getMvs().get(0).getMvValues().get(ms).getDependency());
                r.createCell(2).setCellValue(rms.getMvs().get(0).getMvValues().get(ms).getFile());
                r.createCell(3).setCellValue(rms.getCommitCount().get(ms)==null?0:rms.getCommitCount().get(ms));
                r.createCell(4).setCellValue(rms.getCommitLineCount().get(ms)==null?0:rms.getCommitLineCount().get(ms));
                r.createCell(5).setCellValue(rms.getCommitOverlapRatio().get(ms)==null?0:rms.getCommitOverlapRatio().get(ms));
                r.createCell(6).setCellValue(rms.getCommitFilesetOverlapRatio().get(ms)==null?0:rms.getCommitFilesetOverlapRatio().get(ms));
                r.createCell(7).setCellValue(rms.getPairwiseCommitterOverlap().get(ms)==null?0:rms.getPairwiseCommitterOverlap().get(ms));
                rowNum++;
            }
        }
        File file=new File(path);
        OutputStream outputStream=new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();
    }
    public static void exportMv(Result result,String path) throws IOException {
        Workbook workbook=new HSSFWorkbook();
        for(String vName:result.getResults().keySet()) {
           //每个微服务对应的Mv值及详细数据
            Map<String, MvValue> allValue=result.getResults().get(vName).getMvs().get(0).getMvValues(); //由于List<Mv>只有一个元素
            Sheet sheet=workbook.createSheet(vName);//一个版本一个sheet
            Row row=sheet.createRow(0);
            row.createCell(0).setCellValue("microservice");
            row.createCell(1).setCellValue("mvDepens");
            row.createCell(2).setCellValue("mvFiles");
            row.createCell(3).setCellValue("sourceMicro");
            row.createCell(4).setCellValue("targetMicro");
            row.createCell(5).setCellValue("sourceFile");
            row.createCell(6).setCellValue("targetFile");
            int rowNum=1;
            for(Map.Entry<String,MvValue> en:allValue.entrySet()){
                Row r=sheet.createRow(rowNum);
                r.createCell(0).setCellValue(en.getKey());
                r.createCell(1).setCellValue(en.getValue().getDependency());
                r.createCell(2).setCellValue(en.getValue().getFile());
                rowNum++;
                for(MvDetail md:en.getValue().getMvDetailList()){
                    Row rr=sheet.createRow(rowNum);
                    rr.createCell(3).setCellValue(md.getSourceMicro());
                    rr.createCell(4).setCellValue(md.getTargetMicro());
                    rr.createCell(5).setCellValue(md.getSourceFile());
                    rr.createCell(6).setCellValue(md.getTargetFile());
                    rowNum++;
                }

            }

           /* ResultForAllMs resultForAllMs = results.getResults().get(version);
            List<String> microserviceList = resultForAllMs.getMicroservice();
            Sheet sheet = workbook.createSheet(version);//一个版本一个sheet
            Row row = sheet.createRow(0);*/
        }
        File file=new File(path);
        OutputStream outputStream=new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();

    }

    public static void exportCsv(Result result, File dir) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if(!dir.exists()){
            dir.mkdirs();
        }
        FileUtils.cleanDirectory(dir);
        StringBuilder sb = new StringBuilder();
        Field[] fields = ResultForMs.class.getDeclaredFields();
        for (Field field:
                fields) {
            String fieldName = field.getName();
            switch (fieldName){
                case "mvs":
//                    Double dependency;
//                    Double doubleDependency; // 双方概率都超过阈值
//                    Integer file;
//                    Integer doubleFile;
                    sb.append("mv_dependency").append(",")
//                            .append("mv_doubleDependency").append(",")
                            .append("mv_file").append(",")
//                            .append("mv_doubleFile").append(",")
                    ;
                    break;
                case "undirectedCyclic":
                case "bugCount":
                case "commitAddLineCount":
                case "commitDeleteLineCount":
                    break;
                default:
                    sb.append(fieldName).append(",");
                    break;
            }
        }
        sb.append('\n');

        JSONObject jsonObject = new JSONObject();
        JSONArray jsonResults = new JSONArray();
        jsonObject.put("result", jsonResults);
        for (Map.Entry<String, ResultForMs> entry:
                result.getResults().entrySet()) {
            String version = entry.getKey();
            ResultForMs resultForMs = entry.getValue();
            sb.append(version);
            sb.append('\n');
            JSONObject resultValues = new JSONObject();
            jsonResults.add(resultValues);
            resultValues.put("version", entry.getKey());
            JSONArray jsonArrayMs = new JSONArray();
            resultValues.put("ms", jsonArrayMs);
            for (String microservice:
                    resultForMs.getMicroservice()) {
                Double hub = resultForMs.getHublikes().get(microservice);
               /* if(hub ==null || hub==0){
                    for(Field field:fields){
                        String fName = field.getName();
                        if(fName.equals("mvs")){
                            sb.append(0).append(',').append(0).append(',');//孤立的微服务自然不会存在隐性依赖
                        }else if(fName.equals("microservice")){
                            sb.append(microservice).append(',');
                        }
                    }
                    sb.append('\n');
                    continue;
                }*/
                JSONObject jsonM = new JSONObject();
                jsonArrayMs.add(jsonM);
                for (Field field:
                        fields) {
                    String fieldName = field.getName();
                    Method m = ResultForMs.class.getMethod(getMethodName(fieldName));

                    switch (fieldName){
                        case "mvs":
                            Mv mv = resultForMs.getMvs().get(0);
                            MvValue mvValue = mv.getMvValues().get(microservice);
                            sb.append(mvValue.getDependency()).append(',')
//                                .append(mvValue.getDoubleDependency()).append(',')
                                    .append(mvValue.getFile()).append(',')
//                                .append(mvValue.getDoubleFile()).append(',')
                            ;
                            jsonM.put("MVDN", mvValue.getDependency());
                            jsonM.put("MVFN", mvValue.getFile());
                            break;
                        case "undirectedCyclic":
                        case "bugCount":
                        case "commitAddLineCount":
                        case "commitDeleteLineCount":
                            break;
                        case "microservice":
                            sb.append(microservice).append(',');
                            jsonM.put("microservice", microservice);
                            break;
                        default:
                            Map map = (Map) m.invoke(resultForMs);
                            Object val = map.get(microservice);
                            if(val!=null) {
                                sb.append(val);
                                jsonM.put(fieldName, val);
                            }else{
                                sb.append(0);
                                jsonM.put(fieldName,0);
                            }
                            sb.append(',');
                            break;
                    }
                }
                sb.append('\n');
            }
        }
        sb.append("\n\n");
        Set<String> allMicroservices = result.allMicroservices();

        sb.append('\n');
        sb.append('\n');


        sb.append("MV All\n");
        Map<String, StringBuilder> mvs = new HashMap<>();
        sb.append("microservice,");
        for (Map.Entry<String, ResultForMs> entry:
                result.getResults().entrySet()) {
            sb.append("mvdn,mvin,");
            String version = entry.getKey();
            ResultForMs resultForMs = entry.getValue();
            Map<String, MvValue> mvValues = resultForMs.getMvs().get(0).getMvValues();
            for (String microservice:
                    allMicroservices) {
                StringBuilder sbmv = mvs.get(microservice);
                if (sbmv==null){
                    sbmv = new StringBuilder();
                    mvs.put(microservice, sbmv);
                }
                MvValue mv = mvValues.get(microservice);
                if(mv == null){
                    sbmv.append(",,");
                    continue;
                }
                Double mvdn = mv.getDependency();
                Integer mvin = mv.getFile();
                if(mvdn ==null ){
                    sbmv.append(',');
                }else {
                    sbmv.append(mvdn).append(',');
                }
                if(mvin ==null ){
                    sbmv.append(',');
                }else {
                    sbmv.append(mvin).append(',');
                }
            };
        }
        sb.append('\n');
        for (String microservice:
                allMicroservices) {
            sb.append(microservice).append(',')
                    .append(mvs.get(microservice)).append('\n');
        }
        sb.append('\n');

        FileUtils.write(new File(dir.getAbsolutePath()+"/analysisdata_5_3.csv"),sb, "utf8", false);
        FileUtils.write(new File(dir.getAbsolutePath()+"/lijiaqidata.json"),jsonObject.toJSONString(), "utf8", false);
        sb = null;

        StringBuilder sb1 = new StringBuilder().append("file,targetFile,targetMicro,count,per\n");
        StringBuilder sb2 = new StringBuilder(sb1);
        StringBuilder sb3 = new StringBuilder(sb1);
        StringBuilder sb4=new StringBuilder(sb1);
        int i=0;
        for (Map.Entry<String, ResultForMs> entry:
                result.getResults().entrySet()) {
            if(!entry.getKey().equals("x_3c9_x_95d.x_893.x_893.x_e09d_x_43_x_8b_x_e09f_x_e0a1")){
                continue;
            }
           /* i++;
            if(i!=4){
                continue;
            }*/
            String version = entry.getKey();
            ResultForMs resultForMs = entry.getValue();
            Mv mv = resultForMs.getMvs().get(0);
            Map<String, MvResult.MvResultForMicroservice> map = mv.getMvResult().getResults();
            //x_13/x_46f等三个是对应某个版本下的三个微服务，每个csv文件对应该微服务中存在mv架构异味的文件，
            // 以及这些文件对应的存在共同变更的文件，及共同变更的次数
            MvResult.MvResultForMicroservice mrm1 = map.get("x_13/x_46f");
            statisticsMvResult(sb1, mv, mrm1);
            MvResult.MvResultForMicroservice mrm2 = map.get("x_13/x_663");
            statisticsMvResult(sb2, mv, mrm2);
            MvResult.MvResultForMicroservice mrm3 = map.get("x_3f/x_6f2b");
            statisticsMvResult(sb3, mv, mrm3);

            MvResult.MvResultForMicroservice mrm4=map.get("x_f/x_1b");
            statisticsMvResult(sb4,mv,mrm4);
        }
        FileUtils.write(new File(dir.getAbsolutePath()+"/lijiaqidata_3_4_mv_46f.csv"),sb1, "utf8", false);
        FileUtils.write(new File(dir.getAbsolutePath()+"/lijiaqidata_3_4_mv_663.csv"),sb2, "utf8", false);
        FileUtils.write(new File(dir.getAbsolutePath()+"/lijiaqidata_3_4_mv_6f2b.csv"),sb3, "utf8", false);
        FileUtils.write(new File(dir.getAbsolutePath()+"/lijiaqidata_3_4_mv_x1b.csv"),sb4, "utf8", false);

    }

    private static void statisticsMvResult(StringBuilder sb1, Mv mv, MvResult.MvResultForMicroservice mrm) {
        for (Map.Entry<String, Map<String, Integer>> fileEntry :
                mrm.getFileToFile().entrySet()) {
            String file = fileEntry.getKey();
            Integer fcc = mv.getMvResult().getFileCommitCount().get(file);
            if(fcc<10){
                continue;
            }
            for (Map.Entry<String, Integer> targetFileEntry :
                    fileEntry.getValue().entrySet()) {
                String targetFile = targetFileEntry.getKey();
                Integer count = targetFileEntry.getValue();
                Double per = Double.valueOf(count)/fcc;
                if(per>=0.5) {//这里只输出当前微服务出发的文件对依赖，比如a服务这里只有1个文件对，但在b,c,d服务中也存在和a的文件对
                    sb1.append(file).append(',').append(targetFile).append(',').append(count).append(',').append(per).append('\n');
                }
            }
        }
    }

    private static String getMethodName(String fildeName){
        byte[] items = fildeName.getBytes();
        if((char) items[0] >= 'A' && (char) items[0]<='Z'){
            return "get"+fildeName;
        }
        items[0] = (byte) ((char) items[0] - 'a' + 'A');
        return "get"+ new String(items);
    }
}

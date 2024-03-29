package top.jach.tes.app.jhkt.lijiaqi;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import top.jach.tes.app.dev.DevApp;
import top.jach.tes.app.mock.Environment;
import top.jach.tes.app.mock.InfoTool;
import top.jach.tes.app.mock.InputInfoProfiles;
import top.jach.tes.core.api.domain.action.Action;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.api.domain.info.Info;
import top.jach.tes.core.api.dto.PageQueryDto;
import top.jach.tes.core.api.exception.ActionExecuteFailedException;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.plugin.jhkt.DataAction;
import top.jach.tes.plugin.jhkt.InfoNameConstant;
import top.jach.tes.plugin.jhkt.analysis.MicroserviceAttr;
import top.jach.tes.plugin.jhkt.analysis.MicroserviceAttrsInfo;
import top.jach.tes.plugin.jhkt.arcsmell.ArcSmell;
import top.jach.tes.plugin.jhkt.arcsmell.ArcSmellAction;
import top.jach.tes.plugin.jhkt.arcsmell.ArcSmellsInfo;
import top.jach.tes.plugin.jhkt.dts.DtssInfo;
import top.jach.tes.plugin.jhkt.git.commit.GitCommitsForMicroserviceInfo;
import top.jach.tes.plugin.jhkt.maintain.MainTain;
import top.jach.tes.plugin.jhkt.maintain.MainTainsInfo;
import top.jach.tes.plugin.jhkt.microservice.Microservice;
import top.jach.tes.plugin.jhkt.microservice.MicroservicesInfo;
import top.jach.tes.plugin.tes.code.git.commit.GitCommitsInfo;
import top.jach.tes.plugin.tes.code.git.tree.TreesInfo;
import top.jach.tes.plugin.tes.code.git.version.Version;
import top.jach.tes.plugin.tes.code.git.version.VersionsInfo;
import top.jach.tes.plugin.tes.code.go.GoPackagesInfo;
import top.jach.tes.plugin.tes.code.repo.Repo;
import top.jach.tes.plugin.tes.code.repo.ReposInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

// 继承DevApp 已加载InfoRepository等上下文环境
public class AnalysisMain extends DevApp {
    public static void main(String[] args) throws ActionExecuteFailedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        Context context = Environment.contextFactory.createContext(Environment.defaultProject);

        //下面每一步的查询得到的存储信息的对象（如microservices,dtssInfo,bugMicroserviceRelations等都作为
        // 构建microserviceAttrsInfo对象列表的microserviceAttrsInfos方法的属性，往方法中传入这些属性即可
        // 生成microserviceAttrsInfo对象，该对象直接作为excel表格的形式输出，详见Line 100）

        // 查询整个系统的微服务
        MicroservicesInfo microservices = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.MicroservicesForRepos, MicroservicesInfo.class);
        // 排除部分微服务
        microservices = MicroservicesInfo.createInfoByExcludeMicroservice(microservices,
                "x_2b", "x_1b", "x_23", "x_1d/x_6eed",
                "x_39","x_1f","x_27/x_25","c_demo/c_demoa","c_demo/c_demob",
                "x_13/x_ae5", "x_25", "x_21/7103");
        microservices.setName(InfoNameConstant.MicroservicesForReposExcludeSomeHistory);

        // 计算并存储微服务间的调用关系，用于后续架构异味的计算
        PairRelationsInfo pairRelationsInfo = microservices.callRelationsInfoByTopic(true);
        pairRelationsInfo.setName(InfoNameConstant.MicroserviceExcludeSomeCallRelation);
        InfoTool.saveInputInfos(microservices, pairRelationsInfo);

        // 查询问题单数据
        DtssInfo dtssInfo = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.BugDts, DtssInfo.class);

        // 查询问题单和微服务关系的数据
        PairRelationsInfo bugMicroserviceRelations = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.RelationBugAndMicroservice, PairRelationsInfo.class);

        // 查询所有微服务包含的commit
        Map<String, GitCommitsForMicroserviceInfo> gitCommitsForMicroserviceInfoMap = new HashMap<>();
        for (Microservice microservice :
                microservices.getMicroservices()) {
            GitCommitsForMicroserviceInfo gitCommitsForMicroserviceInfo = new GitCommitsForMicroserviceInfo();
            gitCommitsForMicroserviceInfo
                    .setReposId(microservices.getReposId())
                    .setMicroserviceName(microservice.getElementName())
                    .setStatisticDiffFiles(null)
                    .setGitCommits(null);
            List<Info> infos = Environment.infoRepositoryFactory.getRepository(GitCommitsForMicroserviceInfo.class)
                    .queryDetailsByInfoAndProjectId(gitCommitsForMicroserviceInfo, Environment.defaultProject.getId(), PageQueryDto.create(1,1).setSortField("createdTime"));
            if(infos.size()>0) {
                gitCommitsForMicroserviceInfoMap.put(microservice.getElementName(), (GitCommitsForMicroserviceInfo)infos.get(0));
            }
        }

        // 计算机构异味
        InputInfoProfiles infoProfileMap = InputInfoProfiles.InputInfoProfiles()
                .addInfoProfile(ArcSmellAction.Elements_INFO, microservices)
                .addInfoProfile(ArcSmellAction.PAIR_RELATIONS_INFO, pairRelationsInfo)
                ;

        Action action = new ArcSmellAction();
        ArcSmellsInfo arcSmellsInfo = action.execute(infoProfileMap.toInputInfos(Environment.infoRepositoryFactory), context)
                .getFirstByInfoClass(ArcSmellsInfo.class);

        // 统计微服务的各项指标,上面所有获取的数据对象都传入microserviceAttrsInfos方法中作为参数，生成一个
        //MicroserviceAttrsInfo列表，这个列表就是最终需要导出为excel列表的数据

        //重点看microserviceAttrsInfos()方法，这个方法里version是按照时间段来分的，每个时间段一张sheet
        //可在该方法中增加一个version参数，替代方法中的时间段。
        //现在要做的是用一个for循环，每个循环给一个version名，循环中计算一次该version下的数据，
        //每次循环都以上述version+一堆数据作为microserviceAttrsInfos方法的参数，调用一次该方法
        //然后将结果add进下面的microserviceAttrsInfos列表中，每循环一次都做一次，最终version循环完了这个List也就出来了
        //然后执行数据导出操作
        List<MicroserviceAttrsInfo> microserviceAttrsInfos = microserviceAttrsInfos(microservices, dtssInfo, bugMicroserviceRelations, gitCommitsForMicroserviceInfoMap, arcSmellsInfo);
//每一个microserviceAttrsInfos由版本号和MicroserviceAttr对象（存储一整张表的数据）列表组成
        // 数据导出
        exportCSV(microserviceAttrsInfos, new File("F:\\data\\tes\\analysis\\csv"));
        exportExcel(microserviceAttrsInfos, new File("F:\\data\\tes\\analysis"));
    }



    public static void exportCSV(List<MicroserviceAttrsInfo> microserviceAttrsInfos, File dir) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if(!dir.exists()){
            dir.mkdirs();
        }
        FileUtils.cleanDirectory(dir);
        for (MicroserviceAttrsInfo mai :
                microserviceAttrsInfos) {
            String version = mai.getVersion();
            File file = new File(dir.getAbsolutePath()+"/"+version+".csv");
            StringBuilder sb = new StringBuilder();
            Field[] fields = MicroserviceAttr.class.getDeclaredFields();
            for (Field field:
                    fields) {
                sb.append(field.getName());
                sb.append(',');
            }
            sb.append('\n');
            for (MicroserviceAttr ma :
                    mai.getMicroserviceAttrs()) {
                for (Field field:
                        fields) {
                    Method m = ma.getClass().getMethod("get" + getMethodName(field.getName()));
                    Object val = m.invoke(ma);
                    if (val != null) {
                        sb.append(val);
                    }
                    sb.append(',');
                }
                sb.append('\n');
            }
            FileUtils.write(file, sb.toString(), "utf8");
        }
    }

    private static String getMethodName(String fildeName){
        byte[] items = fildeName.getBytes();
        items[0] = (byte) ((char) items[0] - 'a' + 'A');
        return new String(items);
    }

    public static void exportExcel(List<MicroserviceAttrsInfo> microserviceAttrsInfos, File dir) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, IOException {
        if(!dir.exists()){
            dir.mkdirs();
        }
        exportExcelBase(microserviceAttrsInfos, dir);
        exportExcelForTCommitcountHubLink(microserviceAttrsInfos, dir);
        exportExcelForTHubLinkCommitcount(microserviceAttrsInfos, dir);
        exportExcelForTHubLinkPair(microserviceAttrsInfos, dir);

    }

    private static void exportExcelBase(List<MicroserviceAttrsInfo> microserviceAttrsInfos, File dir)throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, IOException {
        Workbook wb = new XSSFWorkbook();

        for (MicroserviceAttrsInfo mai :
                microserviceAttrsInfos) {
            String version = mai.getVersion();
            Sheet sheet = wb.createSheet(version);

            Row row = sheet.createRow(0);
            Field[] fields = MicroserviceAttr.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(fields[i].getName());
            }
            List<MicroserviceAttr> mas = mai.getMicroserviceAttrs();
            for (int i = 0; i < mas.size(); i++) {
                MicroserviceAttr ma = mas.get(i);
                Row r = sheet.createRow(i + 1);
                for (int j = 0; j < fields.length; j++) {
                    Cell cell = r.createCell(j);

                    Method m = ma.getClass().getMethod("get" + getMethodName(fields[j].getName()));
                    Object val = m.invoke(ma);
                    if (val != null) {
                        if (val instanceof Double) {
                            cell.setCellValue((Double) val);
                        } else if (val instanceof Long) {
                            cell.setCellValue((Long) val);
                        } else if (val instanceof Integer) {
                            cell.setCellValue((Integer) val);
                        } else if (val instanceof Float) {
                            cell.setCellValue((Float) val);
                        } else if (val instanceof Date) {
                            cell.setCellValue((Date) val);
                        } else {
                            cell.setCellValue(val.toString());
                        }
                    }
                }
            }
        }
        File file = new File(dir.getAbsolutePath()+"/"+"analysis.xlsx");
        if(file.exists()){
            FileUtils.forceDelete(file);
        }
        file.createNewFile();
        wb.write(new FileOutputStream(file));
    }

    private static void exportExcelForTCommitcountHubLink(List<MicroserviceAttrsInfo> microserviceAttrsInfos, File dir)
            throws IOException {
        Workbook wb_t_hublink_commitcount = new XSSFWorkbook(); //根据hublink划分两个样本
        for (MicroserviceAttrsInfo mai :
                microserviceAttrsInfos) {
            // t 检验 hublink commitcount
            Sheet sheet_t_arc = wb_t_hublink_commitcount.createSheet(mai.getVersion());
            List<Pair<Long, Long>> pairs = new ArrayList<>();
            for (MicroserviceAttr ma :
                    mai.getMicroserviceAttrs()) {
                Long hublink = ma.getHublink();
                Long commitCount = ma.getCommitCount();
                if(hublink == null || commitCount == null){
                    continue;
                }
                Pair<Long, Long> pair = Pair.of(hublink, commitCount);
                pairs.add(pair);
            }
            Collections.sort(pairs, Comparator.comparingInt(o -> o.getLeft().intValue()));
            for (int i = 0; i < pairs.size() / 2; i++) {
                Row r = sheet_t_arc.getRow(i);
                if(r==null){
                    r = sheet_t_arc.createRow(i);
                }
                r.createCell(0).setCellValue(pairs.get(i).getRight());
            }
            for (int i = pairs.size() / 2; i < pairs.size(); i++) {
                Row r = sheet_t_arc.getRow(i-pairs.size()/2);
                if(r==null){
                    r = sheet_t_arc.createRow(i-pairs.size()/2);
                }
                r.createCell(1).setCellValue(pairs.get(i).getRight());
            }
        }
        File file_t_hublink_commitcount = new File(dir.getAbsolutePath()+"/"+"analysis_t_hublink_commitcount.xlsx");
        if(file_t_hublink_commitcount.exists()){
            FileUtils.forceDelete(file_t_hublink_commitcount);
        }
        file_t_hublink_commitcount.createNewFile();
        wb_t_hublink_commitcount.write(new FileOutputStream(file_t_hublink_commitcount));
    }

    private static void exportExcelForTHubLinkCommitcount(List<MicroserviceAttrsInfo> microserviceAttrsInfos, File dir)
            throws IOException {
        Workbook wb_t_commitcount_hublink = new XSSFWorkbook(); //根据commitCount划分两个样本
        for (MicroserviceAttrsInfo mai :
                microserviceAttrsInfos) {
            // t 检验  commitcount hublink
            List<Pair<Long, Long>> pairs = new ArrayList<>();
            for (MicroserviceAttr ma :
                    mai.getMicroserviceAttrs()) {
                Long hublink = ma.getHublink();
                Long commitCount = ma.getCommitCount();
                if(hublink == null || commitCount == null){
                    continue;
                }
                Pair<Long, Long> pair = Pair.of(hublink, commitCount);
                pairs.add(pair);
            }
            Sheet sheet_t_commitcount = wb_t_commitcount_hublink.createSheet(mai.getVersion());
            Collections.sort(pairs, Comparator.comparingInt(o -> o.getRight().intValue()));
            for (int i = 0; i < pairs.size() / 2; i++) {
                Row r = sheet_t_commitcount.getRow(i);
                if(r==null){
                    r = sheet_t_commitcount.createRow(i);
                }
                r.createCell(0).setCellValue(pairs.get(i).getLeft());
            }
            for (int i = pairs.size() / 2; i < pairs.size(); i++) {
                Row r = sheet_t_commitcount.getRow(i-pairs.size()/2);
                if(r==null){
                    r = sheet_t_commitcount.createRow(i-pairs.size()/2);
                }
                r.createCell(1).setCellValue(pairs.get(i).getLeft());
            }
        }
        File file_t_commitcount_hublink = new File(dir.getAbsolutePath()+"/"+"analysis_t_commitcount_hublink.xlsx");
        if(file_t_commitcount_hublink.exists()){
            FileUtils.forceDelete(file_t_commitcount_hublink);
        }
        file_t_commitcount_hublink.createNewFile();
        wb_t_commitcount_hublink.write(new FileOutputStream(file_t_commitcount_hublink));
    }

    private static void exportExcelForTHubLinkPair(List<MicroserviceAttrsInfo> microserviceAttrsInfos, File dir)
            throws IOException {
        Workbook wb = new XSSFWorkbook(); //根据commitCount划分两个样本
        for (MicroserviceAttrsInfo mai :
                microserviceAttrsInfos) {
            // t 检验  hublink PairwiseCommitterOverlap
            List<Pair<Long, Double>> pairs = new ArrayList<>();
            for (MicroserviceAttr ma :
                    mai.getMicroserviceAttrs()) {
                Long hublink = ma.getHublink();
                Double pairwise = ma.getPairwiseCommitterOverlap();
                if(hublink == null || pairwise == null){
                    continue;
                }
                Pair<Long, Double> pair = Pair.of(hublink, pairwise);
                pairs.add(pair);
            }
            Sheet sheet = wb.createSheet(mai.getVersion());
            Collections.sort(pairs, Comparator.comparingInt(o -> o.getLeft().intValue()));
            for (int i = 0; i < pairs.size() / 2; i++) {
                Row r = sheet.getRow(i);
                if(r==null){
                    r = sheet.createRow(i);
                }
                r.createCell(0).setCellValue(pairs.get(i).getRight());
            }
            for (int i = pairs.size() / 2; i < pairs.size(); i++) {
                Row r = sheet.getRow(i-pairs.size()/2);
                if(r==null){
                    r = sheet.createRow(i-pairs.size()/2);
                }
                r.createCell(1).setCellValue(pairs.get(i).getRight());
            }
        }
        File file = new File(dir.getAbsolutePath()+"/"+"analysis_t_hublink_pairwiseCommitterOverlap.xlsx");
        if(file.exists()){
            FileUtils.forceDelete(file);
        }
        file.createNewFile();
        wb.write(new FileOutputStream(file));
    }

    private static List<MicroserviceAttrsInfo> microserviceAttrsInfos(MicroservicesInfo microservices,
                                                                      DtssInfo dtssInfo,
                                                                      PairRelationsInfo bugMicroserviceRelations,
                                                                      Map<String, GitCommitsForMicroserviceInfo> gitCommitsForMicroserviceInfoMap,
                                                                      ArcSmellsInfo arcSmellsInfo) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        format.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        List<MicroserviceAttrsInfo> microserviceAttrsInfos = new ArrayList<>();
        int[] ds = {1,2,3,6};
        for (int di = 0; di < ds.length; di++) {
            int d = ds[di];
            for (int i = 0; i+d < 7; i++) {
                Calendar start = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
                start.set(2019, 5+i, 1,0,0,0);
                Calendar end = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
                end.set(2019, 5+i+d, 1,0,0,0);

                MainTainsInfo mainTainsInfo = MainTainsInfo.createInfo(DataAction.DefaultReposId,
                        microservices,
                        gitCommitsForMicroserviceInfoMap,
                        dtssInfo,
                        bugMicroserviceRelations,
                        start.getTimeInMillis(),
                        end.getTimeInMillis()
                );
                Map<String, MainTain> map = mainTainsInfo.nameMainTainMap();

                MicroserviceAttrsInfo microserviceAttrsInfo = MicroserviceAttrsInfo.createInfo();
                microserviceAttrsInfo.setVersion(format.format(start.getTime())+"_"+format.format(end.getTime()));
                microserviceAttrsInfos.add(microserviceAttrsInfo);
                for (Microservice m :
                        microservices) {
                    String name = m.getElementName();
                    Long codeLines = m.getCodeLines();
                    Long annotationLines = m.getAnnotationLines();
                    int pubTopicCount = m.getPubTopics().size();
                    int subTopicCount = m.getSubTopics().size();

                    ArcSmell arcSmell = arcSmellsInfo.find(name);
                    Long cyclic = arcSmell.getCyclic();
                    Long hublink = arcSmell.getHublink();
                    Long hublinkForIn = arcSmell.getHublinkForIn();
                    Long hublinkForOut = arcSmell.getHublinkForOut();

                    MainTain mainTain = map.get(name);
                    Long bugCount = mainTain.getBugCount();
                    Long commitCount = mainTain.getCommitCount();
                    Long commitAddLineCount = mainTain.getCommitAddLineCount();
                    Long commitDeleteLineCount = mainTain.getCommitDeleteLineCount();
                    Double commitOverlapRatio = mainTain.getCommitOverlapRatio();
                    Double commitFilesetOverlapRatio = mainTain.getCommitFilesetOverlapRatio();
                    Double pairwiseCommitterOverlap = mainTain.getPairwiseCommitterOverlap();

                    MicroserviceAttr ma = new MicroserviceAttr();
                    microserviceAttrsInfo.addMicroserviceAttr(ma);
                    ma.setMicroserviceName(name)
                            .setCodeLines(codeLines)
                            .setPubTopicCount(pubTopicCount)
                            .setSubTopicCount(subTopicCount)
                            .setCyclic(cyclic==null?0:cyclic)
                            .setHublink(hublink==null?0:hublink)
                            .setHublinkForIn(hublinkForIn==null?0:hublinkForIn)
                            .setHublinkForOut(hublinkForOut==null?0:hublinkForOut)
                            .setBugCount(bugCount)
                            .setCommitCount(commitCount)
                            .setCommitAddLineCount(commitAddLineCount)
                            .setCommitDeleteLineCount(commitDeleteLineCount)
                            .setCommitOverlapRatio(commitOverlapRatio)
                            .setCommitFilesetOverlapRatio(commitFilesetOverlapRatio)
                            .setPairwiseCommitterOverlap(pairwiseCommitterOverlap)
                    ;
                }
            }
        }
        return microserviceAttrsInfos;
    }
}

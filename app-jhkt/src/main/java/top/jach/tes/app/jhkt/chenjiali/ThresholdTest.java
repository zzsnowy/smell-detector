package top.jach.tes.app.jhkt.chenjiali;

import org.apache.commons.collections.ArrayStack;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.MissingObjectException;
import top.jach.tes.app.dev.DevApp;
import top.jach.tes.app.mock.Environment;
import top.jach.tes.app.mock.InfoTool;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.impl.domain.element.Element;
import top.jach.tes.core.impl.domain.element.ElementsValue;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.plugin.jhkt.DataAction;
import top.jach.tes.plugin.jhkt.InfoNameConstant;
import top.jach.tes.plugin.jhkt.Threshold.ThresholdResult;
import top.jach.tes.plugin.jhkt.arcsmell.ui.UiAction;
import top.jach.tes.plugin.jhkt.git.commit.GitCommitsForMicroserviceInfo;
import top.jach.tes.plugin.jhkt.microservice.Microservice;
import top.jach.tes.plugin.jhkt.microservice.MicroservicesInfo;
import top.jach.tes.plugin.jhkt.smellCallable.SloppyCallable;
import top.jach.tes.plugin.tes.code.git.commit.GitCommit;
import top.jach.tes.plugin.tes.code.git.version.Version;
import top.jach.tes.plugin.tes.code.git.version.VersionsInfo;
import top.jach.tes.plugin.tes.code.repo.ReposInfo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author:AdminChen
 * @date:2020/5/23
 * @description:
 */
//这边只负责供给各种不同的源数据，用线程池来并发计算AS，并拿到各个AS计算得到的结果
public class ThresholdTest extends DevApp {
    public static void main(String[] args) throws IOException {
        //System.out.println(testUiThreshold().size());
        List<Double> list=new ArrayList<>();
        list.add(3.0);list.add(7.0);list.add(8.0);list.add(13.0);list.add(4.0);list.add(7.0);list.add(5.0);list.add(9.0);
       //System.out.println(getThresholds(list));
        List<ThresholdResult> data=testUiThreshold(list);
        System.out.println(data.size());
        exportCSV(data,new File("D:\\data\\tes\\thresholdtest"));

        //test();//多个版本运行出来的uiAction计算结果要么是[]空的，要么是[11.0]只有一个数字的，
        // excel里是可以对应上哪个微服务是有值的，哪个微服务是没值的


    }

    public static void exportCSV(List<ThresholdResult> results,File dir) throws IOException {
        if(!dir.exists()){
            dir.mkdirs();
        }
        FileUtils.cleanDirectory(dir);
        File file = new File(dir.getAbsolutePath()+"/"+"thresholdtest.csv");
        StringBuilder sb = new StringBuilder();
        sb.append("version of source data");
        sb.append(",");
        sb.append("thresholds");
        sb.append(",");
        sb.append("result values");
        sb.append(",");
        sb.append("\n");
        for(ThresholdResult result:results){
            sb.append(result.getMicroservicesInfo().getVersion());
            sb.append(',');
            for(Double thrhd:result.getThrhds()){
                sb.append(thrhd);
                sb.append("/");
            }
            sb.append(',');
            Map<String,Double> values=result.getElementsValue().getValueMap();
            for(String key:values.keySet()){
                sb.append(values.get(key)+"  ");
            }
            sb.append(",");
            sb.append("\n");
        }
        FileUtils.write(file, sb.toString(), "utf8");
    }
    //测试ui检测结果是否正确
    public static void test(){
        //存放各个线程运行结果
        //List<ThresholdResult> resuts=new ArrayList<>();
        //新建线程池
       // ExecutorService executor=new ThreadPoolExecutor(5, 15, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        Context context = Environment.contextFactory.createContext(Environment.defaultProject);

        ReposInfo reposInfo = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.TargetSystem, ReposInfo.class);

        VersionsInfo versionsInfoForRelease = DataAction.queryLastInfo(context, InfoNameConstant.VersionsForRelease, VersionsInfo.class);

        for (int i = 0; i < versionsInfoForRelease.getVersions().size()-1; i++) {
            Version version = versionsInfoForRelease.getVersions().get(i);
            // 查询version版本下的所有微服务
            MicroservicesInfo microservices = DataAction.queryLastMicroservices(context, reposInfo.getId(), null, version);

            //存储单个版中所有微服务名称
            List<String> microserviceNames = microservices.microserviceNames();
            //获取pairrelationsInfo
            PairRelationsInfo pairRelationsInfoWithoutWeight = microservices.callRelationsInfoByTopic(false).deWeight();
            pairRelationsInfoWithoutWeight.setName(InfoNameConstant.MicroserviceExcludeSomeCallRelation);
            InfoTool.saveInputInfos(pairRelationsInfoWithoutWeight);

            //查询version版本下所有微服务的commit信息
            Map<String, GitCommitsForMicroserviceInfo> gitCommitsForMicroserviceInfoMap = new HashMap<>();
            List<GitCommit> gct = new ArrayList<>();
            for (Microservice microservice : microservices) {//由于MicroserviceInfo类实现了Iterator方法，因此可以这样遍历
                GitCommitsForMicroserviceInfo gitCommitsForMicroserviceInfo = DataAction.queryLastGitCommitsForMicroserviceInfo(context, reposInfo.getId(), microservice.getElementName(), version);
                gitCommitsForMicroserviceInfoMap.put(microservice.getElementName(), gitCommitsForMicroserviceInfo);
                if (gitCommitsForMicroserviceInfo == null) {
                    //System.out.println("GitCommitsForMicroserviceInfo  "+microservice.getElementName()+"  "+version.getVersionName());
                    continue;
                }
                gct.addAll(gitCommitsForMicroserviceInfo.getGitCommits());
            }
            //给gitCommits去重
            List<GitCommit> gitCommits = gct.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.getReposId() + "#" + o.getRepoName() + "#" + o.getSha()))), ArrayList::new));
            ;
            Collections.sort(gitCommits);
            ElementsValue value = UiAction.calculateUi(gitCommits, 5, microservices, pairRelationsInfoWithoutWeight, 5.0, 4.0, 8.0);
            System.out.println("version:");
            System.out.println(new ArrayList<Double>(value.getValueMap().values()).toString());
        }
    }
    //前端输入给定阈值范围，后台接收数据，自动生成给定范围内的阈值组合。目前用list代替前端输入
    //list从前往后每两个数组成一个for循环的范围
    public static List<List<Double>> getThresholds(List<Double> list){
        List<List<Double>> res=new ArrayList<>();
        //每个AS需要的阈值个数不一，只能对于每个需要阈值测试的AS分别写一个阈值测试类，据输入AS名不同调用对应的方法或类
        //ui有四个阈值，故4层for循环，但别的AS阈值不一样
        for(double i=list.get(0);i<list.get(1);i++){
            for(double j=list.get(2);j<list.get(3);j++){
                for(double k=list.get(4);k<list.get(5);k++){
                    for(double h=list.get(6);h<list.get(7);h++){
                        List<Double> sublist=new ArrayList<>();
                        sublist.add(i);
                        sublist.add(j);
                        sublist.add(k);
                        sublist.add(h);
                        res.add(sublist);
                    }
                }
            }
        }
        return res;
    }

    public static List<ThresholdResult> testUiThreshold(List<Double> list){
        //存放各个线程运行结果
        List<ThresholdResult> resuts=new ArrayList<>();
        //新建线程池
        ExecutorService executor=new ThreadPoolExecutor(5, 15, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        Context context = Environment.contextFactory.createContext(Environment.defaultProject);

        ReposInfo reposInfo = InfoTool.queryLastInfoByNameAndInfoClass(InfoNameConstant.TargetSystem, ReposInfo.class);

        VersionsInfo versionsInfoForRelease = DataAction.queryLastInfo(context, InfoNameConstant.VersionsForRelease, VersionsInfo.class);

        for (int i = 0; i < versionsInfoForRelease.getVersions().size()-1; i++) {
            Version version = versionsInfoForRelease.getVersions().get(i);
            // 查询version版本下的所有微服务
            MicroservicesInfo microservices = DataAction.queryLastMicroservices(context, reposInfo.getId(), null, version);

            //存储单个版中所有微服务名称
            List<String> microserviceNames = microservices.microserviceNames();
            //获取pairrelationsInfo
            PairRelationsInfo pairRelationsInfoWithoutWeight = microservices.callRelationsInfoByTopic(false).deWeight();
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
            //获得当前数据源下所有阈值的组合，每一个内嵌的list都是一组阈值
            //自动获取阈值组合改为前端输入各个阈值范围，通过getThresholds方法得出各2阈值组合
            List<List<Double>> thrs=new ArrayList<>(getThresholds(list));
            /*for(int ie=3;ie<7;ie++){
                for(int j=8;j<13;j++){
                    for(int k=4;k<7;k++){
                        for(int h=5;h<9;h++){
                            List<Double> list=new ArrayList<>();
                            list.add((double)ie);
                            list.add((double)j);
                            list.add((double)k);
                            list.add((double)h);
                            thrs.add(list);
                        }
                    }
                }
            }*/
            //根据多种阈值组合计算AS
            for(int l=0;l<thrs.size();l++){
                synchronized (thrs){
                    int len=(thrs.get(l).get(0)).intValue();
                    double impact=thrs.get(l).get(1);
                    double cochange=thrs.get(l).get(2);
                    double change=thrs.get(l).get(3);
                    int finalL = l;
                    executor.execute(() -> {
                        ElementsValue elementsValue=null;
                        elementsValue = UiAction.calculateUi(gitCommits,len,microservices,pairRelationsInfoWithoutWeight,impact,cochange,change);
                        if(elementsValue!=null){
                                synchronized (resuts){
                                    resuts.add(new ThresholdResult(microservices,pairRelationsInfoWithoutWeight,thrs.get(finalL),elementsValue));
                                }
                        }

                    });
                }
            }

        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return resuts;
    }

   public static List<ThresholdResult> isExpected(List<ThresholdResult> data){
        List<ThresholdResult> result=new ArrayList<>();
       //新建线程池
       ExecutorService executor=new ThreadPoolExecutor(5, 15, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        //这里也要用到并发判断，新写一个方法专门判断一个list<double>集合中的数据分布是否符合规律，然后在excutor中调用该方法
        for(ThresholdResult thrsult:data){
            List<Double> evalues=new ArrayList<>(thrsult.getElementsValue().getValueMap().values());
            executor.execute(() -> {
                if(judge(evalues)){
                    synchronized (result){
                        result.add(thrsult);
                    }
                }
            });

        }
        executor.shutdown();
        return result;

    }
//什么样的数据是我们想要的？
    public static boolean judge(List<Double> values){

        return false;

    }



}

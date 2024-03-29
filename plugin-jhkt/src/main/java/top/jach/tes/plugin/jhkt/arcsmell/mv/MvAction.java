package top.jach.tes.plugin.jhkt.arcsmell.mv;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import top.jach.tes.core.api.domain.action.Action;
import top.jach.tes.core.api.domain.action.DefaultOutputInfos;
import top.jach.tes.core.api.domain.action.InputInfos;
import top.jach.tes.core.api.domain.action.OutputInfos;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.api.domain.meta.Meta;
import top.jach.tes.core.api.exception.ActionExecuteFailedException;
import top.jach.tes.core.impl.domain.element.ElementsValue;
import top.jach.tes.plugin.jhkt.microservice.Microservice;
import top.jach.tes.plugin.jhkt.microservice.MicroservicesInfo;
import top.jach.tes.plugin.tes.code.git.commit.DiffFile;
import top.jach.tes.plugin.tes.code.git.commit.GitCommit;
import top.jach.tes.plugin.tes.code.git.commit.GitCommitsInfo;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static org.apache.commons.lang3.math.NumberUtils.max;
import static org.apache.commons.lang3.math.NumberUtils.min;

public class MvAction implements Action {
    public static final String MICROSERVICE_INFO = "MicroserviceInfo";
    public static final String GIT_COMMITS_INFO = "GitCommitsInfo";


    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDesc() {
        return null;
    }

    @Override
    public Meta getInputMeta() {
        return null;
    }

    //寻找多个滑动窗口中重复出现的提交文件及出现次数
    public Map<String,Map<String,Integer>> find(List<Set<String>> w_paths, List<String> microservices){
        Map<String,Map<String,Integer>> tran=new HashMap<>();//创建一个二维矩阵
        Map<String,Integer> col=new HashMap<>();
        for(Set<String> set:w_paths){
            for(String str:set){
                col.put(str,0);
            }
        }
        List<String> column=new ArrayList<>(new HashSet<>(col.keySet()));//所有文件路径名集合（非重复）
        for(Map.Entry<String, Integer> entry : col.entrySet()){
            tran.put(entry.getKey(),new HashMap<>());//二维矩阵每一行都对应一整个map集合
        }
        //按每个文件路径遍历每一个窗口下的集合
        for(String strs:column){//遍历每个文件路径
            String m = getMicroservicePathByPathname(strs, microservices);
            Map<String,Integer> tmp = tran.get(strs);
            for(int j=0;j<w_paths.size();j++){//遍历每个窗口
                Set<String> tlist = w_paths.get(j);
                if(tlist.contains(strs)){
                    for(String che:tlist){
                        if(che.startsWith(m)){
                            continue;
                        }
                        else{
                            Integer count = tmp.get(che);
                            if (count == null){
                                count = 0;
                            }
                            tmp.put(che,count+1);
                        }
                    }
                    //更新tran中原来strs对应的map的values集合
                    /*Map<String,Integer> check=new HashMap<>(new HashMap<>(tran.get(strs)));
                    for(Map.Entry<String, Integer> ent:tmp.entrySet()){
                        tmp.put(ent.getKey(),check.get(ent.getKey())+ent.getValue());
                    }*/
                }
            }
        }
        return tran;
    }
//根据文件名找到对应的微服务
    public static String getMicroserviceByPathname(String path,List<Microservice> microservices){
        String mname=null;
        for(Microservice microservice:microservices){
            String mPath=microservice.getAllPath();
//            String mPath=microservice.getPath();
            if(mPath==null){
                continue;
            }
            if(path.startsWith(mPath)){
                mname=microservice.getElementName();
                break;
            }
        }
        return mname;
    }
    //根据
    public static String getMicroservicePathByPathnameAndMs(String path,List<Microservice> microservices){
        String mname=null;
        for(Microservice microservice:microservices){
            String mPath=microservice.getAllPath();
//            String mPath=microservice.getPath();
            if(mPath==null){
                continue;
            }
            if(path.startsWith(mPath)){
                mname=mPath;
                break;
            }
        }
        return mname;
    }

    public static String getMicroserviceNameByFilePath(String path,List<Microservice> microservices){
        String mname=null;
        for(Microservice microservice:microservices){
            String mPath=microservice.getAllPath();
//            String mPath=microservice.getPath();
            if(mPath==null){
                continue;
            }
            if(path.startsWith(mPath)){
                mname=microservice.getElementName();
                break;
            }
        }
        return mname;
    }
    public static String getMicroservicePathByPathname(String path,List<String> microservices){
        String mname=null;
        for(String mPath:microservices){
//            String mPath=microservice.getPath();
            if(mPath==null){
                continue;
            }
            if(path.startsWith(mPath)){
                mname=mPath;
                break;
            }
        }
        return mname;
    }
    //根据filePath找到该文件对应的微服务，处在同一个微服务下的文件合并作为一个元素出现
    public static List<Set<String>> fileToMicroservice(List<Set<String>> paths,List<Microservice> microservices){
        List<Set<String>> micros=new ArrayList<>();
        for(Set<String> sets:paths){
            Set<String> tmpset=new HashSet<>();
            for(String str:sets){
                String tmp=getMicroserviceByPathname(str,microservices);
                if(tmpset.contains(tmp) || StringUtils.isBlank(tmp)){
                    continue;//若当前路径所在微服务在之前的遍历中已经出现过，则不重复添加
                }
                else{
                    tmpset.add(tmp);
                }
            }
            micros.add(tmpset);
        }
        return micros;
    }
    //返回微服务对应的共同变更的微服务集合及共同变更的次数的map,返回的微服务之间共同变更次数集合是没错的
    public static MvResult detectMvResultForUi(List<GitCommit> gitCommits,int len,List<Microservice> microservices){
        if (gitCommits == null || gitCommits.size() == 0 || len < 0) {
            return null;
        }
        //将获取的gitCommits对象集合按照提交时间先后排序（git记录的是秒级的提交时间戳）
        //Collections.sort(gitCommits);//在最初数据获取时已经排好序了，故此处不排序

        /*对于每个提交，若其中5个文件都属于同一个微服务，则认为该微服务提交了一次，而非5次*/

        //生成从提交信息中提取出文件集合，其内的set集合记录每一个gitcommits包含的提交文件名集合
        List<Set<String>> blocks = generateFileSetBlocks(gitCommits, microservices);
        //获取每个文件提交的次数
        Map<String, Integer> fileCount = findFileCommitCount(blocks);
        /*最终要得到的结果，string代表文件名，Map代表与该文件存在共同变更的文件名和共同变更次数这里可以通过getMicroservicePathByPathnameAndMs
        方法直接得到任意文件对应的微服务路径那么resultFiles最终存的是微服务之间的共同变更统计记录*/
        Map<String,Map<String,Integer>> resultMicros = new HashMap<>();
        //每一次循环对应某一次提交的文件集合set
        for (int i = 0; i < blocks.size(); i++) {
            int start = max(0, i-len), end = min(i+len+1,blocks.size());
            Set<String> source = blocks.get(i);//某一次提交的文件集合
            List<Set<String>> targets = blocks.subList(start, end);//提交窗口，对应第start到end次提交的文件集合
            for (String file :
                    source) {//遍历某一次提交的文件集合内的文件
                //当前文件名所在的微服务的路径
                String prefix = getMicroservicePathByPathnameAndMs(file, microservices);//微服务路径
                String miname=getMicroserviceNameByFilePath(file,microservices);//微服务名称
                Set<String> appearMicros = new HashSet<>();//当前某文件对应的和它共同变更的微服务集合
                for (Set<String> tfiles :
                        targets) {//遍历提交窗口内每一次提交
                    for (String tfile :
                            tfiles) {//遍历每次提交包含的文件
                        if(!tfile.startsWith(prefix)) {//窗口范围内提交的某文件与当前文件不属于同一个微服务，说明存在 隐形依赖
                            //appearFiles.add(tfile);改为appearFiles内存入文件对应的微服务路径
                            //是否要判断一下
                            appearMicros.add(getMicroserviceNameByFilePath(tfile,microservices));
                        }
                    }
                }
                //根据file作为key获取resultFiles的value，若file对应的value为空，则new一个map存入resultFiles并返回给countMap
                //相当于是在给resultFiles不断根据传入的prefix值（对应key），给key赋值，并创建该key对应的value值，for循环再给value赋值
                //上面的for是用于计算在某一个block里某个文件与其周边提交文件隐形依赖，对于一个Block而言，不计算重复影响的次数（因为是set），
                // 而对于不同block而言，会计算在不同block里的文件/微服务影响其他的同一个文件/微服务的次数
                Map<String, Integer> countMap = resultMicros.computeIfAbsent(miname, k -> new HashMap<>());
                for (String appearFile :
                        appearMicros) {
                    //对于countMap中key为appearFile的value来说，若其对应的value为空，则给value赋0
                    Integer count = countMap.computeIfAbsent(appearFile, k -> 0);
                    countMap.put(appearFile, count+1);//当前只是某一次提交的某一个文件对应的共同变更值，file肯定会有重复
                }
            }
        }

        return new MvResult(len, resultMicros, fileCount, microservices);
    }
    public static void getLevel3Result(List<GitCommit> gitCommits, List<Set<String>> blocks,Map<String,Map<String,Integer>> resultFiles){
        for(String file:resultFiles.keySet()){//遍历所有提交的所有file
            for(String mvfile:resultFiles.get(file).keySet()){
                for(int i=0;i<blocks.size();i++){
                    if(blocks.get(i).contains(mvfile)){

                    }
                }
            }
        }
    }

    public static MvCommitInfo detectMvCommitForFiles(List<GitCommit> gitCommits,int len,List<Microservice> microservices){
        if (gitCommits == null || gitCommits.size() == 0 || len < 0) {
            return null;
        }
        //将获取的gitCommits对象集合按照提交时间先后排序（git记录的是秒级的提交时间戳）
        Collections.sort(gitCommits);

        /*对于每个提交，若其中5个文件都属于同一个微服务，则认为该微服务提交了一次，而非5次*/

        //生成从提交信息中提取出文件集合
        List<Set<String>> blocks = generateFileSetBlocks(gitCommits, microservices);
        //获取每个文件提交的次数
        //Map<String, Integer> fileCount = findFileCommitCount(blocks);

        //获取每个文件所有提交信息（每个文件分别在哪些次commit里提交了）
        //Map<String,Set<GitCommit>> fileCommitMap=generateFileCommitsMap(gitCommits,blocks,new ArrayList<>(fileCount.keySet()));

        /*最终要得到的结果，string代表文件名，Map代表与该文件存在共同变更的文件名和共同变更次数
        //这里可以通过getMicroservicePathByPathnameAndMs方法直接得到任意文件对应的微服务路径
        //（微服务名可以唯一标识一个微服务，微服务路径也可以）那么resultFiles最终存的是微服务之间的共同变更统计记录*/
       // Map<String,Map<String,Integer>> resultFiles = new HashMap<>();
        Map<String,Map<MvCommit,Integer>> resultCommits=new HashMap<>();//每个文件+当前所在提交
        for (int i = 0; i < blocks.size(); i++) {
            int start = max(0, i-len), end = min(i+len+1,blocks.size());
            Set<String> source = blocks.get(i);
            List<Set<String>> targets = blocks.subList(start, end);//当前提交相邻多远距离内视为临近提交
            for (String file :
                    source) {//遍历当前block内所有文件,文件所在gitcommits位置为i位置
                //微服务的路径
                String prefix = getMicroservicePathByPathnameAndMs(file, microservices);
                //Set<String> appearFiles = new HashSet<>();
                Set<MvCommit> appearCommits=new HashSet<>();
                //遍历相邻提交范围内所有blocks内的所有文件
                for (int j=0;j<targets.size();j++) {//在相邻距离内的第j个block
                    Set<String> tfiles=targets.get(j);
                    for (String tfile :
                            tfiles) {
                        if(!tfile.startsWith(prefix)) {//只要相邻范围内任何文件是和当前file不同微服务的，就认为该文件与当前文件是共同提交的
                            //appearFiles.add(tfile);
                            //file与当前的tfile是共同提交的一组文件，且分别对应List<GitCommits>的第i和第j+start个提交
                            appearCommits.add(new MvCommit(file,i,tfile,j));
                        }
                    }
                }
                Map<MvCommit,Integer> commitCount=resultCommits.computeIfAbsent(file,k ->new HashMap<>());
                for(MvCommit mvCommit:appearCommits){
                    Integer count=commitCount.computeIfAbsent(mvCommit,k ->0);
                    commitCount.put(mvCommit,count+1);
                }
                /*Map<String, Integer> countMap = resultFiles.computeIfAbsent(file, k -> new HashMap<>());
                for (String appearFile :
                        appearFiles) {
                    Integer count = countMap.computeIfAbsent(appearFile, k -> 0);
                    countMap.put(appearFile, count+1);
                }*/
            }
        }

        return new MvCommitInfo(gitCommits,resultCommits);
    }


    public static MvResult detectMvResult(List<GitCommit> gitCommits,int len,List<Microservice> microservices){
        if (gitCommits == null || gitCommits.size() == 0 || len < 0) {
            return null;
        }
        //将获取的gitCommits对象集合按照提交时间先后排序（git记录的是秒级的提交时间戳）
        Collections.sort(gitCommits);

        /*对于每个提交，若其中5个文件都属于同一个微服务，则认为该微服务提交了一次，而非5次*/

        //生成从提交信息中提取出文件集合
        List<Set<String>> blocks = generateFileSetBlocks(gitCommits, microservices);
        //获取每个文件提交的次数
        Map<String, Integer> fileCount = findFileCommitCount(blocks);
        // 初始化一个len长度的滑动窗口
//        SlidingWindow slidingWindow = generateSlidingWindow(len, blocks);
//        Map<String,Map<String,Integer>> resultFiles = slidingWindow.slideBlocks(blocks, path -> getMicroservicePathByPathnameAndMs(path, microservices));

        /*最终要得到的结果，string代表文件名，Map代表与该文件存在共同变更的文件名和共同变更次数
        //这里可以通过getMicroservicePathByPathnameAndMs方法直接得到任意文件对应的微服务路径
        //（微服务名可以唯一标识一个微服务，微服务路径也可以）那么resultFiles最终存的是微服务之间的共同变更统计记录*/
        Map<String,Map<String,Integer>> resultFiles = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            int start = max(0, i-len), end = min(i+len+1,blocks.size());
            Set<String> source = blocks.get(i);
            List<Set<String>> targets = blocks.subList(start, end);//当前提交相邻多远距离内视为临近提交
            for (String file :
                    source) {//遍历当前block内所有文件,文件所在gitcommits位置为i位置
                //微服务的路径
                String prefix = getMicroservicePathByPathnameAndMs(file, microservices);
                Set<String> appearFiles = new HashSet<>();
                //遍历相邻提交范围内所有blocks内的所有文件
                for (Set<String> tfiles :
                        targets) {
                    for (String tfile :
                            tfiles) {
                        if(!tfile.startsWith(prefix)) {//只要相邻范围内任何文件是和当前file不同微服务的，就认为该文件与当前文件是共同提交的
                            appearFiles.add(tfile);
                        }
                    }
                }
                Map<String, Integer> countMap = resultFiles.computeIfAbsent(file, k -> new HashMap<>());
                for (String appearFile :
                        appearFiles) {
                    Integer count = countMap.computeIfAbsent(appearFile, k -> 0);
                    countMap.put(appearFile, count+1);
                }
            }
        }

        return new MvResult(len, resultFiles, fileCount, microservices);
    }
    //检测算法,目前返回的是null,可返回的数据是一个map矩阵，记录每个文件与其他文件在不同窗口一起出现的次数
    public static ElementsValue detect(List<GitCommit> gitCommits,int len, int minCommitCount, double minPer,List<Microservice> microservices){
        if (gitCommits == null || gitCommits.size() == 0 || len < 1) {
            return null;
        }
        MvResult  mvResult = detectMvResult(gitCommits, len, microservices);
        Map<String, MvValue> mmvs = mvResult.calculateMvValues(minCommitCount, minPer);
        Map<String, Double> ms = new HashMap<>();
        for (Map.Entry<String,MvValue> entry:
            mmvs.entrySet()){
            ms.put(entry.getKey(), Double.valueOf(entry.getValue().getFile()));
        }
        ElementsValue elmentMv=ElementsValue.createInfo();
        elmentMv.setValueMap(ms);
        return elmentMv;
    }


    private static SlidingWindow generateSlidingWindow(int len, Queue<Set<String>> blocks) {
        List<Set<String>> bs = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            bs.add(blocks.poll());
        }
        return new SlidingWindow(bs);
    }

    private static Map<String, Integer> findFileCommitCount(Iterable<Set<String>> blocks) {
        Map<String, Integer> fileCount = new HashMap<>();
        for (Set<String> block :
                blocks) {
            for (String file :
                    block) {
                Integer c = fileCount.get(file);
                if (c == null){
                    c = 0;
                }
                fileCount.put(file, c+1);
            }
        }
        return fileCount;
    }

    //得到每个微服务提交了哪些文件
    public static Map<String,Set<String>> findMicroFiles(List<GitCommit> gitCommits, List<Microservice> microservices){
        if (gitCommits == null || gitCommits.size() == 0) {
            return null;
        }
        //将获取的gitCommits对象集合按照提交时间先后排序（git记录的是秒级的提交时间戳）
        Collections.sort(gitCommits);

        /*对于每个提交，若其中5个文件都属于同一个微服务，则认为该微服务提交了一次，而非5次*/

        //生成从提交信息中提取出文件集合
        List<Set<String>> blocks = generateFileSetBlocks(gitCommits, microservices);


        Map<String,Set<String>> res=new HashMap<>();
        for(Microservice microservice:microservices){
            String micr=microservice.getElementName();
            Set<String> files=new HashSet<>();
            for(Set<String> block:blocks){
                for(String file:block){
                    if(getMicroserviceNameByFilePath(file,microservices).equals(micr)){
                        files.add(file);
                    }
                }
            }
            res.put(micr,files);
        }
        return res;
    }

    //记录每个文件在分别在哪些commit里提交过，即该文件的所有变更记录
    private static Map<String,Set<GitCommit>> generateFileCommitsMap(List<GitCommit> gitCommits,List<Set<String>> blocks,List<String> files){
        Map<String,Set<GitCommit>> fileCommitMap=new HashMap<>();
        for(String file:files){
            Set<GitCommit> tmp=new HashSet<>();
            for(int i=0;i<blocks.size();i++){
                if(blocks.get(i).contains(file)){//说明该文件在这次commit里提交过;
                    if(!fileCommitMap.containsKey(file)){
                        fileCommitMap.put(file,tmp);
                    }
                    tmp.add(gitCommits.get(i));
                    fileCommitMap.put(file,tmp);
                }

            }
        }
        return fileCommitMap;
    }


    private static List<Set<String>>  generateFileSetBlocks(List<GitCommit> gitCommits, List<Microservice> microservices) {
        List<Set<String>> blocks=new ArrayList<>();
        for(GitCommit gc:gitCommits){
            Set<String> block=new HashSet<>();
            for(DiffFile df:gc.getDiffFiles()){
                for (String p :
                        df.getFilePath()) {
                    String m = getMicroserviceByPathname(gc.getRepoName()+"/"+p,microservices);
                    if (m!=null){
                        block.add(gc.getRepoName()+"/"+p);
                    }
                }
            }
            if(block.size()>0) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    @Override
    public OutputInfos execute(InputInfos inputInfos, Context context) throws ActionExecuteFailedException {
       /* GitCommitsInfo gitCommitsInfo=inputInfos.getInfo(GIT_COMMITS_INFO,GitCommitsInfo.class);
        MicroservicesInfo microservicesInfo=inputInfos.getInfo(MICROSERVICE_INFO,MicroservicesInfo.class);
        List<Microservice> microservices=Lists.newArrayList(microservicesInfo.getMicroservices().iterator());*/

        //计算出各个微服务同时在一个窗口下出现次数,返回所有微服务满足该AS的次数
        //参数6表示设置6个gitCommits组成一个窗口
        //ElementsValue elementMv=detect(gitCommitsInfo,6,microservices);
        return null;
       // return DefaultOutputInfos.WithSaveFlag(elementMv);
    }
}

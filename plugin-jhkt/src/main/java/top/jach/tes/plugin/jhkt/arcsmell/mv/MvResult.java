package top.jach.tes.plugin.jhkt.arcsmell.mv;

import org.apache.commons.lang3.StringUtils;
import top.jach.tes.core.impl.domain.element.ElementsValue;
import top.jach.tes.plugin.jhkt.microservice.Microservice;

import java.io.*;
import java.sql.SQLSyntaxErrorException;
import java.util.*;

import static top.jach.tes.plugin.jhkt.arcsmell.mv.MvAction.getMicroserviceByPathname;

public class MvResult {
    private Integer param;
    private Map<String, MvResultForMicroservice> results = new HashMap<>();//每个微服务string及与该微服务中的文件存在mv的文件+存在mv的次数的map
    private Map<String, Integer> fileCommitCount;
    private List<Microservice> microservices;
    private Map<String,Map<String,Integer>> resultFiles;//每个文件string及与该文件存在mv的文件+存在mv的次数的map
    //private Map<String,Map<MvCommit,Integer>> resultCommits;//每个文件String及与该文件存在mv的文件+二者存在mv的详细信息+二者存在mv的同时存在于同一个commit的次数

    public MvResult(Integer param, Map<String,Map<String,Integer>> resultFiles, Map<String, Integer> fileCommitCount, List<Microservice> microservices) {
        for (Map.Entry<String,Map<String, Integer>> entry:
                resultFiles.entrySet()) {
            String file = entry.getKey();
            String microservice = getMicroserviceByPathname(file, microservices);
            MvResultForMicroservice mvResultForMicroservice = results.get(microservice);
            if (mvResultForMicroservice==null){
                mvResultForMicroservice = new MvResultForMicroservice();
                results.put(microservice, mvResultForMicroservice);
            }
            mvResultForMicroservice.add(file, entry.getValue());
        }
        this.param = param;
        this.resultFiles = resultFiles;
        this.fileCommitCount = fileCommitCount;
        this.microservices = microservices;
       // this.resultCommits=resultCommits;
    }

    public static class MvResultForMicroservice{
        private Map<String, Map<String, Integer>> fileToFile = new HashMap<>();
        public MvResultForMicroservice() {
        }
        public void add(String file, Map<String, Integer> files){
            fileToFile.put(file, files);
        }

        public Map<String, Map<String, Integer>> getFileToFile() {
            return fileToFile;
        }
    }

    public Map<String, MvResultForMicroservice> getResults() {
        return results;
    }

    public Map<String, Integer> getFileCommitCount() {
        return fileCommitCount;
    }
//新的mv算法
    public Map<String,MvValue> calculateMvValues(int minCommitCount,int minCoCommit,double minPer) {
        List<MvDetail> mvDetailList=new ArrayList<>();//记录微服务之间存在的每一对mv对应的文件对
       //mv除了file数据，也有微服务数据，mvMicros就是微服务层面的数据！！！
        Map<String,List<String>> mvMicros=new HashMap<>();//每个微服务中与该微服务存在mv的微服务
        Map<String, Double> mDepens = new HashMap<>();//计算每个微服务在mv的dependency属性上的值
        Map<String, Double> mDoubleDepens = new HashMap<>();
        Map<String, Set<String>> mfiles = new HashMap<>();//计算每个微服务在mv的file属性上的值
        Map<String, Set<String>> mDoublefiles = new HashMap<>();
        Map<String, MvValue> mmvs = new HashMap<>();
        for (Microservice m :
                microservices) {
            mDepens.put(m.getElementName(), 0d);
            mDoubleDepens.put(m.getElementName(), 0d);
            mfiles.put(m.getElementName(), new HashSet<>());
            mDoublefiles.put(m.getElementName(), new HashSet<>());
            mvMicros.put(m.getElementName(),new ArrayList<>());
            //microFileCouple.put(m.getElementName(),new HashMap<>());
        }
        for (Map.Entry<String,Map<String, Integer>> entry:
                resultFiles.entrySet()){
            String file = entry.getKey();
            Double fcc = Double.valueOf(fileCommitCount.get(file));//这个文件一共提交了多少次
            if(fcc<minCommitCount){
                continue;
            }
            for (Map.Entry<String, Integer> entry2 :
                    entry.getValue().entrySet()) {
                String tfile = entry2.getKey();
                Integer count = entry2.getValue();//某文件与这个文件发生隐性依赖的次数
                Double tfcc=Double.valueOf(fileCommitCount.get(tfile));
                String m = getMicroserviceByPathname(file, microservices);
                String tm = getMicroserviceByPathname(tfile, microservices);
                if(m.equals(tm) || StringUtils.isBlank(m) || StringUtils.isBlank(tm)||count<minCoCommit){
                    continue;
                }
                if(file.contains("x_6d")||file.contains("x_81")||tfile.contains("x_6d")||tfile.contains("x_81")){
                    continue;
                }
//                Double tfcc = Double.valueOf(fileCommitCount.get(tfile));
                if(count/fcc >= minPer&&count/tfcc>=minPer){//二者均大于minPer才认为是共同提交

                    Double mc = mDepens.get(m);
                    mvMicros.get(m).add(tm);
                    mDepens.put(m, mc+1);
                    mvMicros.get(tm).add(m);
                    Double tmc = mDepens.get(tm);
                    mDepens.put(tm, tmc+1);

                    mfiles.get(m).add(file);
                    mfiles.get(tm).add(tfile);

                    mvDetailList.add(new MvDetail(m,tm,file,tfile));


                   /* microFileCouple.get(m).put(file,tfile);//<微服务，<file,tfile>>
                    microFileCouple.get(tm).put(tfile,file);*/

                    /*在这里每次拿到一对文件对，就把这个文件对存入当前遍历的两个微服务名下，
                    每一条微服务之间的mv依赖都对应一对存在mv依赖的文件对，
                    我们需要得到每个微服务下的正确的文件对*/

                    /*if(count/tfcc >= minPer){
                        mc = mDoubleDepens.get(m);
                        mDoubleDepens.put(m, mc+1);
                        tmc = mDoubleDepens.get(tm);
                        mDoubleDepens.put(tm, tmc+1);

                        mDoublefiles.get(m).add(file);
                    }*/
                }
            }
        }
         //System.out.println(mvDetailList.size());
        //输出mvDetails内容

        for (Microservice m :
                microservices) {
            String mn = m.getElementName();
            MvValue mvValue = new MvValue();
            List<MvDetail> tmplist=new ArrayList<>();
            for(MvDetail md:mvDetailList){
                if(mn.equals(md.sourceMicro)||mn.equals(md.targetMicro)){
                    tmplist.add(md);
                }
            }
            mvValue.setMvDetailList(tmplist);
            mvValue.setDependency(mDepens.get(mn));
            mvValue.setDoubleDependency(mDoubleDepens.get(mn));
            mvValue.setFile(mfiles.get(mn).size());
            mvValue.setDoubleFile(mDoublefiles.get(mn).size());
            mmvs.put(m.getElementName(), mvValue);
            if(mfiles.get(mn).size()==0&&mDepens.get(mn)>0){
                System.out.println("ssssssssssssssssssssssss");
            }
        }
        //outputDetails(mvDetailList);
        return mmvs;
    }

/*    //输出mvdetails
    public void outputDetails(List<MvDetail> detailList) throws IOException {
        Workbook workbook=new HSSFWorkbook();
        Sheet sheet=workbook.createSheet("version1");
        Row row=sheet.createRow(0);
        row.createCell(0).setCellValue("sourceMicro");
        row.createCell(1).setCellValue("targetMicro");
        row.createCell(2).setCellValue("sourceFile");
        row.createCell(3).setCellValue("targetFile");
        int rr=1;
        for(MvDetail md:detailList){
            Row rrr=sheet.createRow(rr);
            rrr.createCell(0).setCellValue(md.sourceMicro);
            rrr.createCell(1).setCellValue(md.targetMicro);
            rrr.createCell(2).setCellValue(md.sourceFile);
            rrr.createCell(3).setCellValue(md.targetFile);
            rr++;
        }
        File file=new File("D:\\data\\versions5\\mvDetails.xls");
        //File file=new File("D:\\data\\versions5\\udWithWeight.xls");
        OutputStream outputStream=new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();
    }*/

    public Map<String, MvValue> calculateMvValues(int minCommitCount, double minPer) {
        Map<String, Double> mDepens = new HashMap<>();//计算每个微服务在mv的dependency属性上的值
        Map<String, Double> mDoubleDepens = new HashMap<>();
        Map<String, Set<String>> mfiles = new HashMap<>();//计算每个微服务在mv的file属性上的值
        Map<String, Set<String>> mDoublefiles = new HashMap<>();
        Map<String, MvValue> mmvs = new HashMap<>();
        for (Microservice m :
                microservices) {
            mDepens.put(m.getElementName(), 0d);
            mDoubleDepens.put(m.getElementName(), 0d);
            mfiles.put(m.getElementName(), new HashSet<>());
            mDoublefiles.put(m.getElementName(), new HashSet<>());
        }
        for (Map.Entry<String,Map<String, Integer>> entry:
                resultFiles.entrySet()){
            String file = entry.getKey();
            Double fcc = Double.valueOf(fileCommitCount.get(file));//这个文件一共提交了多少次
            if(fcc<minCommitCount){
                continue;
            }
            for (Map.Entry<String, Integer> entry2 :
                    entry.getValue().entrySet()) {
                String tfile = entry2.getKey();
                Integer count = entry2.getValue();//某文件与这个文件发生隐性依赖的次数
                String m = getMicroserviceByPathname(file, microservices);
                String tm = getMicroserviceByPathname(tfile, microservices);
                if(m.equals(tm) || StringUtils.isBlank(m) || StringUtils.isBlank(tm)){
                    continue;
                }
//                Double tfcc = Double.valueOf(fileCommitCount.get(tfile));
                if(count/fcc >= minPer){
                    Double mc = mDepens.get(m);
                    mDepens.put(m, mc+1);
                    Double tmc = mDepens.get(tm);
                    mDepens.put(tm, tmc+1);

                    mfiles.get(m).add(file);
                    mfiles.get(tm).add(tfile);

                    /*if(count/tfcc >= minPer){
                        mc = mDoubleDepens.get(m);
                        mDoubleDepens.put(m, mc+1);
                        tmc = mDoubleDepens.get(tm);
                        mDoubleDepens.put(tm, tmc+1);

                        mDoublefiles.get(m).add(file);
                    }*/
                }
            }
        }

        for (Microservice m :
                microservices) {
            String mn = m.getElementName();
            MvValue mvValue = new MvValue();
            mvValue.setDependency(mDepens.get(mn));
            mvValue.setDoubleDependency(mDoubleDepens.get(mn));
            mvValue.setFile(mfiles.get(mn).size());
            mvValue.setDoubleFile(mDoublefiles.get(mn).size());
            mmvs.put(m.getElementName(), mvValue);
        }
        return mmvs;
    }

    public List<Microservice> getMicroservices() {
        return microservices;
    }

    public Map<String, Map<String, Integer>> getResultFiles() {
        return resultFiles;
    }
}

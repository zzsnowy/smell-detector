/*
package top.jach.tes.plugin.jhkt.arcsmell.concernOverload;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.*;
import java.util.*;

*/
/**
 * @author:AdminChen
 * @date:2020/11/13
 * @description:
 *//*

public class COAction {

    public static void main(String[] args) throws IOException{
        List<String> versions = Arrays.asList("x_3c9_x_95d.x_893.x_893.x_e09d_x_43_x_8b_x_e09f_x_e0a1",
                "x_1635-x_95d.x_893.x_893_x_e0a3_x_1ff_x_e0a5_x_e0a7",
                "x_1635-x_95d.x_893.x_935_x_1ff_x_e0a9_x_29229",
                "x_1635-x_95d.x_4af.x_893.x_ec8b_x_1ff_x_2922d",
                "x_1635-x_95d.x_4af.x_893_x_1ff_x_e0af_x_e0a3_x_e0b1");
        HSSFWorkbook wb=new HSSFWorkbook();
        Gson gson=new Gson();
        String mm= FileUtils.readFileToString(new File("D:\\tdata\\microMap.json"),"utf8");
        Map<String,String> microMap=gson.fromJson(mm,JsonObjectForMM.class).getMicroMap();
        for (String versionName : versions) {
            //直接对所有微服务所有相关topic影响权重值求四分位+1.5iqr,得到0.08668这个值，因此影响权重值大于这个值的topic都认为是当前微服务相关的topic
            JsonObjectForMT1 micro_topics=postProcess("D:\\tdata\\","D:\\tdata\\microPaths\\"+versionName+".xls",versionName,"D:\\tdata\\microFiles.json",0.08668);
            //topicCount根据value值排序
            Map<String,Integer> map1=micro_topics.microtopicCount;
            List<Map.Entry<String,Integer>> list1=new ArrayList<>(map1.entrySet());
            Collections.sort(list1, new Comparator<Map.Entry<String, Integer>>() {
                @Override
                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                    return o2.getValue().compareTo(o1.getValue());//降序排列
                }
            });

            Sheet sheet=wb.createSheet(versionName);
            Row row=sheet.createRow(0);
            row.createCell(0).setCellValue("microservice");
            row.createCell(1).setCellValue("topicsCount");
            row.createCell(2).setCellValue("topicName");
            row.createCell(3).setCellValue("topicWeight");
            int i=1;
            for(Map.Entry<String,Integer> mapping:list1){
                String micro=mapping.getKey();//包含topic数最多的微服务名
                Row rw=sheet.createRow(i);
                rw.createCell(0).setCellValue(microMap.get(micro));
                rw.createCell(1).setCellValue(mapping.getValue());
                i++;
                //int i=2;//后面详细topic从该行开始打印
                Map<String,Double> map2=micro_topics.microTopics.get(micro);//当前遍历到的微服务下所有topic的详细情况
                List<Map.Entry<String,Double>> list2=new ArrayList<>(map2.entrySet());
                Collections.sort(list2, new Comparator<Map.Entry<String, Double>>() {
                    @Override
                    public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                        return o2.getValue().compareTo(o1.getValue());
                    }
                });
                for(Map.Entry<String,Double> mapp:list2){
                    Row r=sheet.createRow(i);
                    r.createCell(2).setCellValue(mapp.getKey());
                    r.createCell(3).setCellValue(mapp.getValue());
                    i++;
                }
            }

        }
        FileOutputStream otp=new FileOutputStream("D:\\tdata\\microTopicRatio.xls");
        wb.write(otp);
        otp.flush();
    }

    //计算一个list的异常值范围
    public static double[] getfiqr(List<Double> list){
        Double[] arrtmp=list.toArray(new Double[list.size()]);
        double[] arr=new double[arrtmp.length];
        for(int i=0;i<arrtmp.length;i++){
            arr[i]=arrtmp[i];
        }
        double[] arr1 = new double[4];
        // 长度小于4时,补齐arr数组至长度四位
        if (arr.length < 4) {
            for (int i = 0; i < arr.length; i++) {
                arr1[i] = arr[i];
            }
            for (int k = arr.length; k < 4; k++) {
                arr1[k] = arr1[k];
            }
            return arr1;
        }

        double[] tempArr = Arrays.copyOf(arr, arr.length);
        Arrays.sort(tempArr);
        double[] quartiles = new double[3];
        double[] yichang=new double[2];
        int n = arr.length;
        double Q1 = (n+1) * 0.25D;
        double Q2 = (n+1) * 0.5D;
        double Q3 = (n+1) * 0.75D;
        //Q1
        if(Q1 % 2 == 0){
            quartiles[0] = tempArr[(int)Q1];
        }else{
            double Q1y = Q1-Math.floor(Q1);
            double Q1r;
            Q1r = (1D - Q1y) * tempArr[(int) Math.floor(Q1)-1] + Q1y * tempArr[(int) Math.ceil(Q1)-1];
            quartiles[0] = Q1r;
        }
        //Q2
        if(Q2 % 2 == 0){
            quartiles[1] = tempArr[(int)Q2];
        }else{
            double Q2y = Q2-Math.floor(Q2);
            double Q2r;
            Q2r = (1D - Q2y) * tempArr[(int) Math.floor(Q2)-1] + Q2y * tempArr[(int) Math.ceil(Q2)-1];
            quartiles[1] = Q2r;
        }
        //Q3
        if(Q3 % 2 == 0){
            quartiles[2] = tempArr[(int)Q3];
        }else{
            double Q3y = Q3-Math.floor(Q3);
            double Q3r;
            Q3r = (1D - Q3y) * tempArr[(int) Math.floor(Q3)-1] + Q3y * tempArr[(int) Math.ceil(Q3)-1];
            quartiles[2] = Q3r;
        }
        double iqr=quartiles[2]-quartiles[0];
        yichang[0]=quartiles[0]-1.5*iqr;
        yichang[1]=quartiles[2]+1.5*iqr;
        return yichang;
    }

    public static Map<String,List<String>> getFileTopics(List<String> filesequence,double[][] theta,double[] upAndDown){
        Map<String,List<String>> res=new HashMap<>();
        for(int i=0;i<filesequence.size();i++){
            String file=filesequence.get(i);
            List<String> topics=new ArrayList<>();
            for(int j=0;j<theta[0].length;j++){
                if(theta[i][j]<upAndDown[0]||theta[i][j]>upAndDown[1]){
                    topics.add("topic"+j+"th");
                }
            }
            res.put(file,topics);
        }
        return res;
    }

    public static List<String> getMs(String msPath) throws IOException{
        List<String> paths=new ArrayList<>();
        InputStream is=new FileInputStream((msPath));
        Workbook wb=Workbook.getWorkbook(is);
        Sheet sheet=wb.getSheet(0);//拿到第一行
        int rows=sheet.getRows();
        for(int i=0;i<rows;i++){
            String path=sheet.getCell(0,i).getContents().trim();
            paths.add(path.replace("/","\\"));//由于读取得到的fileSequence是D:\sss\aaa这种形式，故也要转变成单个右斜杠\形式，而非双右斜杠\\
        }
        return paths;
    }

    //根据传入的微服务路径截取尾端作为微服务名
    public static String getMsName(String mpath){
        String[] msArr=mpath.split("\\\\");
        String ms="";
        if(!"domain".equals(msArr[msArr.length-2])){
            ms=msArr[msArr.length-2]+"_"+msArr[msArr.length-1];
        }else{
            ms=msArr[msArr.length-1];
        }
        return ms;
    }
    */
/*
        //把模型训练得到的结果封装成TrainModel类，类里包含K,M,theta[][]这三个数据
        public static void outputTrain(String path,String versionName) throws IOException {
            JsonObjectForTrainModel tmodel=new JsonObjectForTrainModel();
            LDAOption option = new LDAOption();

            option.est = false;
            option.inf = true;
            option.modelName = "model-final";
            option.niters = 1200;
            option.dir ="D:\\5gCore\\topicTrainingData\\"+versionName+"\\";

            Inferencer inferencer = new Inferencer();

            inferencer.init(option);

            Model model = inferencer.trnModel;
            tmodel.setK(model.K);
            tmodel.setM(model.M);
            tmodel.setV(model.V);
            tmodel.setTheta(model.theta);
            tmodel.setPhi(model.phi);

            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("D:\\tdata\\"+versionName+"\\traindata.json"),"UTF-8");
            Tool tool=new Tool();
            Gson gson=new Gson();
            String jso=gson.toJson(tmodel);
            String formatJson=tool.stringToJSON(jso);//格式化
            osw.write(formatJson);
            osw.flush();
            osw.close();

        }*//*

    //所需数据：所有微服务路径集合mpath；所有微服务下包含的文件数量mfpath；每个版本K,M,theta[][]，flist数据，这是lda训练得到的数据
    public static JsonObjectForMT1 postProcess(String trainPath,String mpath,String versionName,String mfpath,double yuzhi) throws IOException, BiffException {
        Gson gson=new Gson();
        String td= FileUtils.readFileToString(new File(trainPath+versionName+"\\traindata.json"),"utf8");
        JsonObjectForTrainModel model=gson.fromJson(td,JsonObjectForTrainModel.class);//读入已封装好的lda模型训练完成的数据

        List<Double> mk=new ArrayList<>();//存储theta矩阵中所有数字，作为求出四分位值+1.5iqr的输入
        for(int i=0;i<model.M;i++){
            Double[] tmp=new Double[model.K];
            for(int j=0;j<model.K;j++){
                tmp[j]=model.theta[i][j];
            }
            mk.addAll(Arrays.asList(tmp));//由于List只存储包装类的元素，因此要把double转换成Double才能用asList方法
        }
        double[] upAndDown=getfiqr(mk);

        //存储第一阶段的所有文件路径集合，这就是当前微服务下所有文件的集合
        List<String> filesequence = new ArrayList<>();
        try {
            FileInputStream is = new FileInputStream(trainPath+versionName+"\\files.flist");
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = "";
            while ((line = br.readLine()) != null) {
                filesequence.add(line);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Gson gson=new Gson();
        String mf= FileUtils.readFileToString(new File(mfpath),"utf8");
        Map<String,Map<String,Integer>> mfiles=gson.fromJson(mf,JsonObjectForMF.class).getMicroFile();//各个版本下所有微服务包含的文件数量
        Map<String,Integer> mfile=mfiles.get(versionName);//当前版本下各个微服务包含的文件数
        Map<String,List<String>> fileTopics=getFileTopics(filesequence,model.theta,upAndDown);//每个文件中符合要求的主题
        Map<String,Map<String,Double>> serviceTopics=new HashMap<>();//每个微服务下包含的topic以及每个topic在该微服务多少个文件里出现（作为权重）
        List<String> mspaths=getMs(mpath);//获取所有微服务路径集合
        List<String> msnames=new ArrayList<>();
        for(int h=0;h<mspaths.size();h++){
            msnames.add(getMsName(mspaths.get(h)));
        }
        for(String msname:mspaths){//对于每一个微服务而言
            List<String> items=new ArrayList<>();//每个微服务包含的topic集合，topic有重复，每个topic重复次数就是该topic重复影响当前微服务文件的次数
            for(String fname:filesequence){
                if(fname.startsWith(msname)){//这个文件是该微服务下的文件，则该文件涉及的topic都加入到当前微服务的topic集合中去
                    items.addAll(fileTopics.get(fname));
                }
            }

            //统计当前微服务下所包含topic分别出现次数，赋予权重
            Map<String,Double> topics=new HashMap<>();
            if (items == null || items.size() == 0) {
                serviceTopics.put(getMsName(msname),topics);
            }else{
                for (String temp : items) {
                    if(!topics.containsKey(temp)){
                        topics.put(temp,1.0);
                    }else{
                        double count = topics.get(temp);
                        topics.put(temp,count+1.0);
                    }
                }
                double fcount=mfile.get(getMsName(msname));//当前微服务包含的所有文件数量
                Map<String,Double> newTopic=new HashMap<>();
                for(Map.Entry<String,Double> en:topics.entrySet()){
                    String k=en.getKey();
                    double v=en.getValue()/fcount;//当前topic影响了当前微服务几分之几的文件
                    if(v>yuzhi){//若当前topic对当前微服务的影响范围超过几分之几yuzhi，则加入“影响微服务的topic”的集合中
                        newTopic.put(k,v);
                    }
                }
                serviceTopics.put(getMsName(msname),newTopic);
            }
        }
        Map<String,Integer> microtopicCount=new HashMap<>();
        for(Map.Entry<String,Map<String,Double>> enn:serviceTopics.entrySet()){
            microtopicCount.put(enn.getKey(),enn.getValue().size());//每个微服务有多少个topic
        }
        JsonObjectForMT1 micro_topics=new JsonObjectForMT1(150,microtopicCount,serviceTopics);

        return micro_topics;
        // output(serviceTopics,msnames,topic_words,document_topics,versionName);

    }
}
*/

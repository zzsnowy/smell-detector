package top.jach.tes.app.jhkt;

import com.alibaba.excel.EasyExcel;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import top.jach.tes.app.jhkt.codetopics.lda.*;
import top.jach.tes.app.jhkt.codetopics.preprocess.*;
import top.jach.tes.app.jhkt.codetopics.preprocess.excel.ExcelData;
import top.jach.tes.app.jhkt.codetopics.preprocess.excel.ExcelListener;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static top.jach.tes.app.jhkt.codetopics.common.Constant.*;
import static top.jach.tes.app.jhkt.codetopics.preprocess.CommonStopWordList.myStopWords;

/**
 * @author:AdminChen
 * @date:2020/8/4
 * @description:
 */
public class FinalTest {

    public static boolean isTrained = false;

    public static void main(String[] args) throws IOException, BiffException {
        // 等集成到TES中去之后，直接从其他模块传入一个List<String>作为主题建模最原始的输入，但将linux路径表示方式转换成windows路径表示方式的方法还是要写
        // 微服务完整路径列表，可从other模块中写个方法获取，类比于getMS()方法，这里不需要从excel中读，直接就能拿到list
//        Map<String,List<Double>> res=new HashMap<>();
//        String versionName="feature-20.0.1_release_0730_20190928142120";
//        String path="D:\\5gCore\\mss\\msFor4\\"+versionName+".xls";//读取4个版本各版本下微服务的excel
//        List<String> reposnames=getMs("D:\\5gCore\\mss\\ms2\\"+versionName+".xls");//代码仓集合
//        //每个版本执行前都执行一遍check当前版本所有代码仓的操作
//        for(String repos:reposnames){
//            Git git=Git.open(new File(repos));
//            git.reset().setMode(ResetCommand.ResetType.HARD).call();
//            git.checkout().setName(versionName).setForce(true).call();
//        }

        // step1、读取Excel，进行代码文本预处理。输出words.dat、files.flist
        preProcess(excelDir);

        // step2、执行LDA，获得主题模型
        ldaProcess(40);

        // step3、后置服务抽取、关注点筛选在 PostProcessTest中

    }

    public static void allProcess(String mspath){

    }

    public static List<String> linuxToWin(List<String> mslist){
        List<String> res = new ArrayList<>();
        for (String path : mslist){
            res.add(path.replace("/","\\"));
        }
        return res;
    }

    //获取一个list的箱型图的上下界，超出上下界范围则是异常值
    public static Double[] getAbnormalValue(List<Double> lst){
        Double[] abnormals=new Double[2];
        if(lst.size()<4)  return abnormals;
        List<BigDecimal> ablist=new ArrayList<>();
        for(Double num:lst){
            ablist.add(BigDecimal.valueOf(num));
        }
        int len=ablist.size();
        Collections.sort(ablist);
        BigDecimal q1=null;
        BigDecimal q3=null;
        int index=0;
        if(len%2==0){//偶数
            index=new BigDecimal(len).divide(new BigDecimal("4")).intValue();
            q1=ablist.get(index-1).multiply(new BigDecimal("0.25")).add(ablist.get(index).multiply(new BigDecimal("0.75")));
            index=new BigDecimal(3*(len+1)).divide(new BigDecimal("4")).intValue();
            q3=ablist.get(index-1).multiply(new BigDecimal("0.75")).add(ablist.get(index).multiply(new BigDecimal("0.25")));
        }else{//奇数
            q1=ablist.get(new BigDecimal(len+1).multiply(new BigDecimal("0.25")).intValue()-1);
            q3=ablist.get(new BigDecimal(len+1).multiply(new BigDecimal("0.75")).intValue()-1);
        }
        int iqr=(q3.subtract(q1)).intValue();
        abnormals[0]=q1.intValue()-1.5*iqr;
        abnormals[1]=q3.intValue()+1.5*iqr;
        return abnormals;
    }

    /**
     * 数据预处理：读取Excel里的项目路径，扫描源代码文件，
     * @param path
     * @throws IOException
     * @throws BiffException
     */
    private static void preProcess(String path) throws IOException, BiffException {
        Corpus corpus = preByService(path);
        // content1存储所有文件的words列表通过空格拼接而成的字符串
        StringBuilder content1 = new StringBuilder();
        content1.append(corpus.documents.size()).append("\n");
        for (Document doc : corpus.documents) {
            String line = String.join(" ", doc.words);
            line += "\n\n";
            content1.append(line);
        }

        // content2存储所有文件的绝对路径名
        StringBuilder content2 = new StringBuilder();
        for (String filename : corpus.fileNames) {
            content2.append(filename).append("\n");
        }

        // 第一阶段成果就是上述的content1和content2,将二者输出到main/java/src.test.example目录下的文件中
        try {
            File file = new File(wordsDir);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
                if (!file.exists()) {
                    file.createNewFile();
                }
            }

            FileWriter fileWritter = new FileWriter(file.getAbsolutePath(), false);
            fileWritter.write(content1.toString());
            fileWritter.close();

            File file2 = new File(filenameDir);
            if (!file2.getParentFile().exists()) {
                file2.getParentFile().mkdirs();
                if (!file2.exists()) {
                    file2.createNewFile();
                }
            }

            FileWriter fileWritter2 = new FileWriter(file2.getAbsolutePath(), false);
            fileWritter2.write(content2.toString());
            fileWritter2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Corpus preByService(String path) throws IOException {
        Corpus allCorpus = new Corpus(); //总的词袋
        allCorpus.init(new ArrayList<>(), new ArrayList<>());
//        List<String> allPath = getMs(path);
        List<String> allPath = getPathFromExcelByEasyExcel(path);
        // 扫描目标路径下的所有代码文件（文件树的DFS遍历）
        for(String mpath : allPath){
            // 自定义过滤规则
            FileFilter filter = pathname -> (pathname.getName().endsWith(".go")
                    || pathname.getName().endsWith(".java")
                    || pathname.getName().endsWith(".c")
                    || pathname.getName().endsWith(".cpp")
                    || pathname.isDirectory())
                    // 增加过滤测试文件的规则
                    && !pathname.getName().toLowerCase(Locale.ROOT).endsWith("test");

            Corpus corpus = new Corpus();
            //将target_projects目录下的案例项目文件夹用自定义过滤器过滤出来，所有符合要求的文件绝对路径名
            System.out.println("=========current path: " + mpath);
            corpus.init(mpath, filter);
            //输出所有源代码文件的绝对路径
            System.out.println("所有的源代码文件：" + corpus.documents.size());
            for (String file : corpus.fileNames) {
                System.out.println(file);
            }

            // 分词在init那一步已经做过了
            System.out.println("显示第一个文档的词处理结果：");
            System.out.println("分词后:    " + corpus.documents.get(0).words);
            PreProcessMethods.splitIdentifier(corpus);
            System.out.println("驼峰拆分后:    " + corpus.documents.get(0).words);
            PreProcessMethods.toLowerCase(corpus);
            System.out.println("转小写后:    " + corpus.documents.get(0).words);
            PreProcessMethods.removeStopWords(corpus, myStopWords);
            System.out.println("去停用词后:    " + corpus.documents.get(0).words);
            PreProcessMethods.filtering(corpus);
            System.out.println("去除杂质词后:    " + corpus.documents.get(0).words);
            PreProcessMethods.tf_idf(corpus);
            System.out.println("tf_idf筛选后:    " + corpus.documents.get(0).words);
            // 目前不一定执行提取词干，因为会出现create变成creat 单词被切割现象
            PreProcessMethods.stemming(corpus);
            System.out.println("词干提取后:    " + corpus.documents.get(0).words);

            // 输出3个最高词频的作为服务词汇参考
            Map<String, Integer> wordfrequency = new HashMap<>();
            for(Document document : corpus.documents){
                for(String w : document.words){
                    if(!wordfrequency.containsKey(w)){
                        wordfrequency.put(w, 0);
                    }
                    wordfrequency.put(w, wordfrequency.get(w)+1);
                }
            }
            List<Map.Entry<String, Integer>> collect = wordfrequency.entrySet().stream()
                    .sorted((entry1, entry2) -> (entry2.getValue() - entry1.getValue()))
                    .limit(3)
                    .collect(Collectors.toList());
            System.out.print("服务词汇: ");
            for (Map.Entry<String, Integer> entry : collect) {
                System.out.print(entry.getKey() + "-" + entry.getValue() + " ; ");
            }
            System.out.println();

            allCorpus.fileNames.addAll(corpus.fileNames);
            allCorpus.documents.addAll(corpus.documents);
        }
        Map<String, Integer> wordfrequency = new HashMap<>();
        for (Document document : allCorpus.documents) {
            for (String w : document.words) {
                if (!wordfrequency.containsKey(w)) {
                    wordfrequency.put(w, 0);
                }
                wordfrequency.put(w, wordfrequency.get(w) + 1);
            }
        }
        System.out.println("一共不重复词有：" + wordfrequency.size());
        return allCorpus;
    }

    /**
     * 【旧版本】使用JXL从指定Excel文件读取微服务项目路径
     * @param msPath
     * @return
     * @throws IOException
     * @throws BiffException
     */
    public static List<String> getMs(String msPath) throws IOException, BiffException {
        List<String> paths = new ArrayList<>();
        InputStream is = new FileInputStream(msPath);
        Workbook wb = Workbook.getWorkbook(is);
        Sheet sheet = wb.getSheet(0);
        int rows = sheet.getRows();
        for(int i = 0; i < rows; i++){
            String path = sheet.getCell(0,i).getContents().trim();
            paths.add(path.replace("/","\\"));
        }
        return paths;
    }

    /**
     * 基于easyExcel来读取Excel文件
     * @param excelPath
     * @return
     * @throws FileNotFoundException
     */
    private static List<String> getPathFromExcelByEasyExcel(String excelPath) throws FileNotFoundException {
        InputStream is = new FileInputStream(excelPath);
        ExcelListener listener = new ExcelListener();
        EasyExcel.read(is, ExcelData.class, listener).sheet().doRead();
        return listener.getPathList();
    }

    /**
     * 输入待聚类的数量，执行LDA训练，返回模型perplexity值
     * @param kValue
     * @return
     */
    private static double ldaProcess(int kValue){
        LDAOption option = new LDAOption();

        option.est = true;
        option.inf = false;

        option.alpha = 1.0 / kValue; // 控制语料库主题的密度，一般设置成主题数的倒数1/K
        option.beta = 0.01; // 尽量设小一些，0.01等
        option.K = kValue; // 经验来确定的值  设置不同k值比较主题模型效果好坏来确定好的k值。见Model.perplexity()方法
        option.niters = 1200; // 迭代次数一般1000-2000  从一开始到最终收敛过程中的迭代次数，值越大正确性越高，运行时间越长

        //训练结果输出文档存储的目录
        option.dir = exampleDir;
        option.dfile = "words.dat"; // 上一步分词、清洗后的所有文件的词库作为LDA输入
        option.savestep = 100;
        option.twords = 50; // 输出的tword中用前50个概率大的词代表主题的内容

        Estimator estimator = new Estimator();
        estimator.init(option);
        estimator.estimate();

        Model model = estimator.trnModel;

        System.out.println("本次训练的K值：" + kValue);
        System.out.println("模型perplexity值：" + model.perplexity());

        //isTrained=true;//训练完成，将isTrained标记为true
        return model.perplexity();
    }

    private static double[] getfiqr(List<Double> list){
        Double[] arrtmp = list.toArray(new Double[list.size()]);
        double[] arr = new double[arrtmp.length];
        for(int i = 0; i < arrtmp.length; i++){
            arr[i] = arrtmp[i];
        }
        double[] arr1 = new double[4];
        if (arr.length < 4){
            for(int i = 0; i < arr.length; i++){
                arr1[i] = arr[i];
            }
            for(int k = arr.length; k < 4; k++){
                arr1[k] = arr1[k];
            }
            return arr1;
        }
        double[] tempArr = Arrays.copyOf(arr,arr.length);
        Arrays.sort(tempArr);
        double[] quartiles = new double[3];
        double[] yichang = new double[2];
        int n =  arr.length;
        double Q1 = (n+1)*0.25D;
        double Q2 = (n+1)*0.5D;
        double Q3 = (n+1)*0.75D;
        if (Q1 % 2 == 0) {
            quartiles[0] = tempArr[(int)Q1];
        } else {
            double Q1y = Q1 - Math.floor(Q1);
            double Q1r;
            Q1r = (1D-Q1y)*tempArr[(int)Math.floor(Q1)-1]+Q1y*tempArr[(int)Math.ceil(Q1)-1];
            quartiles[0] = Q1r;
        }
        if(Q2%2==0){
            quartiles[1]=tempArr[(int)Q2];
        }else{
            double Q2y=Q2-Math.floor(Q2);
            double Q2r;
            Q2r=(1D-Q2y)*tempArr[(int)Math.floor(Q2)-1]+Q2y*tempArr[(int)Math.ceil(Q2)-1];
            quartiles[1]=Q2r;
        }
        if(Q3%2==0){
            quartiles[2]=tempArr[(int)Q3];
        }else{
            double Q3y=Q3-Math.floor(Q3);
            double Q3r;
            Q3r=(1D-Q3y)*tempArr[(int)Math.floor(Q3)-1]+Q3y*tempArr[(int)Math.ceil(Q3)-1];
            quartiles[2]=Q3r;
        }
        double iqr=quartiles[2]-quartiles[0];
        yichang[0]=quartiles[0]-1.5*iqr;
        yichang[1]=quartiles[2]+1.5*iqr;
        return yichang;
    }

    public static Map<String,List<String>> getFileTopics(List<String> filesequence, double[][] theta,double[] upAndDown){
        Map<String,List<String>> res = new HashMap<>();
        for(int i=0; i<filesequence.size(); i++){
            String file = filesequence.get(i);
            List<String> topics = new ArrayList<>();
            for(int j=0; j<theta[0].length; j++){
                if(theta[i][j] < upAndDown[0] || theta[i][j] > upAndDown[1]){
                    topics.add("topic" + j + "th");
                }
            }
            res.put(file, topics);
        }
        return res;
    }

    //最新postprocess，直接从这个方法输出msconcern.json可以得到微服务和topic之间的映射
    public static void postProcess(String mpath, String versionName){
        LDAOption option = new LDAOption();
        option.est = false;
        option.inf = true;
        option.modelName = "model-final";
        option.niters = 1200;
        option.dir = currentDir + "\\src\\test\\example\\"; //直接从lda已训练好的文件中得出

        Inferencer inferencer = new Inferencer();
        inferencer.init(option);

        Model model = inferencer.trnModel;
        // 上面就是LDATest中test2()的内容，获取已训练过的模型做以下操作
        List<Double> mk = new ArrayList<>(); //存储theta矩阵中所有数字，作为求四分位值+1.5iqr的输入
        for(int i = 0; i < model.M; i++){
            Double[] tmp = new Double[model.K];
            for(int j = 0; j < model.K; j++){
                tmp[j] = model.theta[i][j];
            }
            mk.addAll(Arrays.asList(tmp)); //List只存储包装类的元素，故数组要用Double类型
        }
        double[] upAndDown = getfiqr(mk);

        List<String> filesequence=new ArrayList<>();
        try{
            FileInputStream is = new FileInputStream(filenameDir);
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line = "";
            while((line = br.readLine()) != null){
                filesequence.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int docnum = model.M;
        Map<String,List<String>> fileTopics = getFileTopics(filesequence, model.theta, upAndDown); //每个文件中符合要求的主题
        Map<String,Map<String,Double>> serviceTopics = new HashMap<>();
    }
}

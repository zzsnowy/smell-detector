package top.jach.tes.app.jhkt;

import com.alibaba.excel.EasyExcel;
import com.google.gson.Gson;
import top.jach.tes.app.jhkt.codetopic.lda.Inferencer;
import top.jach.tes.app.jhkt.codetopic.lda.LDAOption;
import top.jach.tes.app.jhkt.codetopic.lda.Model;
import org.junit.Test;
import top.jach.tes.app.jhkt.codetopic.postprocess.FileDependence;
import top.jach.tes.app.jhkt.codetopic.postprocess.FunctionalTopic;
import top.jach.tes.app.jhkt.codetopic.postprocess.FunctionalTopicUtil;
import top.jach.tes.app.jhkt.codetopic.preprocess.excel.ServiceData;
import top.jach.tes.app.jhkt.codetopic.preprocess.excel.ServiceListener;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static preprocess.fileUtil.readFile;

public class PostProcessTest {

    public String currentDir = System.getProperty("user.dir");
    public String exampleDir = currentDir + "\\src\\test\\example";
    public String excelDir = currentDir + "\\project.xlsx";
    public String serviceDir = currentDir + "\\service.xlsx";
    public String concernDir = currentDir + "\\concern.txt";
    public String wordsDir = exampleDir + "\\words.dat";
    public String filenameDir = exampleDir + "\\files.flist";
    public String relationDir = "C:\\Users\\zhao\\Desktop\\relation.json";

    @Test
    public void functionalTopicSelectTest() throws FileNotFoundException {
        LDAOption option = new LDAOption();
        option.est = false;
        option.inf = true;
        option.modelName = "model-final";
        option.niters = 1000;
        option.dir = exampleDir;

        // 1、加载聚类模型
        Inferencer inferencer = new Inferencer();
        inferencer.init(option);
        Model model = inferencer.trnModel;
//        System.out.println(model.printTopics(10));

        // 2、读取文件列表
        List<String> fileSequence = readFileList();

        // 3、手动输入服务列表，每个服务为一个单词。包括服务下的文件列表
        Map<String, List<String>> serviceFiles = serviceExtract(fileSequence);

        // 下面的步骤需要先去depends项目里完成：对depends获取的依赖关系做进一步处理变成relation.json文件
        String content = readFile(relationDir);
        Gson gson = new Gson();
        // 要求FileDependence的属性结构与json对象的内容结构一致。json文件中包含fileSequence和dependGraph，FileDependence也包含这两个属性
        FileDependence fileDependence = gson.fromJson(content, FileDependence.class);

        FunctionalTopicUtil topicUtil = new FunctionalTopicUtil(fileSequence, serviceFiles, model, fileDependence);
//        FunctionalTopicUtil topicUtil = new FunctionalTopicUtil(fileSequence, serviceFiles, model);
        // 得到的是每个主题词与所有微服务的关系概率
        List<Map<String, Double>> list = topicUtil.getTopicServiceProb();
        System.out.println("每个主题词上，各个服务的关系概率：");
        for (int i = 0; i < list.size(); i++) {
            System.out.println("topic " + i + " 高度相关的前5个服务：---------------------------------------------------");
            List<Map.Entry<String, Double>> collect = list.get(i).entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(5)
                    .collect(Collectors.toList());
            System.out.println(collect);
        }

        // 筛选出功能性主题列表，非功能性的主题过滤掉。每个主题以FunctionalTopic的数据结构存储
        // tc_threshold 主题内聚性阈值，tc_threshold=0.4是根据论文中准确度达到峰值时对应的tc值设置的（论文P44）
        // file_threshold 主题与文件相关度阈值
        // service_threshold 主题与服务相关度阈值
        List<FunctionalTopic> topicList = topicUtil.findFunctionalTopic(0.4, 0.006, 0.015);
        writeConcernToFile(model, topicList, 0.4);
    }

    @Test
    public void serviceExtractTest() throws FileNotFoundException {
        // 服务抽取测试
        List<String> fileSequence = readFileList();
        serviceExtract(fileSequence);
    }

    /**
     * 手动获取服务信息。要得到服务名 和 旗下的文件列表
     * @param fileSequence
     * @return
     */
    private Map<String, List<String>> serviceExtract(List<String> fileSequence) throws FileNotFoundException {
        // 从Excel读取服务名称和目录
        InputStream is = new FileInputStream(serviceDir);
        ServiceListener listener = new ServiceListener();
        EasyExcel.read(is, ServiceData.class, listener).sheet().doRead();
        List<ServiceData> serviceList = listener.serviceList;

        // 对每个服务，读取其目录下的文件
        Map<String, List<String>> serviceFilesMap = new HashMap<>();
        FileFilter filter = pathname -> (pathname.getName().endsWith(".go")
                || pathname.getName().endsWith(".java")
                || pathname.getName().endsWith(".c")
                || pathname.getName().endsWith(".cpp")
                || pathname.isDirectory())
                // 增加过滤测试文件的规则
                && !pathname.getName().toLowerCase(Locale.ROOT).endsWith("test");
        for (ServiceData sd : serviceList) {
            List<String> files = scan(sd.getPath(), filter);
            serviceFilesMap.put(sd.getName(), files);
        }

        printServiceFileNumbers(serviceFilesMap);
        return serviceFilesMap;
    }

    /**
     * 读取files.flist
     * @return
     */
    private List<String> readFileList() {
        List<String> fileSequence = new ArrayList<>();
        try {
            FileInputStream is = new FileInputStream(filenameDir);
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                fileSequence.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileSequence;
    }

    /**
     * 打印每个服务对应的文件数量
     * @param serviceFiles
     */
    private void printServiceFileNumbers(Map<String, List<String>> serviceFiles) {
        System.out.println("服务 - 文件数量对应关系：");
        int total = 0;
        for (Map.Entry<String, List<String>> entry : serviceFiles.entrySet()) {
            System.out.println("service: " + entry.getKey() + " ; file number: " + entry.getValue().size());
            total += entry.getValue().size();
        }
        System.out.println("total:" + total);
        System.out.println("---------------------------------------------------------------------------------");
    }

    /**
     * 打印并把功能性主题筛选结果写入文件
     * @param model
     * @param topicList
     * @param tc_threshold
     */
    private void writeConcernToFile(Model model, List<FunctionalTopic> topicList, double tc_threshold) {
        System.out.println("功能性主题筛选结果：");
        int count = 0;
        // 反转得到服务对应的关注点
        HashMap<String, List<Integer>> serviceToConcern = new HashMap<>();
        // 写入文件的内容
        List<Integer> idList = new ArrayList<>();
        List<Double> tcList = new ArrayList<>();
        for (FunctionalTopic topic : topicList) {
            if (topic.tc >= tc_threshold) {
                System.out.println(model.printTopics(3).get(topic.topicID));
                System.out.println("TC = " + topic.tc);

                System.out.print("services:");
                topic.services.forEach(item -> {
                    System.out.print(item + ";");
                    List<Integer> list = serviceToConcern.getOrDefault(item, new ArrayList<>());
                    list.add(topic.topicID);
                    serviceToConcern.put(item, list);
                });
                System.out.println();

//                System.out.println("files:");
//                topic.files.forEach(System.out::println);
                count ++;

                idList.add(topic.topicID);
                tcList.add(topic.tc);
            }
        }
//        System.out.println("关注点数量：" + count);

        for (Map.Entry<String, List<Integer>> entry : serviceToConcern.entrySet()) {
            System.out.println("服务：" + entry.getKey());
            entry.getValue().forEach(concern -> System.out.print(concern + ","));
            System.out.println();
            System.out.println("关注点数量：" + entry.getValue().size());
        }

        // 关注点写入文件
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(concernDir));
            for (int i = 0; i < idList.size(); i++) {
                writer.write(idList.get(i) + " " + tcList.get(i));
                writer.write("\n");
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("写入：" + concernDir + " 完成");
    }

    private List<String> scan(String path, FileFilter filter) {
        File[] files = new File(path).listFiles(filter);
        List<String> fileNames = new ArrayList<>();
        for (File f : files) {
            if (f.isFile()) {
                fileNames.add(f.getAbsolutePath());
            } else {
                fileNames.addAll(scan(f.getAbsolutePath(), filter));
            }
        }
        return fileNames;
    }

    /**
     * 读取model-final.theta：文件与主题的概率分布
     * @return
     */
    private List<List<Double>> readFileTopicMap() {
        String theta = "D:\\data\\codeTopics\\src\\test\\example\\model-final.theta";
        List<List<Double>> result = new ArrayList<>();
        try {
            FileInputStream is = new FileInputStream(theta);
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                List<Double> dataList = Arrays.stream(line.split(" "))
                        .map(Double::parseDouble)
                        .collect(Collectors.toList());
                result.add(dataList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 读取关注点 - tc值信息
     */
    private Map<Integer, Double> readConcernTCMap() {
        Map<Integer, Double> concernTCMap = new HashMap<>();
        try {
            FileInputStream is = new FileInputStream(concernDir);
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                String[] datas = line.split(" ");
                concernTCMap.put(Integer.parseInt(datas[0]), Double.parseDouble(datas[1]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return concernTCMap;
    }

    @Test
    public void calculateConcernMetricTest() throws FileNotFoundException {
        // 加载数据
        List<String> fileList = readFileList();
        List<List<Double>> fileTopic = readFileTopicMap();
        Map<Integer, Double> concernTCMap = readConcernTCMap();
        Set<Integer> concerns = concernTCMap.keySet();

        // 读取每个服务下的文件列表，模拟一组功能原子文件
        Map<String, List<String>> serviceFiles = serviceExtract(fileList);
        double file_threshold = 0.006;
        // 对每个服务
        for (Map.Entry<String, List<String>> entry : serviceFiles.entrySet()) {
            System.out.println("service: " + entry.getKey());
            List<String> files = entry.getValue();
            double concernMetric = 0;
            // 对每个代码文件
            for (String file : files) {
                int fileId = fileList.indexOf(file);
                // 找出相关的主题
                List<Double> topics = fileTopic.get(fileId);
                List<Integer> curConcerns = new ArrayList<>();
                // 遍历每个topic
                for (int i = 0; i < topics.size(); i++) {
                    // 基于file_threshold和关注点进行过滤
                    if (topics.get(i) >= file_threshold && concerns.contains(i)) {
                        curConcerns.add(i);
                    }
                }
                // 计算当前代码文件的分数
                for (int c : curConcerns) {
                    // 关注点的TC值乘以当前关注点的概率
                    concernMetric += concernTCMap.get(c) * topics.get(c);
                }
            }
            System.out.println("concern分数：" + concernMetric);
        }
    }
}

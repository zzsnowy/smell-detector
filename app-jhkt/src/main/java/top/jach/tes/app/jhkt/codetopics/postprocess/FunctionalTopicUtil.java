package top.jach.tes.app.jhkt.codetopics.postprocess;

import top.jach.tes.app.jhkt.codetopics.lda.Model;

import java.util.*;

public class FunctionalTopicUtil {

    public List<String> globalFileSequence;  // 所有代码文件的顺序信息。

    public Map<String, List<String>> serviceFiles;

    public Model model;

    public int[][] graph;

    public FunctionalTopicUtil(List<String> globalFileSequence, Map<String, List<String>> serviceFiles, Model model){
        this.globalFileSequence = globalFileSequence;
        this.serviceFiles = serviceFiles;
        this.model = model;
    }

    /**
     * 自定义功能性主题筛选类
     *
     * @param globalFileSequence 代码文件列表
     * @param serviceFiles 服务 - 文件列表映射map
     * @param model LDA概率模型
     * @param fileDependence depends生成的依赖矩阵
     */
    public FunctionalTopicUtil(List<String> globalFileSequence, Map<String, List<String>> serviceFiles,
                               Model model, FileDependence fileDependence) {
        this.globalFileSequence = globalFileSequence;
        this.serviceFiles = serviceFiles;
        this.model = model;
        // fileDependency中的fileSequence是和globalFileSequence等价的
        graph = fileDependence.dependGraph;
    }

    /**
     * 计算TC值
     * @param selected
     * @param rest
     * @return
     */
    private double calculateTC(List<String> selected, List<String> rest) {
        // 当前主题已选文件的依赖数量
        int r_int = 0;
        // 当前主题已选文件和未选文件间的依赖数量
        int r_ext = 0;
        for (String file1 : selected) {
            int i = globalFileSequence.indexOf(file1);
            for (String file2 : selected) {
                int j = globalFileSequence.indexOf(file2);
                if (i != j && graph[i][j] == 1) {
                    r_int += 1;
                }
            }
        }
        for (String file1 : selected) {
            int i = globalFileSequence.indexOf(file1);
            for (String file2 : rest) {
                int j = globalFileSequence.indexOf(file2);
                if (graph[i][j] == 1) {
                    r_ext += 1;
                }
            }
        }
        return r_int / (r_int + r_ext + 0.000000000000001);
    }


    // 求解p(t|ms)  该方法输出的list作为concernOverload的输入数据
    public List<Map<String,Double>> getTopicServiceProb(){
        // list[i]表示第i个主题的p(t|ms)
        List<Map<String,Double>> list = new ArrayList<>();
        for(int topic = 0; topic <model.K; topic++) {//t
            // 每个主题计算一个map
            Map<String, Double> serviceProbs = new HashMap<>();
            // 对每个主题，遍历全部的微服务
            for (Map.Entry<String, List<String>> service : serviceFiles.entrySet()) {
                String serviceName = service.getKey();
                List<String> files = service.getValue();
                int serviceWords = 0;
                double serviceProbability = 0.0;

                // 遍历当前服务下的代码文件
                for (String file : files) {
                    // 微服务包含的文件
                    int fileID = globalFileSequence.indexOf(file);
                    // 每个文件分配的主题词数量
                    int fileWords = model.z[fileID].size();
                    // 当前微服务包含的主题词数量
                    serviceWords += fileWords;

//                    if (serviceName.equals("transaction")) {
//                        System.out.println("cur topic: " + topic + "; cur srv: " + serviceName + "; cur File: " + file);
//                        System.out.println("filewords: " + fileWords + "; servicewords: " + serviceWords);
//                    }
                }

                for (String file : files) {
                    // 微服务包含的文件
                    int fileID = globalFileSequence.indexOf(file);
                    // 当前主题词是当前文件主题的概率
                    double fileProb = model.theta[fileID][topic];
                    int fileWords = model.z[fileID].size();

//                    if (serviceName.equals("transaction")) {
//                        System.out.println("filewords: " + fileWords + "; servicewords: " + serviceWords + "; fileProb: " + fileProb + "; serviceProb: " + serviceProbability);
//                    }
                    serviceProbability += (double) fileWords / serviceWords * fileProb; //公式中的p(t|ms)
                }
                // 主题词与当前微服务的关系概率
                System.out.println("######## topic - " + topic + " put srv " + serviceName + " : " + serviceProbability);
                serviceProbs.put(serviceName, serviceProbability);
            }
            // 当前主题词与全部微服务的关系概率
            list.add(serviceProbs);
        }

        return list;
    }

    /**
     * 寻找功能性主题
     * @param tc_threshold 主题内聚性阈值
     * @param file_threshold 主题与文件相关度阈值
     * @param service_threshold 主题与服务相关度阈值
     * @return
     */
    public List<FunctionalTopic> findFunctionalTopic(double tc_threshold, double file_threshold, double service_threshold) {
        List<FunctionalTopic> ret = new ArrayList<>();
        // 主题与服务的相关度
        List<Map<String,Double>> topicServiceProb = getTopicServiceProb();
        for (int topic = 0; topic < model.K; topic++) {
            double tc = 0;
            double max_tc = tc;
            List<String> selectedFile = new ArrayList<>();
            List<String> restFile = new ArrayList<>(globalFileSequence);

            // System.out.println(selectedFile);
            // System.out.println(restFile);

            //计算p(t|ms)的过程
            // 当前主题与所有服务的相关度
            Map<String, Double> serviceProbs = topicServiceProb.get(topic);

//            printServiceToTopicProb(topic, serviceProbs);

            // 按相关度从高到低排序
            List<Map.Entry<String, Double>> sortedserviceList = new ArrayList<>(serviceProbs.entrySet());
            sortedserviceList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
//            System.out.println("当前主题：" + topic + " 与各个服务的关联概率");
//            System.out.println(sortedserviceList);

            // 当前主题相关的服务列表
            ArrayList<String> topic_services = new ArrayList<>();
            int index = 0;
            // 根据主题内聚度和服务相关度阈值，筛选与当前主题高度相关的服务，并记录下相关的代码文件
            // 第一次循环tc值未确定，需要基于tc < tc_threshold的条件找到第一个满足内聚性的功能性主题
            while (index < sortedserviceList.size() && tc < tc_threshold && sortedserviceList.get(index).getValue() >= service_threshold) {
                String curService = sortedserviceList.get(index).getKey();
                List<String> files = serviceFiles.get(curService);
                topic_services.add(curService);

                for (String file : files) {
                    int fileID = globalFileSequence.indexOf(file);
                    if (model.theta[fileID][topic] >= file_threshold) {
                        selectedFile.add(file);
                        restFile.remove(file);
                    }
                }

                tc = calculateTC(selectedFile, restFile);
                max_tc = Math.max(tc, max_tc);
                index++;
            }

            // 之后再通过第二次循环，扩展功能性主题的服务集合
            while (index < sortedserviceList.size() && tc >= tc_threshold && sortedserviceList.get(index).getValue() >= service_threshold) {
                topic_services.add(sortedserviceList.get(index).getKey());
                List<String> files = serviceFiles.get(sortedserviceList.get(index).getKey());

                for (String file : files) {
                    int fileID = globalFileSequence.indexOf(file);
                    if (model.theta[fileID][topic] >= file_threshold) {
                        selectedFile.add(file);
                        restFile.remove(file);
                    }
                }

                tc = calculateTC(selectedFile, restFile);
                max_tc = Math.max(tc, max_tc);
                index++;
            }

            FunctionalTopic ft = new FunctionalTopic();
            ft.tc = max_tc;
            ft.files = new ArrayList<>(selectedFile);
            ft.topicID = topic;
            ft.services = topic_services;
            ret.add(ft);
        }

        Collections.sort(ret);
        return ret;
    }

    /**
     * 打印服务与主题的概率分布
     * @param topic
     * @param serviceProbs
     */
    private void printServiceToTopicProb(int topic, Map<String, Double> serviceProbs) {
//        for (Map.Entry<String, List<String>> service : serviceFiles.entrySet()) {
//            String serviceName = service.getKey();
//            List<String> files = service.getValue();
//
//            int serviceWords = 0;
//            double serviceProbability = 0.0;
//
//            for (String file : files) {
//                int fileID = globalFileSequence.indexOf(file);
//                int fileWords = model.z[fileID].size();
//                serviceWords += fileWords;
//            }
//            //System.out.println(serviceWords);
//
//            for (String file : files) {
//                int fileID = globalFileSequence.indexOf(file);
//                double fileProb = model.theta[fileID][topic];
//                int fileWords = model.z[fileID].size();
//                serviceProbability += (double) fileWords / serviceWords * fileProb;
//            }
//            serviceProbs.put(serviceName, serviceProbability);
//        }
        System.out.println(serviceProbs);
    }


}





package top.jach.tes.app.jhkt;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static top.jach.tes.app.jhkt.codetopics.preprocess.fileUtil.readFile;

public class FunctionAtomTest {

    private static final String dependsPath = "D:\\dependency.json";
    private static final String[] dependType = {"Call", "Contain", "Import", "Include", "Parameter", "Return", "Use"};

    @Test
    public void generateFATest() {
        // 通过凝聚式分层聚类得到功能原子
        String jsonString = readFile(dependsPath);
        System.out.println("读取：" + dependsPath + " 成功");
        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        JSONArray fileArray = jsonObject.getJSONArray("variables");
        int len = fileArray.size();
        // step1、读取文件列表
        List<String> fileSequence = new ArrayList<>();
        for (Object aFileArray : fileArray) {
            fileSequence.add(aFileArray.toString());
        }
        System.out.println("获取文件列表成功");

        // step2、生成类文件之间的依赖矩阵
        int[][] dependMatrix = new int[len][len];
        JSONArray cellArray = jsonObject.getJSONArray("cells");
        for (int i = 0; i < cellArray.size(); i++){
            JSONObject subObj = cellArray.getJSONObject(i);
            int srcId = subObj.getIntValue("src");
            int destId = subObj.getIntValue("dest");
//            System.out.println(srcId + " to " + destId);
            JSONObject values = subObj.getJSONObject("values");
            for (String type : dependType) {
                int weight = values.getIntValue(type);
                if (weight > 0) {
//                    System.out.println(type + ":" + weight);
                    dependMatrix[srcId][destId] += weight;
                }
            }
        }
//        for (int i = 0; i < len; i++) {
//            for (int j = 0; j < len; j++) {
//                if (dependMatrix[i][j] > 0) {
//                    System.out.println(i + " " + j + " " + dependMatrix[i][j]);
//                }
//            }
//        }

        System.out.println("获取依赖关系成功");

        // step3、层次聚类
        clustering(fileSequence, dependMatrix);

    }

    class FunctionAtom {
        List<Integer> files;

        public FunctionAtom(int init) {
            files = new ArrayList<>();
            files.add(init);
        }
    }

    class PairDistance {
        int x, y;
        double distance;

        public PairDistance(int x, int y, double distance) {
            this.x = x;
            this.y = y;
            this.distance = distance;
        }
    }

    private void clustering(List<String> fileSequence, int[][] dependMatrix) {
        // 初始每个类都是一个簇
        int len = fileSequence.size();
        List<FunctionAtom> functionAtomList = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            functionAtomList.add(new FunctionAtom(i));
        }

        // 停止条件
        int minClusterNum = (int)(len * 0.1);

        while (functionAtomList.size() > minClusterNum) {
            // 计算当前功能原子的点对相似度
            List<PairDistance> distances = calculateDistance(functionAtomList, dependMatrix);
            // 从大到小排序
            distances.sort((i1, i2) -> Double.compare(i2.distance, i1.distance));
            // 选择两个最大的进行合并
            FunctionAtom fax = functionAtomList.get(distances.get(0).x);
            FunctionAtom fay = functionAtomList.get(distances.get(0).y);
            fax.files.addAll(fay.files);
            functionAtomList.remove(distances.get(0).y);
        }

        System.out.println("current functionAtomList size:" + functionAtomList.size());

        HashMap<Integer, Integer> countMap = new HashMap<>();
        for (int i = 0; i < functionAtomList.size(); i++) {
            System.out.println("功能原子-" + (i + 1) + " 文件数量：" + functionAtomList.get(i).files.size() + " ------------------------------------------------------------------------");
            for (int fi : functionAtomList.get(i).files) {
                System.out.println(fileSequence.get(fi));
            }
            countMap.put(functionAtomList.get(i).files.size(), countMap.getOrDefault(functionAtomList.get(i).files.size(), 0) + 1);
        }

        for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
            System.out.println(entry.getKey() + ";" + entry.getValue());
        }

    }

    private List<PairDistance> calculateDistance(List<FunctionAtom> functionAtomList, int[][] dependMatrix) {
        int len = functionAtomList.size();
        List<PairDistance> list = new ArrayList<>();

        for (int i = 0; i < len; i++) {
            FunctionAtom fax = functionAtomList.get(i);
            for (int j = i + 1; j < len; j++) {
                FunctionAtom fay = functionAtomList.get(j);

                double curDis = 0;
                for (int xfile : fax.files) {
                    for (int yfile : fay.files) {
                        curDis += dependMatrix[xfile][yfile];
                        curDis += dependMatrix[yfile][xfile];
                    }
                }
                curDis /= (fax.files.size() * fay.files.size());

                list.add(new PairDistance(i, j, curDis));
            }
        }
        return list;
    }
}

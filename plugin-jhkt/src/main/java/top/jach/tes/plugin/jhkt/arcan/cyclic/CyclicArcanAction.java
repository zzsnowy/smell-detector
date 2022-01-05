package top.jach.tes.plugin.jhkt.arcan.cyclic;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import org.apache.commons.compress.utils.Lists;
import top.jach.tes.core.api.domain.action.Action;
import top.jach.tes.core.api.domain.action.DefaultOutputInfos;
import top.jach.tes.core.api.domain.action.InputInfos;
import top.jach.tes.core.api.domain.action.OutputInfos;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.api.domain.info.Info;
import top.jach.tes.core.api.domain.meta.Meta;
import top.jach.tes.core.api.exception.ActionExecuteFailedException;
import top.jach.tes.core.impl.domain.element.Element;
import top.jach.tes.core.impl.domain.element.ElementsInfo;
import top.jach.tes.core.impl.domain.element.ElementsValue;
import top.jach.tes.core.impl.domain.info.value.StringInfo;
import top.jach.tes.core.impl.domain.meta.InfoField;
import top.jach.tes.core.impl.domain.relation.PairRelation;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;

import javax.lang.model.util.Elements;
import java.sql.SQLOutput;
import java.util.*;

public class CyclicArcanAction implements Action {
    public static final String Elements_INFO = "elements_info";
    public static final String PAIR_RELATIONS_INFO = "PairRelationsInfo";
    public static final int MAX_NODE_COUNT = 100;
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
        return () -> Arrays.asList(
                InfoField.createField(Elements_INFO).setInfoClass(ElementsInfo.class),
                InfoField.createField(PAIR_RELATIONS_INFO).setInfoClass(PairRelationsInfo.class)
        );
    }

    private static List<Integer> getCycle(List<Integer> dfsTrace, Integer node) {
        List<Integer> tmpList = new ArrayList<>();
        int f = 0;
        for (Integer integer : dfsTrace) {
            if (integer.equals(node) || f == 1) {
                int tmp = integer;
                tmpList.add(tmp);
                f = 1;
            }
        }
        return tmpList;
    }

    private static class Cycle {
        private List<Integer> list;
        public Cycle(List<Integer> list) {
            int offset = list.indexOf(Collections.min(list));
            this.list = new ArrayList<>(list.subList(offset, list.size()));
            this.list.addAll(list.subList(0, offset));
        }

        @Override
        public boolean equals(Object o) {
            return hashCode() == o.hashCode();
        }

        @Override
        public int hashCode() {
            return list.toString().hashCode();
        }

        @Override
        public String toString() {
            return "Cycle{" +
                    "list=" + list +
                    '}';
        }
    }

    private static void _dfs(int[][] matrix, Integer currentNode, boolean[] flag, List<Integer> dfsTrace, HashSet<Cycle> result, boolean[] dfsFlag){
        // 1. 出口
        dfsFlag[currentNode] = true;
        if(flag[currentNode]) {
            result.add(new Cycle(getCycle(dfsTrace, currentNode)));
            return;
        }
        // 2. body 计算所有可能的分支
        flag[currentNode] = true;
        dfsTrace.add(currentNode);

        for(int nextNode = 0; nextNode < 100; nextNode++) {
            if(matrix[currentNode][nextNode] != 1) {
                continue;
            }
            _dfs(matrix, nextNode, flag, dfsTrace, result, dfsFlag);
        }

        flag[currentNode] = false;
        dfsTrace.remove(currentNode);
    }


    //该方法根据元素和元素之间的关系，以此为参数调用方法，输出架构异味
    @Override
    public OutputInfos execute(InputInfos inputInfos, Context context) throws ActionExecuteFailedException {
        ElementsInfo<Element> elementsInfo = inputInfos.getInfo(Elements_INFO, ElementsInfo.class);
        PairRelationsInfo pairRelationsInfo = inputInfos.getInfo(PAIR_RELATIONS_INFO, PairRelationsInfo.class);
        ElementsValue elementCyclic = CalculateCyclic(context, elementsInfo, pairRelationsInfo);
        if(elementCyclic == null){
            return null;
        }
        return DefaultOutputInfos.WithSaveFlag(elementCyclic);
    }

    public static ElementsValue CalculateCyclic(Context context, ElementsInfo elementsInfo, PairRelationsInfo pairRelationsInfo) {
        //构建有向图
        List<Element> nodess = Lists.newArrayList(elementsInfo.iterator());
        List<String> elements = new ArrayList<String>();//存储节点名即可
        for (Element e : nodess) {
            elements.add(e.getElementName());
        }
        List<PairRelation> relations = Lists.newArrayList(pairRelationsInfo.getRelations().iterator());
        int[][] matrix = new int[100][100];//有向图的邻接矩阵
        if (elements.size() > 100) {
            context.Logger().info("elements 超长");
            return null;
        }
        //初始化矩阵
        for (PairRelation pr : relations) {
            int startIndex = elements.indexOf(pr.getSourceName());
            int endIndex = elements.indexOf(pr.getTargetName());
            if (startIndex >= 0 && endIndex >= 0) {
                matrix[startIndex][endIndex] = 1;
            }
        }

        boolean[] flag = new boolean[100];
        boolean[] dfsFlag = new boolean[100];
        Arrays.fill(flag, false);
        Arrays.fill(dfsFlag, false);
        List<Integer> dfsTrace = new ArrayList<>();
        HashSet<Cycle> result = new HashSet<>();
        for (int currentNode = 0; currentNode < 100; currentNode++) {
            if (dfsFlag[currentNode]) {
                continue;
            }
            _dfs(matrix, currentNode, flag, dfsTrace, result, dfsFlag);
        }
//        result.stream().forEach(circle -> {
//            System.out.println(circle);
//        });

        //把数值表示的环映射为微服务名称表示的环，同时去除自身依赖的情况
        List<List<String>> cycleResult = getCycleResult(result, elements);

        ElementsValue elementCyclic = ElementsValue.createInfo();
        HashMap<String, Double> map = new HashMap<>();
        for (List<String> c : cycleResult) {
            for (String node : c) {
                if (map.containsKey(node)) {
                    map.put(node, map.get(node) + 1.0);
                } else {
                    map.put(node, 1.0);
                }
            }
        }
        elementCyclic.setValue(map);
        return elementCyclic;
    }


    private static List<List<String>> getCycleResult(HashSet<Cycle> result, List<String> elements) {
        List<List<String>> cycleResult = new ArrayList<>();
        for(Cycle c : result ){
            if(c.list.size() == 1) continue;
            List<String> tmp = new ArrayList<>();
            for(Integer node : c.list){
                tmp.add(elements.get(node));
            }
            cycleResult.add(tmp);
        }
        return cycleResult;
    }


    public static ElementsValue CalculateUndirectedCyclic(Context context, ElementsInfo elementsInfo, PairRelationsInfo pairRelationsInfo) {
        return null;
    }
}




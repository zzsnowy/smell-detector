package top.jach.tes.plugin.jhkt.arcan.hublink;

import org.apache.commons.compress.utils.Lists;
import top.jach.tes.core.api.domain.action.Action;
import top.jach.tes.core.api.domain.action.DefaultOutputInfos;
import top.jach.tes.core.api.domain.action.InputInfos;
import top.jach.tes.core.api.domain.action.OutputInfos;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.api.domain.meta.Meta;
import top.jach.tes.core.api.exception.ActionExecuteFailedException;
import top.jach.tes.core.impl.domain.element.Element;
import top.jach.tes.core.impl.domain.element.ElementsInfo;
import top.jach.tes.core.impl.domain.element.ElementsValue;
import top.jach.tes.core.impl.domain.info.value.StringInfo;
import top.jach.tes.core.impl.domain.meta.InfoField;
import top.jach.tes.core.impl.domain.relation.PairRelation;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.plugin.jhkt.microservice.Microservice;
import top.jach.tes.plugin.jhkt.microservice.MicroservicesInfo;

import java.util.*;

public class HublinkArcanAction implements Action {
    public static final String Elements_INFO = "elements_info";
    public static final String PAIR_RELATIONS_INFO = "PairRelationsInfo";
    public static final String HUBLINK_IN_AND_OUT="hublinkElements";
    public static final String HUBLINK_IN="hublinkElements_e";
    public static final String HUBLINK__OUT="hublinkElements_s";


    public static final int MAX_NODE_COUNT = 20;



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
    //计算每个节点在各个集合中出现次数+排序+输出所有步骤抽取成一个方法
    public static ElementsValue cal(List<String> nodes,List<Double> nodesValue,List<String> allnodes,HashMap<String,Double> map,String flag){
        // Set<String> nset=nodes.keySet();
        //////这个nodes是Map格式的，不允许同样的值存在，这个for循环相当于只是把nodes复制给map罢了
        for(int i=0;i<nodes.size();i++){
            if(map.containsKey(nodes.get(i))){
                double tmp2=map.get(nodes.get(i))+nodesValue.get(i);
                map.put(nodes.get(i),tmp2);
            }
            else{
                map.put(nodes.get(i),nodesValue.get(i));
            }
        }
        //就是下面这个for循环把所有的hubink值变成了0
        //不存在该异味的微服务也要加上
        for(int j=0;j<allnodes.size();j++){
            if(!map.containsKey(allnodes.get(j))){
                map.put(allnodes.get(j),0.0);
            }
        }
        /*Set set=map.entrySet();
        //为了使map能按照value值排序
        List<Map.Entry<String,Double>> list=new ArrayList<Map.Entry<String,Double>>(set);
        Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));*/
        ElementsValue element=ElementsValue.createInfo();
        element.setName(flag);
        element.setValueMap(map);

        return element;
    }
    public static ElementsValue calculateHub(MicroservicesInfo microservices, ElementsValue hublike_weight_in, ElementsValue hublike_weight_out, ElementsValue hub) {
        List<Map.Entry<String, Double>> listIn = new ArrayList<>(hublike_weight_in.getValueMap().entrySet());
        List<Map.Entry<String, Double>> listOut = new ArrayList<>(hublike_weight_out.getValueMap().entrySet());
        listIn.sort(new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        listOut.sort(new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        double midIn, midOut;
        int len = listIn.size();
        assert len % 2 == 0;
        if((len % 2) == 0){
            midIn = (listIn.get(len / 2).getValue() + listIn.get(len / 2 - 1).getValue())/2.0;
            midOut = (listOut.get(len / 2).getValue() + listOut.get(len / 2 - 1).getValue())/2.0;
        }else {
            midIn = listIn.get(len / 2).getValue();
            midOut = listOut.get(len / 2).getValue();
        }
        //若包满足：传入依赖数 > 传入mid && 传出依赖数 > 传出mid &&  |传入依赖数 - 传出依赖数| <= 1/4(传入依赖数 + 传出依赖数)，则认为有异味
//        for(Map.Entry<String, Double> entry : hublike_weight_in.getValueMap().entrySet()){
//            if(entry)
//        }
        Map<String, Double> mapIn = hublike_weight_in.getValueMap();
        Map<String, Double> mapOut = hublike_weight_out.getValueMap();
        ElementsValue element=ElementsValue.createInfo();
        for(Microservice micro:microservices){
            String microName=micro.getElementName();
            if((mapIn.get(microName) > midIn) && (mapOut.get(microName) > midOut) &&
                    (Math.abs(mapIn.get(microName) - mapOut.get(microName)) <= (hub.getValueMap().get(microName)/4.0))){
                element.put(microName, 1.0);
            }else{
                element.put(microName, 0.0);
            }
        }
        return element;
    }



    //该方法根据元素和元素之间的关系，以此为参数调用方法，输出架构异味
    @Override
    public OutputInfos execute(InputInfos inputInfos, Context context) throws ActionExecuteFailedException {
        PairRelationsInfo pairRelationsInfo = inputInfos.getInfo(PAIR_RELATIONS_INFO, PairRelationsInfo.class);
        List<PairRelation> relations = Lists.newArrayList(pairRelationsInfo.getRelations().iterator());
        List<String> nodes=new ArrayList<>();//存储所有节点名
        List<String> sourceNodes=new ArrayList<>();//存储开始节点名
        List<String> endNodes=new ArrayList<>();//存储结束节点名
        List<Double> nodesValue=new ArrayList<>();
        List<Double> sourceNodesValue=new ArrayList<>();
        List<Double> endNodesValue=new ArrayList<>();
        /*HashMap<String, Double> nodes=new HashMap<>();
        HashMap<String, Double> sourceNodes=new HashMap<>();
        HashMap<String, Double> endNodes=new HashMap<>();//*/
        //map存储的不允许重复，而hublink就是为了计算重复次数，改成两个list同步记录节点名和对应的权重值
        //从而允许重复
        for(int i=0;i<relations.size();i++){
            sourceNodes.add(relations.get(i).getSourceName());
            sourceNodesValue.add(relations.get(i).getValue());
            endNodes.add(relations.get(i).getTargetName());
            endNodesValue.add(relations.get(i).getValue());
            nodes.add(relations.get(i).getSourceName());
            nodesValue.add(relations.get(i).getValue());
            nodes.add(relations.get(i).getTargetName());
            nodesValue.add(relations.get(i).getValue());
        }

        List<String> allnodes=new ArrayList<>(new ArrayList<>(nodes));
        //HashMap allnodes=(HashMap) ((HashMap<String, Double>) nodes).clone();//赋值所有节点名，三个计算都需要用到
        //排序
        HashMap<String, Double> map = new HashMap<>();
        HashMap<String, Double> sourceMap = new HashMap<>();
        HashMap<String, Double> endMap = new HashMap<>();
        ElementsValue elementHublink=cal(nodes,nodesValue,allnodes,map,HUBLINK_IN_AND_OUT);
        ElementsValue elementHublink_s=cal(sourceNodes,sourceNodesValue,allnodes,sourceMap,HUBLINK__OUT);
        ElementsValue elementHublink_e=cal(endNodes,endNodesValue,allnodes,endMap,HUBLINK_IN);

        return DefaultOutputInfos.WithSaveFlag(elementHublink,elementHublink_s,elementHublink_e);

    }
}

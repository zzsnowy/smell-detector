package top.jach.tes.plugin.jhkt.arcsmell.ui;

import com.sun.org.apache.bcel.internal.generic.RETURN;
import org.apache.commons.compress.utils.Lists;
import top.jach.tes.core.api.domain.action.Action;
import top.jach.tes.core.api.domain.action.InputInfos;
import top.jach.tes.core.api.domain.action.OutputInfos;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.api.domain.meta.Meta;
import top.jach.tes.core.api.exception.ActionExecuteFailedException;
import top.jach.tes.core.impl.domain.element.ElementsInfo;
import top.jach.tes.core.impl.domain.element.ElementsValue;
import top.jach.tes.core.impl.domain.meta.InfoField;
import top.jach.tes.core.impl.domain.relation.PairRelation;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.plugin.jhkt.arcsmell.hublink.HublinkAction;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvAction;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvResult;
import top.jach.tes.plugin.jhkt.microservice.Microservice;
import top.jach.tes.plugin.jhkt.microservice.MicroservicesInfo;
import top.jach.tes.plugin.tes.code.git.commit.GitCommit;

import java.util.*;

/**
 * @author:AdminChen
 * @date:2020/4/9
 * @description: detect for unstable interface
 */
public class UiAction implements Action{
    public static final String Elements_INFO = "elements_info";
    public static final String PAIR_RELATIONS_INFO = "PairRelationsInfo";
    public static final String HUBLINK_IN_AND_OUT="hublinkElements";
    public static final String HUBLINK_IN="hublinkElements_e";
    public static final String HUBLINK__OUT="hublinkElements_s";
    public static final String UD="udElements";
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
    //遍历pairrelations得到每个微服务所依赖的服务名称的集合，每个微服务与其依赖集合构成map中一个键值对
    public static Map<String,List<String>> getDependencies(MicroservicesInfo microservices,PairRelationsInfo pairRelationsInfo){
        Map<String,List<String>> microDependenciesMap=new HashMap<>();

        List<PairRelation> relations = Lists.newArrayList(pairRelationsInfo.getRelations().iterator());
        //遍历relations得到当前服务所依赖的服务名称的集合relist
        for(Microservice micro:microservices){
            String microName=micro.getElementName();
            List<String> relist=new ArrayList<>();
            for(int i=0;i<relations.size();i++){
                if(microName.equals(relations.get(i).getSourceName())){//当前服务处于开始节点位置，则说明依赖
                    relist.add(relations.get(i).getTargetName());
                }
            }
            microDependenciesMap.put(microName,relist);
        }
        return microDependenciesMap;
    }
//某个微服务的修改，影响到了哪些文件；
    //某个微服务的修改，会影响到哪些微服务,最终用UiResult来存储每个被依赖数大于impact的微服务的修改，影响了哪些文件以及微服务
//这个才是有用的计算Ui
    public static List<UiResult> calUi(List<GitCommit> gitCommits, int len, List<Microservice> microservices, PairRelationsInfo pairRelationsInfo, double impact){
        List<UiResult> result=new ArrayList<>();
        //这里写ui的输出逻辑，主要基于hublike和mv的结果做一层输出形式更改然后输出就可以了
        ElementsValue inElement= HublinkAction.calculateHublikeIn(pairRelationsInfo);
        Map<String,Double> inLinks=inElement.getValueMap();
        Map<String, Map<String,Integer>> mvFiles= MvAction.detectMvResult(gitCommits,len,microservices).getResultFiles();
        Map<String,Map<String,Integer>> mvMicros=MvAction.detectMvResultForUi(gitCommits,len,microservices).getResultFiles();
        Map<String,Set<String>> mcroFiles=MvAction.findMicroFiles(gitCommits,microservices);
        for(Microservice microservice:microservices){
            String name=microservice.getElementName();
            UiResult res=new UiResult();
            Map<String,Integer> Ufiles=new HashMap<>();
            //Map<String,Integer> Umicros=new HashMap<>();
            res.setMicroservice(name);//对于每个微服务，若不满足条件，则uiresult后两个属性不设值，为空
            res.setMsFiles(mcroFiles.get(name));
            //在get(name)这一步有可能得到null，共21个微服务最后Hubike得到的是17个的值
            if(inLinks.containsKey(name)){
                if(inLinks.get(name)>impact){
                    for(String file:mvFiles.keySet()){
                        String prefix = MvAction.getMicroserviceNameByFilePath(file, microservices);//file对应的微服务名
                        if(name.equals(prefix)){
                            for(Map.Entry<String,Integer> en:mvFiles.get(file).entrySet()){
                                Ufiles.put(en.getKey(),en.getValue());
                            }
                            //Ufiles.putAll(mvFiles.get(file));
                        }
                    }

                    res.setUiFiles(Ufiles);//当前微服务所包含文件所影响的文件集合
                    res.setUiMicroservices(mvMicros.get(name));//当前微服务影响的微服务集合
                }
            }

            result.add(res);
        }
        //可参考自己原来写的calculateUi代码
        return result;
    }


    public static ElementsValue calculateUi(List<GitCommit> gitCommits, int len, MicroservicesInfo microservices, PairRelationsInfo pairRelationsInfo, double impact, double cochange, double change){
        ElementsValue element=ElementsValue.createInfo();
        Map<String,Set<String>> resultDetail=new HashMap<>();//存储中间结果，而非只给一个值
//目前已可以通过MvAction类中的detectMvResultForUi方法获取每个微服务对应的与其共同变更的微服务集合及共同变更次数
        //得到的string对应microservice类的getAllPath()方法获得的路径
        ElementsValue inElement=HublinkAction.calculateHublikeIn(pairRelationsInfo);
        List<Microservice> microservices1=microservices.getMicroservices();
        MvResult mvResult= MvAction.detectMvResultForUi(gitCommits,len,microservices1);
        Map<String,Map<String,Integer>> microResult=mvResult.getResultFiles();
        for(Microservice microservice:microservices1){
            //String mname=microservice.getAllPath();
            String name=microservice.getElementName();
            Set<String> cochangeSet=new HashSet<>();//与当前遍历到的微服务共同变更次数超过cochange次的微服务集合
            double inValue=0.0;//inElement只有18个，总的微服务有21个，自然存在null情况
            if(inElement.getValueMap().get(name)!=null){
                inValue=inElement.getValueMap().get(name);//若不在inElement里则默认inValue为0
            }

            //由于整个筛选要经过四层阈值筛选，对于每个符合要求的if都做了操作，但对于不符合其中一层要求的else没操作
            //这也导致了ui检测结果其他都为空，只有全部符合要求的微服务才有值。把else的处理代码要加上来
            if(inValue>impact){
                Map<String, Integer> cochangeMap=microResult.computeIfAbsent(name,k -> new HashMap<>());
                if(cochangeMap.size()>0){
                    for(String key:cochangeMap.keySet()){
                        if(cochangeMap.get(key)>cochange){
                            cochangeSet.add(key);//共同变更次数超过cochange的微服务名的集合
                        }
                    }
                    if(cochangeSet.size()>change){
                        resultDetail.put(name,cochangeSet);//每个微服务的满足ui的微服务集合
                        //只将满足三层要求的存在UI的微服务的具体记录存入resultDetail，其他不存在UI的无记录
                    }
                }
            }
        }
        for(String key:microservices.microserviceNames()){
            if(resultDetail.containsKey(key)){
                int value=resultDetail.get(key).size();
                element.put(key,Double.valueOf(value));
            }else{
                element.put(key,0.0);
            }
        }
       /* for(String key:resultDetail.keySet()){
            int value=resultDetail.get(key).size();
            element.put(key,Double.valueOf(value));
        }*/
        return element;
    }


    @Override
    public OutputInfos execute(InputInfos inputInfos, Context context) throws ActionExecuteFailedException {
        return null;
    }
}

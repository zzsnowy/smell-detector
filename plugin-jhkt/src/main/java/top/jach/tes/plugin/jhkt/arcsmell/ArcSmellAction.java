package top.jach.tes.plugin.jhkt.arcsmell;

import top.jach.tes.core.api.domain.action.Action;
import top.jach.tes.core.api.domain.action.InputInfos;
import top.jach.tes.core.api.domain.action.OutputInfos;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.api.domain.action.DefaultOutputInfos;
import top.jach.tes.core.api.domain.meta.Meta;
import top.jach.tes.core.api.exception.ActionExecuteFailedException;
import top.jach.tes.core.impl.domain.element.Element;
import top.jach.tes.core.impl.domain.element.ElementsInfo;
import top.jach.tes.core.impl.domain.element.ElementsValue;
import top.jach.tes.core.impl.domain.info.value.StringInfo;
import top.jach.tes.core.impl.domain.meta.InfoField;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.plugin.jhkt.arcsmell.cyclic.CyclicAction;
import top.jach.tes.plugin.jhkt.arcsmell.hublink.HublinkAction;
import top.jach.tes.plugin.jhkt.arcsmell.mv.MvAction;
import top.jach.tes.plugin.jhkt.microservice.MicroservicesInfo;
import top.jach.tes.plugin.tes.code.git.commit.GitCommitsInfo;

import java.util.Arrays;
import java.util.Map;

public class ArcSmellAction implements Action {
    public static final String Elements_INFO = "elements_info";
    public static final String PAIR_RELATIONS_INFO = "PairRelationsInfo";
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
        return () -> Arrays.asList(
                InfoField.createField(Elements_INFO).setInfoClass(ElementsInfo.class),
                InfoField.createField(PAIR_RELATIONS_INFO).setInfoClass(PairRelationsInfo.class)
        );
    }

    @Override
    public OutputInfos execute(InputInfos inputInfos, Context context) throws ActionExecuteFailedException {
        ElementsInfo elementsInfo = inputInfos.getInfo(Elements_INFO, ElementsInfo.class);//元素
        PairRelationsInfo pairRelationsInfo = inputInfos.getInfo(PAIR_RELATIONS_INFO, PairRelationsInfo.class);
        GitCommitsInfo gitCommitsInfo=inputInfos.getInfo(GIT_COMMITS_INFO,GitCommitsInfo.class);
        MicroservicesInfo microservicesInfo=inputInfos.getInfo(MICROSERVICE_INFO,MicroservicesInfo.class);
        //元素之间的关系

        ArcSmellsInfo arcSmellsInfo = new ArcSmellsInfo();

        Action cyclicAction = new CyclicAction();
        Action hublinkAction = new HublinkAction();
        Action mvAction=new MvAction();

        inputInfos.put(CyclicAction.Elements_INFO, elementsInfo);
        inputInfos.put(CyclicAction.PAIR_RELATIONS_INFO, pairRelationsInfo);
//        OutputInfos outputInfos = cyclicAction.execute(inputInfos, context);
//        ElementsValue cyclicSmells = outputInfos.getFirstByInfoClass(ElementsValue.class);
//
       /* inputInfos.put(MvAction.MICROSERVICE_INFO, microservicesInfo);
        inputInfos.put(MvAction.GIT_COMMITS_INFO,gitCommitsInfo);
        outputInfos=mvAction.execute(inputInfos,context);
        ElementsValue mvSmells=outputInfos.getFirstByInfoClass(ElementsValue.class);*/


        inputInfos.put(HublinkAction.Elements_INFO, elementsInfo);
        inputInfos.put(HublinkAction.PAIR_RELATIONS_INFO, pairRelationsInfo);
        OutputInfos outputInfos = hublinkAction.execute(inputInfos, context);
        ElementsValue hublinkSmells = outputInfos.getFirstByInfoClassAndName(ElementsValue.class, HublinkAction.HUBLINK_IN_AND_OUT);
        ElementsValue hublinkSmellsForIn = outputInfos.getFirstByInfoClassAndName(ElementsValue.class, HublinkAction.HUBLINK_IN);
        ElementsValue hublinkSmellsForOut = outputInfos.getFirstByInfoClassAndName(ElementsValue.class, HublinkAction.HUBLINK__OUT);

        /*for (Map.Entry<String, Double> entry:
                cyclicSmells.getValueMap().entrySet()) {
            ArcSmell arcSmell = arcSmellsInfo.find(entry.getKey());
            if(arcSmell == null){
                arcSmell = new ArcSmell();
                arcSmellsInfo.put(entry.getKey(), arcSmell);
            }
            arcSmell.setCyclic(entry.getValue().longValue());
        }*/
       /* for(Map.Entry<String,Double> entry:
                 mvSmells.getValueMap().entrySet()){
            ArcSmell arcSmell=arcSmellsInfo.find(entry.getKey());
            if(arcSmell==null){
                arcSmell=new ArcSmell();
                arcSmellsInfo.put(entry.getKey(),arcSmell);
            }
            arcSmell.setMv(entry.getValue().longValue());
        }*/
        for (Map.Entry<String, Double> entry:
                hublinkSmells.getValueMap().entrySet()) {
            ArcSmell arcSmell = arcSmellsInfo.find(entry.getKey());
            if(arcSmell == null){
                arcSmell = new ArcSmell();
                arcSmellsInfo.put(entry.getKey(), arcSmell);
            }
            arcSmell.setHublink(entry.getValue().longValue());
            arcSmell.setCyclic(0l);
        }
        for (Map.Entry<String, Double> entry:
                hublinkSmellsForIn.getValueMap().entrySet()) {
            ArcSmell arcSmell = arcSmellsInfo.find(entry.getKey());
            if(arcSmell == null){
                arcSmell = new ArcSmell();
                arcSmellsInfo.put(entry.getKey(), arcSmell);
            }
            arcSmell.setHublinkForIn(entry.getValue().longValue());
        }
        for (Map.Entry<String, Double> entry:
                hublinkSmellsForOut.getValueMap().entrySet()) {
            ArcSmell arcSmell = arcSmellsInfo.find(entry.getKey());
            if(arcSmell == null){
                arcSmell = new ArcSmell();
                arcSmellsInfo.put(entry.getKey(), arcSmell);
            }
            arcSmell.setHublinkForOut(entry.getValue().longValue());
        }
        return DefaultOutputInfos.WithSaveFlag(arcSmellsInfo);
    }
}

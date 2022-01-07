package top.jach.tes.app.jhkt.chenjiali.result;

import lombok.Data;
import top.jach.tes.core.impl.domain.relation.PairRelation;
import top.jach.tes.plugin.jhkt.maintain.MainTain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author:AdminChen
 * @date:2020/8/31
 * @description:某个版本下依赖相关的所有异味的数据
 */
@Data
public class ResultForAllMs {
    List<String> microservice;
    List<PairRelation> relationWeight;
    List<PairRelation> relationNoWeight;
    Map<String, Double> hublike_weight = new HashMap<>();
    Map<String, Double> hublike_weight_in = new HashMap<>();
    Map<String, Double> hublike_weight_out = new HashMap<>();
    Map<String, Double> hublike_no_weight = new HashMap<>();
    Map<String, Double> hublike_no_weight_in = new HashMap<>();
    Map<String, Double> hublike_no_weight_out = new HashMap<>();
    Map<String, Double> hub_weight = new HashMap<>();
    Map<String, Double> hub_no_weight = new HashMap<>();
    Map<String, Double> cyclic = new HashMap<>();
    Map<String,Double> unstable_weight=new HashMap<>();
    Map<String,Double> unstable_no_weight=new HashMap<>();
   // Map<String, Double> undirectedCyclic = new HashMap<>();

    Map<String, Long> commitCount = new HashMap<>();
    Map<String, Long> bugCount = new HashMap<>();
    Map<String, Long> commitAddLineCount = new HashMap<>();
    Map<String, Long> commitDeleteLineCount = new HashMap<>();
    Map<String, Long> commitLineCount = new HashMap<>();
    Map<String, Double> CommitOverlapRatio = new HashMap<>();
    Map<String, Double> CommitFilesetOverlapRatio = new HashMap<>();
    Map<String, Double> PairwiseCommitterOverlap = new HashMap<>();


    public void addMainTain(MainTain mainTain){
        commitCount.put(mainTain.getElementName(), mainTain.getCommitCount());
        bugCount.put(mainTain.getElementName(), mainTain.getBugCount());
        commitAddLineCount.put(mainTain.getElementName(), mainTain.getCommitAddLineCount());
        commitDeleteLineCount.put(mainTain.getElementName(), mainTain.getCommitDeleteLineCount());
        commitLineCount.put(mainTain.getElementName(), mainTain.getCommitAddLineCount()+mainTain.getCommitDeleteLineCount());
        CommitOverlapRatio.put(mainTain.getElementName(), mainTain.getCommitOverlapRatio());
        CommitFilesetOverlapRatio.put(mainTain.getElementName(), mainTain.getCommitFilesetOverlapRatio());
        PairwiseCommitterOverlap.put(mainTain.getElementName(), mainTain.getPairwiseCommitterOverlap());
    }
}

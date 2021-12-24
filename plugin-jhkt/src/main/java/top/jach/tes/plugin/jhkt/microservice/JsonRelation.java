package top.jach.tes.plugin.jhkt.microservice;

import lombok.Getter;
import lombok.Setter;
import top.jach.tes.core.impl.domain.relation.PairRelation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author:AdminChen
 * @date:2020/8/28
 * @description:
 */
@Getter
@Setter
public class JsonRelation {
    List<PairRelation> relations=new ArrayList<>();
    public JsonRelation(){}
    public JsonRelation(List<PairRelation> relations){
        this.relations=relations;
    }
}

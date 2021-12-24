package top.jach.tes.app.jhkt.chenjiali;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * @author:AdminChen
 * @date:2020/10/12
 * @description:
 */
@Getter
@Setter
public class JsonObjectForMT {
    int concerns;
    List<String> serviceNames;
    Map<String,List<Double>> serviceConcernProbabilities;

    public JsonObjectForMT(int concerns,List<String> serviceNames,Map<String,List<Double>> serviceConcernProbabilities){
        this.concerns=concerns;
        this.serviceNames=serviceNames;
        this.serviceConcernProbabilities=serviceConcernProbabilities;
    }
}

package top.jach.tes.app.jhkt.chenjiali.result;

import lombok.Data;
import top.jach.tes.app.jhkt.lijiaqi.result.ResultForMs;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author:AdminChen
 * @date:2020/8/31
 * @description:
 */
@Data
public class ResultAll {
    //String为version字符串
    Map<String, ResultForAllMs> results = new LinkedHashMap<>();

    public ResultAll() {
    }

    public void put(String version, ResultForAllMs resultForMs){
        results.put(version, resultForMs);
    }

    public Set<String> allMicroservices(){
        Set<String> microservices = new HashSet<>();
        for (ResultForAllMs rfm :
                results.values()) {
            microservices.addAll(rfm.microservice);
        }
        microservices.remove("x_1f");
        return microservices;
    }
}

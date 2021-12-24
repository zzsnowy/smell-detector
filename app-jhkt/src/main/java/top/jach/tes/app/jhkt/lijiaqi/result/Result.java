package top.jach.tes.app.jhkt.lijiaqi.result;

import lombok.Getter;

import java.util.*;

@Getter
public class Result {
    //String为version字符串
    Map<String, ResultForMs> results = new LinkedHashMap<>();

    public Result() {
    }

    public void put(String version, ResultForMs resultForMs){
        results.put(version, resultForMs);
    }

    public Set<String> allMicroservices(){
        Set<String> microservices = new HashSet<>();
        for (ResultForMs rfm :
                results.values()) {
            microservices.addAll(rfm.microservice);
        }
        microservices.remove("x_1f");
        return microservices;
    }
}

package top.jach.tes.plugin.jhkt.arcsmell.concernOverload;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author:AdminChen
 * @date:2020/11/13
 * @description:
 */
@Getter
@Setter
public class JsonObjectForMT1 {
    int allConcerns;
    Map<String,Integer> microtopicCount;
    Map<String,Map<String,Double>> microTopics;

    public JsonObjectForMT1(int i,Map<String,Integer> microtopicCount,Map<String,Map<String,Double>> microTopics){
        this.allConcerns=i;
        this.microtopicCount=microtopicCount;
        this.microTopics=microTopics;
    }
}

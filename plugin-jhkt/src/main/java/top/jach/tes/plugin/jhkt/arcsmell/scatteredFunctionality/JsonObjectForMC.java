package top.jach.tes.plugin.jhkt.arcsmell.scatteredFunctionality;

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
public class JsonObjectForMC {
   // private int allConcerns;
    //private Map<String,Integer> microtopicCount;
    private Map<String, List<String>> microTopics;

    public JsonObjectForMC(Map<String,List<String>> microTopics){
        this.microTopics=microTopics;
    }

}

package top.jach.tes.app.jhkt.chenjiali;

import java.util.Map;

/**
 * @author:AdminChen
 * @date:2020/9/25
 * @description:
 */
public class JsonObjectForMF {
    Map<String, Map<String,Double>> microFiile;

    public JsonObjectForMF(Map<String, Map<String,Double>> microFiile){
        this.microFiile=microFiile;
    }
}

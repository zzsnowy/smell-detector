package top.jach.tes.plugin.jhkt.arcsmell.concernOverload;

import lombok.Getter;

import java.util.Map;

/**
 * @author:AdminChen
 * @date:2020/11/13
 * @description:
 */
@Getter
public class JsonObjectForMM {
    public Map<String,String> microMap;
    public JsonObjectForMM(Map<String,String> mp){
        this.microMap=mp;
    }
}

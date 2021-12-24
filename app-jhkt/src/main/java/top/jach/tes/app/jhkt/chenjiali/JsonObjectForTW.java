package top.jach.tes.app.jhkt.chenjiali;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author:AdminChen
 * @date:2020/10/12
 * @description:
 */
@Getter
@Setter
public class JsonObjectForTW {
    Map<String, Map<String,Double>> topic_words;

    public JsonObjectForTW(Map<String,Map<String,Double>> topic_words){
        this.topic_words=topic_words;
    }
}

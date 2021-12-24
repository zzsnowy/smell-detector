package top.jach.tes.app.jhkt.chenjiali.result;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * @author:AdminChen
 * @date:2020/10/10
 * @description:关于CO和SF的结果输出格式
 */
@Getter
@Setter
public class OutputDataStruct {
    String version;
    LinkedHashMap<String,Integer> component_smellcount=new LinkedHashMap<>();
    LinkedHashMap<String, ArrayList<String>> detail_info=new LinkedHashMap<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LinkedHashMap<String, Integer> getComponent_smellcount() {
        return component_smellcount;
    }

    public void setComponent_smellcount(LinkedHashMap<String, Integer> component_smellcount) {
        this.component_smellcount = component_smellcount;
    }

    public LinkedHashMap<String, ArrayList<String>> getDetail_info() {
        return detail_info;
    }

    public void setDetail_info(LinkedHashMap<String, ArrayList<String>> detail_info) {
        this.detail_info = detail_info;
    }
}

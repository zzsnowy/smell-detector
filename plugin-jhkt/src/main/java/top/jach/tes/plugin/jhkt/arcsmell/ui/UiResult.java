package top.jach.tes.plugin.jhkt.arcsmell.ui;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author:AdminChen
 * @date:2020/8/31
 * @description:用于存储每个微服务在UI上的输出，主要包含每个微服务改变影响了哪些文件，影响了哪些微服务
 */
@Getter
@Setter
public class UiResult {
    private String microservice;
    private Set<String> msFiles;//每个微服务内包含的文件
    private Map<String,Integer> UiFiles;//这个microservice改变影响了哪些文件，影响了各个文件多少次
    private Map<String,Integer> UiMicroservices;//这个microservice改变影响了哪些微服务，来源于UiFiles

    public UiResult(){}
    public UiResult(String microservice,Map<String,Integer> resultFiles,Map<String,Integer> resultMicroservices){
        this.microservice=microservice;
        this.UiFiles=resultFiles;
        this.UiMicroservices=resultMicroservices;
    }

}

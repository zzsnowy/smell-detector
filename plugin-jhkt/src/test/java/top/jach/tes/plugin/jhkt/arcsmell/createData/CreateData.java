package top.jach.tes.plugin.jhkt.arcsmell.createData;

import top.jach.tes.plugin.jhkt.microservice.Microservice;
import top.jach.tes.plugin.jhkt.microservice.MicroservicesInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author:AdminChen
 * @date:2020/6/28
 * @description:用于创建测试所需的数据
 */
public class CreateData {

    public static List<Microservice> createMicroservice(){
        List<Microservice> res=new ArrayList<>();
        for(int i=0;i<21;i++){

        }

        return res;
    }
    public static MicroservicesInfo createMicroserviceInfo(){
        MicroservicesInfo info=new MicroservicesInfo();
        info.setReposId(1001L);
        info.setRepoName("repos01");
        info.setVersion("versions01");
        return info;
    }
}

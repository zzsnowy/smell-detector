package top.jach.tes.plugin.jhkt.microservice;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import top.jach.tes.core.api.domain.info.Info;
import top.jach.tes.core.api.domain.info.InfoProfile;
import top.jach.tes.core.impl.domain.element.Element;
import top.jach.tes.core.impl.domain.element.ElementsInfo;
import top.jach.tes.core.impl.domain.relation.PairRelation;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.plugin.tes.code.repo.WithRepo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Getter
@Setter
@ToString(callSuper = true)
public class MicroservicesInfo extends ElementsInfo<Microservice> implements WithRepo {
    Long reposId;

    String repoName;

    String version;

    private List<Microservice> microservices = new ArrayList<>();

    public static MicroservicesInfo createInfo(){
        MicroservicesInfo info = new MicroservicesInfo();
        info.initBuild();
        return info;
    }

    public List<String> microserviceNames(){
        List<String> names = new ArrayList<>();
        for (Microservice m :
                this) {
            names.add(m.getElementName());
        }
        return names;
    }

    public static MicroservicesInfo createInfo(Info... infos){
        MicroservicesInfo microservices = createInfo();
        for (Info info :
                infos) {
            if (info instanceof MicroservicesInfo) {
                MicroservicesInfo microservicesInfo = (MicroservicesInfo) info;
                for (Microservice m :
                        microservicesInfo.getMicroservices()) {
                    String[] paths = microservicesInfo.getRepoName().split("/");
                    m.setElementName(StringUtils.strip(paths[paths.length-1]+"/"+m.getPath(), "/"));
                    microservices.addMicroservice(m);
                }
            }
        }
        return microservices;
    }

    public static MicroservicesInfo createInfoByExcludeMicroservice(MicroservicesInfo mInfo, String... excludeNames){
        MicroservicesInfo microservices = createInfo();
        microservices.setName(mInfo.getName());
        microservices.setReposId(mInfo.getReposId())
                .setRepoName(mInfo.getRepoName())
                .setVersion(mInfo.getVersion())
                .setDesc(mInfo.getDesc());

        for (Microservice m :
                mInfo.getMicroservices()) {
            boolean exclude = false;
            for (String name :
                    excludeNames) {
                if (StringUtils.isNoneBlank(name) && name.equals(m.getElementName())) {
                    exclude = true;
                    System.out.println("Exclude microservice: "+name);
                    break;
                }
            }
            if(!exclude){
                microservices.addMicroservice(m);
            }
        }
        return microservices;
    }

    public static Map<PairRelation,Double> relationCount(String path) throws IOException {
        Map<PairRelation,Double> res=new HashMap<>();
        Gson gson=new Gson();
        String str= FileUtils.readFileToString(new File(path),"utf8");
        JsonRelation jsonRelation=gson.fromJson(str,JsonRelation.class);
        for(PairRelation pr:jsonRelation.getRelations()){
            if(res.isEmpty()){
                res.put(pr,1.0);
                continue;
            }
            boolean flag=true;
            for(PairRelation par:res.keySet()){
                if(par.getSourceName().equals(pr.getSourceName())&&par.getTargetName().equals(pr.getTargetName())){
                    res.put(par,res.get(par)+1.0);
                    flag=false;
                    break;
                }
            }
            if(flag){
                res.put(pr,1.0);
            }
        }
        return res;
    }
    //最新的
    public PairRelationsInfo callRelationsInfoByTopicWithJsonNew(boolean weight,List<PairRelation> rels) throws IOException {
        PairRelationsInfo pairRelations = PairRelationsInfo.createInfo().setSourceElementsInfo(InfoProfile.createFromInfo(this));
       for(PairRelation pr:rels){
           PairRelation prr=new PairRelation(pr.getSourceName(),pr.getTargetName());
           if(weight){
               prr.setValue(pr.getValue());
           }else{
               prr.setValue(1d);
           }
           pairRelations.addRelation(prr);
       }
        return pairRelations;
    }

    public PairRelationsInfo callRelationsInfoByTopicWithJson(boolean weight,String path) throws IOException {
        PairRelationsInfo pairRelations = PairRelationsInfo.createInfo().setSourceElementsInfo(InfoProfile.createFromInfo(this));
        Map<PairRelation,Double> relationDouble=relationCount(path);//所有relation带权重，所有版本relation的并集
        List<PairRelation> tmp=new ArrayList<>();
        for (Microservice microservice :
                this.microservices) {
            for (String pubTopic :
                    microservice.getPubTopics()) {
                List<Microservice> subMicroservices = getMicroserviceBySubTopic(pubTopic);
                for (Microservice subMicroservice :
                        subMicroservices) {
                    if(microservice.getElementName().equals(subMicroservice.getElementName())){
                        continue;
                    }
                    PairRelation prl=new PairRelation(microservice.getElementName(), subMicroservice.getElementName());
                    prl.setValue(1d);
                    tmp.add(prl);
                }
            }
        }
        for(PairRelation prr:tmp){
            double val=0;
            boolean isExist=false;
            for(PairRelation pp:relationDouble.keySet()){
                if(pp.getSourceName().equals(prr.getSourceName())&&pp.getTargetName().equals(prr.getTargetName())){
                    val=relationDouble.get(pp);
                    isExist=true;
                    break;
                }
            }
            if(isExist){
                if(weight){
                    prr.setValue(val);
                }else{
                    prr.setValue(1.0);
                }
                pairRelations.addRelation(prr);
            }
        }
        return pairRelations;
    }
    public PairRelationsInfo callRelationsInfoByTopic(boolean weight){
        PairRelationsInfo pairRelations = PairRelationsInfo.createInfo().setSourceElementsInfo(InfoProfile.createFromInfo(this));
      //  List<PairRelation> relationList=new ArrayList<>();//为了去重
        for (Microservice microservice :
                this.microservices) {
            if(weight){
                Set<String> concurrent=microservice.getPubTopics();
                List<Microservice> subMicroservices = getMicroserviceBySubTopics(concurrent);//只要二者存在交集，就说明两个微服务之间存在联系
                for (Microservice subMicroservice :
                        subMicroservices) {//找到与当前遍历的微服务存在交集的微服务，一一计算两微服务之间的关系权重
                        double value=0;
                        //这里要为to,sto,stoo来new一个内存，否则to等直接指向microservice的源地址改变了microservice的原数据
                        Set<String> to=new HashSet<>(microservice.getPubTopics());
                        Set<String> sto=new HashSet<>(subMicroservice.getSubTopics());
                        Map<String,Integer> stoo=new HashMap<>(subMicroservice.getSubTopicOneOf());
                        sto.retainAll(to);
                        for(String str:sto){
                            if(stoo.containsKey(str)){
                                if(stoo.get(str)>0){
                                    value+=stoo.get(str);
                                }
                                else if(stoo.get(str)==0){
                                    value+=1;
                                }
                            }
                            else{
                                value+=1;
                            }
                        }
                        PairRelation prl=new PairRelation(microservice.getElementName(),subMicroservice.getElementName());
                        prl.setValue(value);
                        pairRelations.addRelation(prl);
                }
            }
            else{

                for (String pubTopic :
                        microservice.getPubTopics()) {
                    List<Microservice> subMicroservices = getMicroserviceBySubTopic(pubTopic);
                    for (Microservice subMicroservice :
                            subMicroservices) {
                        if(microservice.getElementName().equals(subMicroservice.getElementName())){
                            continue;
                        }
                        PairRelation prl=new PairRelation(microservice.getElementName(), subMicroservice.getElementName());
                        prl.setValue(1d);
                        pairRelations.addRelation(prl);
                    }
                }
            }
        }
        return pairRelations;
    }


    public List<Microservice> microservicesForRepo(String repoName){
        if(StringUtils.isBlank(repoName)){
            return getMicroservices();
        }
        List<Microservice> list = new ArrayList<>();
        for (Microservice m :
                getMicroservices()) {
            if (repoName.equals(m.getRepoName())){
                list.add(m);
            }
        }
        return list;
    }

    public void noRelationsByTopic(){
        for (Microservice microservice :
                this.microservices) {
            for (String pubTopic :
                    microservice.getPubTopics()) {
                List<Microservice> subMicroservices = getMicroserviceBySubTopic(pubTopic);
                if(subMicroservices.size()==0){
                    System.out.println(String.format("%s %s", microservice.getElementName(), pubTopic));
                }
            }
        }
    }

    public List<Microservice> getMicroserviceBySubTopic(String subTopic){
        List<Microservice> microservices = new ArrayList<>();
        for (Microservice microservice :
                this.microservices) {
            if (microservice.getSubTopics().contains(subTopic)) {
                microservices.add(microservice);
            }
        };
        return microservices;
    }




    public List<Microservice> getMicroserviceBySubTopics(Set<String> subTopics){
        List<Microservice> microservices = new ArrayList<>();
        for (Microservice microservice :
                this.microservices) {
            Set<String> requl=new HashSet<>(new HashSet<>(subTopics));
            requl.retainAll(microservice.getSubTopics());
            if (requl.size()>0) {
                microservices.add(microservice);
            }
        }
        return microservices;
    }


    public MicroservicesInfo addMicroservice(Microservice... microservices){
        this.microservices.addAll(Arrays.asList(microservices));
        return this;
    }

    public MicroservicesInfo setReposId(Long reposId) {
        this.reposId = reposId;
        return this;
    }

    public MicroservicesInfo setRepoName(String repoName) {
        this.repoName = repoName;
        return this;
    }

    public MicroservicesInfo setVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public Iterator<Microservice> iterator() {
        return microservices.iterator();
    }

    @Override
    public Microservice getElementByName(String elementName) {
        for (Microservice microservice :
                microservices) {
            if (microservice.getElementName().equals(elementName)) {
                return microservice;
            }
        }
        return null;
    }
}

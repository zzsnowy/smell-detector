package top.jach.tes.plugin.jhkt.microservice;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import top.jach.tes.core.api.domain.info.Info;
import top.jach.tes.core.api.domain.info.InfoProfile;
import top.jach.tes.core.impl.domain.element.ElementsInfo;
import top.jach.tes.core.impl.domain.relation.PairRelation;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.plugin.tes.code.repo.WithRepo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Getter
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

    public PairRelationsInfo callRelationsInfoByTopic(){
        PairRelationsInfo pairRelations = PairRelationsInfo.createInfo().setSourceElementsInfo(InfoProfile.createFromInfo(this));
        for (Microservice microservice :
                this.microservices) {
            for (String pubTopic :
                    microservice.getPubTopics()) {
                List<Microservice> subMicroservices = getMicroserviceBySubTopic(pubTopic);
                for (Microservice subMicroservice :
                        subMicroservices) {
                    pairRelations.addRelation(new PairRelation(microservice.getElementName(), subMicroservice.getElementName()));
                }
            }
        }
        return pairRelations;
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

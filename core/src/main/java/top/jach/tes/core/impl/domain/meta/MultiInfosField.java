package top.jach.tes.core.impl.domain.meta;

import top.jach.tes.core.api.domain.info.Info;
import top.jach.tes.core.impl.domain.info.InfoOfInfo;
import top.jach.tes.core.api.factory.InfoRepositoryFactory;

import java.util.Arrays;
import java.util.List;

public abstract class MultiInfosField<I extends Info> extends FieldWithName<List<Info>>{

    @Override
    public Class<List<Info>> getInputClass() {
        return null;
    }

    @Override
    public Info getInfo(List<Info> input, InfoRepositoryFactory infoRepositoryFactory) {
        InfoOfInfo<I> infoOfInfo =  InfoOfInfo.createInfoOfInfo();
        for (Info info :
                input) {
            List<Info> infoList = infoRepositoryFactory.getRepository(info.getInfoClass())
                    .queryDetailsByInfoIds(Arrays.asList(info.getId()));
            Info i = infoList.get(0);
            infoOfInfo.addInfos((I) i);
        }
        return infoOfInfo;
    }
}

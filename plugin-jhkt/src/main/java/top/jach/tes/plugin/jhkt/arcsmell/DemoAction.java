package top.jach.tes.plugin.jhkt.arcsmell;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.ArrayUtils;
import top.jach.tes.core.api.domain.action.Action;
import top.jach.tes.core.api.domain.action.InputInfos;
import top.jach.tes.core.api.domain.action.OutputInfos;
import top.jach.tes.core.api.domain.context.Context;
import top.jach.tes.core.api.domain.action.DefaultOutputInfos;
import top.jach.tes.core.api.domain.meta.Meta;
import top.jach.tes.core.api.exception.ActionExecuteFailedException;
import top.jach.tes.core.impl.domain.element.Element;
import top.jach.tes.core.impl.domain.element.ElementsInfo;
import top.jach.tes.core.impl.domain.info.value.StringInfo;
import top.jach.tes.core.impl.domain.meta.InfoField;
import top.jach.tes.core.impl.domain.relation.PairRelation;
import top.jach.tes.core.impl.domain.relation.PairRelationsInfo;
import top.jach.tes.core.impl.domain.relation.Relation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DemoAction implements Action {
    public static final String Elements_INFO = "elements_info";
    public static final String PAIR_RELATIONS_INFO = "PairRelationsInfo";
    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDesc() {
        return null;
    }

    @Override
    public Meta getInputMeta() {
        return () -> Arrays.asList(
                InfoField.createField(Elements_INFO).setInfoClass(ElementsInfo.class),
                InfoField.createField(PAIR_RELATIONS_INFO).setInfoClass(PairRelationsInfo.class)
        );
    }

    @Override
    public OutputInfos execute(InputInfos inputInfos, Context context) throws ActionExecuteFailedException {
        ElementsInfo<Element> elementsInfo = inputInfos.getInfo(Elements_INFO, ElementsInfo.class);
        PairRelationsInfo pairRelationsInfo = inputInfos.getInfo(PAIR_RELATIONS_INFO, PairRelationsInfo.class);

        List<Element> elements = Lists.newArrayList(elementsInfo.iterator());
        List<PairRelation> relations = Lists.newArrayList(pairRelationsInfo.getRelations().iterator());


        context.Logger().info(elementsInfo.toString());
        context.Logger().info(pairRelationsInfo.toString());

        return DefaultOutputInfos.WithSaveFlag(StringInfo.createInfo(elementsInfo.toString()));
    }
}

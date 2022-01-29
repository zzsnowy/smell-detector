import lda.Estimator;
import lda.Inferencer;
import lda.LDAOption;
import lda.Model;
import org.junit.Test;

import java.util.Arrays;

public class LDATest {

    //第一个test用来训练
    @Test
    public void test1() {
        LDAOption option = new LDAOption();

        option.est = true;
        option.inf = false;

        option.alpha = 0.05;//控制语料库主题的密度，一般设置成主题数的倒数1/K
        option.beta = 0.1;//尽量设小一些，0.01等
        option.K = 20;//经验来确定的值  设置不同k值比较主题模型效果好坏来确定好的k值。见Model.perplexity()方法
        option.niters = 1000;//迭代次数一般1000-2000  从一开始到最终收敛过程中的迭代次数，值越大正确性越高，运行时间越长

        //训练结果输出文档存储的目录
        option.dir = "D:\\FILE\\work\\codeTopics\\codeTopics\\src\\test\\example";
        option.dfile = "shop.dat";//上一步分词、清洗后的所有文件的词库作为LDA输入
        option.savestep = 100;
        option.twords = 50;//输出的tword中用前50个概率大的词代表主题的内容

        Estimator estimator = new Estimator();
        estimator.init(option);
        estimator.estimate();

        Model model = estimator.trnModel;

        System.out.println();
        System.out.println(model.perplexity());

    }

    //第二个test用来保存第一个test训练的结果，第二个test的输出才是postprocess要的数据
    @Test
    public void test2() {
        LDAOption option = new LDAOption();

        option.est = false;
        option.inf = true;
        option.modelName = "model-final";//获取最后一轮训练结束得到的模型final结尾的模型
        option.niters = 1000;
        option.dir = "D:\\FILE\\work\\codeTopics\\codeTopics\\src\\test\\example";

        Inferencer inferencer = new Inferencer();

        inferencer.init(option);

        System.out.println(inferencer.trnModel.perplexity());
        System.out.println(inferencer.trnModel.printTopics(20));

        //用获取的模型对测试文档（newString[]那个）测试，输出该文档有哪些topic
        Model newModel = inferencer.inference(new String[]{"sock imag tag price"}); //新的文件，经过分词等预处理。
        // 每一个字符串是一个文件，每个文件不同词用空格隔开，作为已训练好的LDA模型的输入，推断新文档的主题构成
        System.out.println(Arrays.deepToString(newModel.z));
    }

}

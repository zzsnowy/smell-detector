package top.jach.tes.app.jhkt;

import org.junit.Test;
import top.jach.tes.app.jhkt.codetopics.preprocess.Corpus;
import top.jach.tes.app.jhkt.codetopics.preprocess.PreProcessMethods;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static top.jach.tes.app.jhkt.codetopics.preprocess.CommonStopWordList.myStopWords;

public class PreProcessTest {

    @Test
    public void codeFileProcessTest() {
        List<String> allPath = new ArrayList<String>() {{
//            add("D:\\Development\\idea_projects\\microservices-platform-master\\zlt-monitor\\sc-admin");
            add("D:\\Development\\idea_projects\\microservices-platform-master\\zlt-transaction\\txlcn-tm");
        }};

        // 扫描目标路径下的所有代码文件（文件树的DFS遍历）
        for(String mpath : allPath){
            FileFilter filter = pathname -> (pathname.getName().endsWith(".go")
                    || pathname.getName().endsWith(".java")
                    || pathname.getName().endsWith(".c")
                    || pathname.getName().endsWith(".cpp")
                    || pathname.isDirectory())
                    // 增加过滤测试文件的规则
                    && !pathname.getName().toLowerCase(Locale.ROOT).endsWith("test");

            Corpus corpus = new Corpus();
            System.out.println("=========current path: " + mpath);
            corpus.init(mpath, filter);
            //输出所有源代码文件的绝对路径
            System.out.println("所有的源代码文件：" + corpus.documents.size());
            for (String file : corpus.fileNames) {
                System.out.println(file);
            }


            System.out.println("分词后：");
            for (int i = 0; i < corpus.documents.size(); i++) {
                System.out.println("file " + i + ": " + corpus.documents.get(i).words);
            }
            PreProcessMethods.splitIdentifier(corpus);
            System.out.println("驼峰拆分后:");
            for (int i = 0; i < corpus.documents.size(); i++) {
                System.out.println("file " + i + ": " + corpus.documents.get(i).words);
            }
            PreProcessMethods.toLowerCase(corpus);
            System.out.println("转小写后:    ");
            for (int i = 0; i < corpus.documents.size(); i++) {
                System.out.println("file " + i + ": " + corpus.documents.get(i).words);
            }
            PreProcessMethods.removeStopWords(corpus, myStopWords);
            System.out.println("去停用词后:    ");
            for (int i = 0; i < corpus.documents.size(); i++) {
                System.out.println("file " + i + ": " + corpus.documents.get(i).words);
            }
            PreProcessMethods.filtering(corpus);
            System.out.println("去除杂质词后:    ");
            for (int i = 0; i < corpus.documents.size(); i++) {
                System.out.println("file " + i + ": " + corpus.documents.get(i).words);
            }
            PreProcessMethods.tf_idf(corpus);
            System.out.println("tf_idf筛选后:    ");
            for (int i = 0; i < corpus.documents.size(); i++) {
                System.out.println("file " + i + ": " + corpus.documents.get(i).words);
            }
            PreProcessMethods.stemming(corpus);
            System.out.println("词干提取后:    ");
            for (int i = 0; i < corpus.documents.size(); i++) {
                System.out.println("file " + i + ": " + corpus.documents.get(i).words);
            }
        }
    }
}

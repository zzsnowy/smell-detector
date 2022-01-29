package preprocess;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import static preprocess.fileUtil.readFile;

/*
* 用于存放项目代码文件下所有用自定义过滤器过滤出来的文件绝对路径集合+每个文件的内容（Document存放）的数据结构
* 最终所有的preprocess操作都是在读取项目文件赋值后的corpus对象上进行的。不断去除corpus中杂质内容，最终得到的
* corpus对象存储是就是第二阶段分析所需的数据源
* */
public class Corpus {
    /**
     * 保存文件顺序
     */
    public ArrayList<String> fileNames;

    /**
     * 所有文件对应document数据结构列表
     */
    public ArrayList<Document> documents;

    /**
     * 词袋默认初始化方法
     * @param fileNames
     * @param documents
     */
    public void init(ArrayList<String> fileNames, ArrayList<Document> documents) {
        this.fileNames = fileNames;
        this.documents = documents;
    }

    /**
     * 自定义的init方法
     * @param path
     * @param filter
     */
    public void init(String path, FileFilter filter) {
        fileNames = new ArrayList<>();
        documents = new ArrayList<>();

        //在scan里传入多个Path，将不同目录下扫描得到的文件都整合到一个corpus中去
        scan(new File(path), filter); //此方法就是给Corpus的fileNames赋值的
        //接下来遍历上一步扫描到的所有.java .go结尾的文件绝对路径集合fileNames,按行读取每个文件的内容
        //每个文件内容string赋值给Document类的rawStr属性，没有给words赋值。每个document加入list中
        for (int i = 0; i < fileNames.size(); i++) {
            String filename = fileNames.get(i);
            Document document = new Document();
            document.rawStr = readFile(filename);
            document.words = PreProcessMethods.tokenize(document.rawStr);
            documents.add(document);
//            System.out.println("tokenize " + filename);
//            System.out.println(document.words);
        }
    }

    public void init(List<String>paths, FileFilter filter) {
        fileNames = new ArrayList<String>();
        documents = new ArrayList<Document>();

        //在scan里传入多个Path，将不同目录下扫描得到的文件都整合到一个corpus中去
        for(String path:paths){
            scan(new File(path), filter);//此方法就是给Corpus的fileNames赋值的
            //接下来遍历上一步扫描到的所有.java .go结尾的文件绝对路径集合fileNames,按行读取每个文件的内容
            //每个文件内容string赋值给Document类的rawStr属性，没有给words赋值。每个document加入list中
            for (int i = 0; i < fileNames.size(); i++) {
                String filename = fileNames.get(i);
                Document document = new Document();
                //document.rawStr = readFile(filename);
                document.words=PreProcessMethods.tokenize(readFile(filename));
                documents.add(document);
            }
        }
    }

    /**
     * 递归扫描项目目录，获取所有的文件，赋值给filenames
     * @param file
     * @param filter
     */
    private void scan(File file, FileFilter filter) {
        //传入自定义过滤器filter，对file文件目录下的文件夹进行过滤，返回所有以.java，.go结尾的文件及一些目录
        File[] files = file.listFiles(filter);

        for (File f : files) {
            //遍历过滤器过滤得到的file集合，若为正常文件（.java  .go结尾）则将文件绝对路径添加到fileNames
            //集合中，若不是文件而是directory目录则继续对该目录下递归调用scan继续过滤该目录下的file集合
            //最终fileNames中存放最初传入的file目录下所有文件绝对路径集合
            if (f.isFile()) {
                fileNames.add(f.getAbsolutePath());
            } else {
                scan(f, filter);
            }
        }
    }

    public int getFileIDByName(String filename) {
        return fileNames.indexOf(filename);
    }

    public String getFileNameByID(int id) {
        return fileNames.get(id);
    }

    public int wordFrequency(String word) {
        int frequency = 0;
        for (Document doc : this.documents) {
            for (String s : doc.words) {
                if (word.equals(s)) {
                    frequency++;
                }
            }
        }
        return frequency;
    }

    public Corpus combine(ArrayList<Corpus> corpuses){

        return null;
    }



}





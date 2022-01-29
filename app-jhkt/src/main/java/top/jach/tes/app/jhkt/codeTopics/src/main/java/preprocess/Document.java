package preprocess;

import java.util.List;

/*
* 用于存放每个文件  通过fileUtil读出来的文件内容字符串+文件内容字符串分词后的单词list的数据结构
* */
public class Document {
    public String rawStr;
    public List<String> words;

    @Override
    public String toString() {
        return "Document{" +
                "rawStr='" + rawStr + '\'' +
                ", words=" + words +
                '}';
    }
}

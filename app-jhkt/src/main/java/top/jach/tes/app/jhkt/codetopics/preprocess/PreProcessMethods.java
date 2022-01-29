package top.jach.tes.app.jhkt.codetopics.preprocess;

import org.tartarus.snowball.ext.englishStemmer;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
* 以静态方法的形式提供所有项目文件预处理的相关操作封装的方法，
* 这些方法直接在PreProcessTest中以类.方法的形式调用
* */
public class PreProcessMethods {

    //此方法传入某个文件所有内容合成一个字符串context，然后用replaceAll去除内容字符串一些干扰词
    //再分割成一个一个单词token存入words列表中，这个words就是给document对象的words赋值
    public static List<String> tokenize(String context) {
        List<String> words = new ArrayList<String>();
        //去除邮箱，url等；
        String code = context.replaceAll("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]", "");
        code = code.replaceAll("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*", "");

        code = code.replaceAll("import(\\sstatic)?\\s[\\S]+", "");

        code = code.replaceAll("package\\s[\\S]+", "");

        Pattern pattern = Pattern.compile("[A-Za-z_]+[A-Za-z0-9_]*");
        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            String token = matcher.group(); // 结果
            words.add(token);
        }
        return words;
    }

    //通过调用上面的tokenize方法得到每个文件处理分割后的单词列表words，并赋值给corpus中各个documents的words属性
    public static void tokenize(Corpus corpus) {
        for (Document doc : corpus.documents) {
            ArrayList<String> words = (ArrayList<String>) tokenize(doc.rawStr);
            doc.words = words;
        }
    }

    public static void removeStopWords(Corpus corpus, List<String> stopWordList) {
        for (Document doc : corpus.documents) {
            doc.words.removeAll(stopWordList);
        }
    }

    private static boolean isUnderlineCase(String token) {
        return token.contains("_");
    }

    private static boolean isCamelCase(String token) {
        //若字符串全都是大写字母返回false
        if (token.toUpperCase().equals(token)) return false;

        boolean flag = false;
        for (int i = 1; i < token.length() - 1; i++) {
            char c = token.charAt(i);
            //只要字符串中有一个大写字母就返回true
            if ((c - 'A') >= 0 && (c - 'Z') <= 0) {
                flag = true;
            }
        }
        return flag;
    }

    public static List<String> splitIdentifier(String word) {
        if (isCamelCase(word)) {//判断当前word是否是驼峰拼写法
            int firstDownCase = 1;
            int followingUpperCase;
            //找到word中第一个小写字母的字符的位置
            for (; firstDownCase < word.length(); firstDownCase++) {
                char c = word.charAt(firstDownCase);
                if ((c - 'a') >= 0 && (c - 'z') <= 0) {
                    break;
                }
            }
            //找到word中第一个小写字母之后的第一个大写字母字符的位置
            for (followingUpperCase = firstDownCase; followingUpperCase < word.length(); followingUpperCase++) {
                char c = word.charAt(followingUpperCase);
                if ((c - 'A') >= 0 && (c - 'Z') <= 0) {
                    break;
                }
            }

            if (firstDownCase == 1) {
                List list1 = splitIdentifier(word.substring(0, followingUpperCase));
                List list2 = splitIdentifier(word.substring(followingUpperCase));
                list1.addAll(list2);
                return list1;
            } else {
                List list1 = splitIdentifier(word.substring(0, firstDownCase - 1));
                List list2 = splitIdentifier(word.substring(firstDownCase - 1));
                list1.addAll(list2);
                return list1;
            }

        } else if (isUnderlineCase(word)) {//判断当前是否是a_b这种形式的拼写法
            List list = new ArrayList();

            for (String token : word.split("_")) {
                list.addAll(splitIdentifier(token));
            }
            return list;
        } else {
            List list = new ArrayList<String>();
            list.add(word);
            return list;
        }

    }

    public static void splitIdentifier(Corpus corpus) {
        for (Document doc : corpus.documents) {
            ArrayList<String> ret = new ArrayList<>();
            for (String s : doc.words) {
                ret.addAll(splitIdentifier(s));
            }
            doc.words = ret;
        }
    }


    public static String stemming(String word) {
        englishStemmer stemmer = new englishStemmer();
        stemmer.setCurrent(word);
        if (stemmer.stem()) {
            return stemmer.getCurrent();
        }
        return word;
    }

    //英文分词
    public static void stemming(Corpus corpus) {
        for (Document doc : corpus.documents) {
            ArrayList<String> ret = new ArrayList<>();
            for (String s : doc.words) {
                ret.add(stemming(s));
            }
            doc.words = ret;
        }
    }

    public static void toLowerCase(Corpus corpus) {
        for (Document doc : corpus.documents) {
            ArrayList<String> ret = new ArrayList<>();
            for (String s : doc.words) {
                ret.add(s.toLowerCase());
            }
            doc.words = ret;
        }
    }

    /**
     * 去除杂志词
     * @param corpus
     */
    public static void filtering(Corpus corpus){
        Map<String,Integer> wordfrequency = new HashMap<>();
        for(Document document : corpus.documents){
            for(String w : document.words){
                if(!wordfrequency.containsKey(w)){
                    wordfrequency.put(w, 0);
                }
                wordfrequency.put(w, wordfrequency.get(w)+1);
            }
        }

        int frequencyThreshold = 5;
        for (Document document : corpus.documents) {
            List<String> tmp = new ArrayList<>();
            for (String word : document.words) {
                // 1、过短的词
                if (word.length() <= 1) continue;
                // 2、数字或者小数
                if (word.matches("-?[0-9]+.？[0-9]*")) continue;
                // 3、在当前词袋中的词频过低，之前是按阈值10或者5来计算，这里改成按词袋规模的 * 0.05
                if (wordfrequency.get(word) <= frequencyThreshold) continue;
                tmp.add(word);
            }
            document.words = tmp;
        }
    }

    public static Map<String,Double> getDocCount(Corpus corpus){
        Map<String,Double> res=new HashMap<>();
        Set<String> wordset=new HashSet<>();
        for(Document doc:corpus.documents){
            wordset.addAll(doc.words);
        }
        for(String word:wordset){
            for(Document document:corpus.documents){
                if(document.words.contains(word)){
                    if(!res.containsKey(word)){
                        res.put(word,1.0);
                    } else {
                        res.put(word,res.get(word)+1.0);
                    }
                }
            }
        }
        return res;
    }

    /**
     * 对词袋的词进行tf_idf筛选
     * @param corpus
     */
    public static void tf_idf(Corpus corpus){
        Map<String, Double> docCount = getDocCount(corpus);
        for(Document document : corpus.documents){
            List<String> tmp = new ArrayList<>();
            // 计算该文档的词频
            Map<String,Double> docfrequency = new HashMap<>();
            if(document.words.size() == 0) continue;
            for (String word : document.words){
                if(!docfrequency.containsKey(word)) {
                    docfrequency.put(word, 0.0);
                }
                docfrequency.put(word, docfrequency.get(word) + 1.0);
            }
            // 计算每个单词对应的 tfidf值
            Map<String,Double> doctfidf = new HashMap<>();
            for(Map.Entry<String,Double> entry : docfrequency.entrySet()){
                double tf = entry.getValue() / document.words.size();
                double idf = Math.log((double) corpus.documents.size() / (1 + docCount.get(entry.getKey())));
                double tfidf = tf*idf;
                doctfidf.put(entry.getKey(), tfidf);
            }
            // 对所有的tfidf值进行排序，从小到大
            List<Double> tivalue = new ArrayList<>();
            for(Map.Entry<String,Double> en : doctfidf.entrySet()){
                tivalue.add(en.getValue());
            }
            Collections.sort(tivalue);
            // 阈值取前20%小的，即最终将排除20%最小的tfidf对应的单词
            int threshIndex = (int)Math.floor(tivalue.size() * 0.2) - 1;
            // 只有一个词的文件不过滤
            if (threshIndex >= 0) {
                double thresh = tivalue.get(threshIndex);
                for(String word : document.words){
                    double dd = doctfidf.get(word);
                    if(new BigDecimal(dd).compareTo(new BigDecimal(thresh)) > 0){
                        tmp.add(word);
                    }
                }
                document.words = tmp;
            }
        }
    }


}

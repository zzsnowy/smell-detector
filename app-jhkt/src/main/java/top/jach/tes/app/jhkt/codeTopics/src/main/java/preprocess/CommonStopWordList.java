package preprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommonStopWordList {
    public static final List<String> ENGLISH_STOP_WORDS = Arrays.asList("none","present","get","first",
            "very", "been", "don't", "about", "couldn't", "your", "during", "when", "haven't", "wasn't", "these", "her",
            "above", "because", "doesn't", "if", "you", "they", "between", "having", "hasn", "in", "mightn't", "myself",
            "them", "is", "it", "being", "then", "yourselves", "am", "shouldn", "an", "each", "weren't", "himself",
            "itself", "as", "at", "re", "other", "don", "be", "hasn't", "against", "ain", "mustn't", "isn't", "our",
            "ourselves", "hadn't", "out", "mightn", "into", "how", "should've", "same", "are", "too", "does", "by",
            "whom", "have", "she's", "where", "wouldn", "you've", "after", "shouldn't", "so", "a", "wouldn't", "d",
            "more", "i", "m", "off", "o", "the", "such", "s", "hadn", "t", "shan", "y", "to", "under", "yours", "did",
            "but", "through", "ll", "theirs", "won't", "before", "isn", "own", "had", "do", "while", "him", "down",
            "couldn", "that", "wasn", "ours", "his", "ma", "than", "won", "me", "only", "should", "few", "yourself",
            "from", "has", "up", "those", "which", "all", "doesn", "you'd", "you'll", "below", "needn", "it's", "didn't",
            "this", "its", "my", "both", "ve", "most", "she", "weren", "mustn", "once", "were", "that'll", "aren",
            "aren't", "herself", "who", "here", "needn't", "some", "no", "doing", "for", "their", "why", "shan't",
            "we", "hers", "nor", "can", "not", "and", "you're", "themselves", "of", "now", "just", "on", "over", "didn",
            "haven", "or", "will", "again", "was", "any", "with", "what", "there", "until", "further", "he");
    public static final List<String> GO_KEY_WORDS = Arrays.asList("struct", "defer", "select", "const", "import", "for",
            "range", "interface", "type", "switch", "default", "goto", "else", "continue", "map", "if", "case", "package",
            "break", "var", "go", "func", "chan", "fallthrough", "return");
    public static final List<String> JAVA_KEY_WORDS = Arrays.asList("collection","implements", "synchronized", "private",
            "import", "for", "do", "float", "while", "interface", "long", "switch", "default", "protected", "public",
            "native", "continue", "else", "catch", "if", "class", "case", "new", "void", "static", "package", "break",
            "byte", "double", "finally", "false", "this", "volatile", "abstract", "throws", "int", "instanceof", "super",
            "boolean", "extends", "null", "throw", "transient", "char", "true", "final", "short", "try", "return");

    // myStopWords存放自定义的所有停用词

    public static final List<String> myStopWords = new ArrayList<String>() {{
        addAll(CommonStopWordList.ENGLISH_STOP_WORDS);
        addAll(CommonStopWordList.GO_KEY_WORDS);
        addAll(CommonStopWordList.JAVA_KEY_WORDS);
        addAll(Arrays.asList("fake", "github", "com", "demo", "string", "spring", "err", "error", "request",
                "response", "http", "httptransport", "nil", "object", "fmt", "yyyy", "dd", "ss", "mm", "hh",
                "repository", "dao", "dto", "service", "controller", "entity", "override", "autowired", "servlet", "jpa",
                "instance", "mock", "bean", "context", "args"));
        // myStopWords.addAll(readStops("D:\\stop_words_for_hw.xls")); //读取hw专有停用词
    }};
}

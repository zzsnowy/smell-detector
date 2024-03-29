package top.jach.tes.app.dev;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import top.jach.tes.app.mock.Environment;
import top.jach.tes.core.api.domain.Project;
import top.jach.tes.core.api.domain.info.Info;
import top.jach.tes.core.api.factory.InfoRepositoryFactory;
import top.jach.tes.core.api.repository.InfoRepository;
import top.jach.tes.core.impl.domain.context.BaseContextFactory;
import top.jach.tes.core.impl.factory.DefaultInfoRepositoryFactory;
import top.jach.tes.core.impl.matching.DefaultNToOneMatchingStrategy;
import top.jach.tes.core.impl.matching.NToOneMatchingStrategy;
import top.jach.tes.core.impl.service.DefaultInfoService;
import top.jach.tes.plugin.tes.code.git.commit.GitCommitMongoReository;
import top.jach.tes.plugin.tes.code.git.commit.GitCommitRepository;
import top.jach.tes.plugin.tes.code.git.commit.GitCommitsInfo;
import top.jach.tes.plugin.tes.code.git.commit.GitCommitsInfoMongoRepository;
import top.jach.tes.plugin.tes.repository.GeneraInfoMongoRepository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

public abstract class DevApp {
    private static boolean inited = false;
    static {
        init();
    }
    public static void init() {
        if(inited){
            return;
        }
        defaultInfoRepositoryFactory = infoRepositoryFactory();
        addGitCommitInfoRepository();

        Environment.infoRepositoryFactory = defaultInfoRepositoryFactory;
        Environment.contextFactory = new BaseContextFactory(Environment.iLoggerFactory, Environment.infoRepositoryFactory);
        Environment.infoService = new DefaultInfoService(Environment.contextFactory);
        Environment.defaultProject = new Project().setName("DevProject").setDesc("project for dev");
        Environment.defaultProject.setId(1l).setCreatedTime(1575784638000l).setUpdatedTime(1575784638000l);
    }
    private static DefaultInfoRepositoryFactory defaultInfoRepositoryFactory;
    public static GitCommitRepository gitCommitRepository;
//为大部分都绑定GeneraInfoMongoRepository
    private static DefaultInfoRepositoryFactory infoRepositoryFactory(){
        DefaultInfoRepositoryFactory factory = new DefaultInfoRepositoryFactory();
        DefaultNToOneMatchingStrategy<Class<? extends Info>, InfoRepository> strategy = new DefaultNToOneMatchingStrategy<>();
        factory.register(new NToOneMatchingStrategy<Class<? extends Info>, InfoRepository>() {
            @Override
            public InfoRepository NToM(Class<? extends Info> aClass) {
                return (InfoRepository) Proxy.newProxyInstance(
                        GeneraInfoMongoRepository.class.getClassLoader(),
                        GeneraInfoMongoRepository.class.getInterfaces(),
                        new MongoRepositoryProxy());
            }
            @Override
            public Set<Class<? extends Info>> MToN(InfoRepository infoRepository) {
                return null;
            }
        });
        return factory;
    }

    static class MongoRepositoryProxy implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            MongoClient mongoClient = new MongoClient();
            MongoCollection mongoCollection = mongoClient.getDatabase("tes_dev").getCollection("general_info");
            Object o = new GeneraInfoMongoRepository(mongoCollection);
            Object result = method.invoke(o, args);
            mongoClient.close();
            return result;
        }
    }

    public static void addInfoPrpositoryFactoryMatching(InfoRepository infoRepository, Class<? extends Info> clazz){
        defaultInfoRepositoryFactory.register(new NToOneMatchingStrategy<Class<? extends Info>, InfoRepository>() {
            @Override
            public InfoRepository NToM(Class<? extends Info> aClass) {
                if (clazz.equals(aClass)) {
                    return infoRepository;
                }
                return null;
            }
            @Override
            public Set<Class<? extends Info>> MToN(InfoRepository infoRepository) {
                return null;
            }
        });
    }
    private static void addGitCommitInfoRepository(){
        MongoClient mongoClient = new MongoClient();
        MongoCollection profileCollection = mongoClient.getDatabase("tes_dev").getCollection("git_commits_info_profile");
        MongoCollection commitsCollection = mongoClient.getDatabase("tes_dev").getCollection("git_commits");
        gitCommitRepository = new GitCommitMongoReository(commitsCollection);
        GitCommitsInfoMongoRepository infoRepository = new GitCommitsInfoMongoRepository(profileCollection, gitCommitRepository);

        defaultInfoRepositoryFactory.register(new NToOneMatchingStrategy<Class<? extends Info>, InfoRepository>() {
            @Override
            public InfoRepository NToM(Class<? extends Info> aClass) {
                if (GitCommitsInfo.class.isAssignableFrom(aClass)) {
                    return infoRepository;
                }
                return null;
            }
            @Override
            public Set<Class<? extends Info>> MToN(InfoRepository infoRepository) {
                return null;
            }
        });
    }
}

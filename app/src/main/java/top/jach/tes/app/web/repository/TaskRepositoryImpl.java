package top.jach.tes.app.web.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.jach.tes.app.web.entity.TaskEntity;
import top.jach.tes.core.api.domain.Task;
import top.jach.tes.core.api.repository.TaskRepository;

@Component
public class TaskRepositoryImpl implements TaskRepository {
    @Autowired
    TaskEntityRepository taskEntityRepository;
    @Override
    public Task save(Task task) {
        return taskEntityRepository.save(TaskEntity.createFromTask(task)).toTask();
    }
}

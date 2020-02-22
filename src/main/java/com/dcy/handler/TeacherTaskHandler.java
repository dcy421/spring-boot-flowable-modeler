package com.dcy.handler;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

public class TeacherTaskHandler implements TaskListener {


    @Override
    public void notify(DelegateTask delegateTask) {
        delegateTask.setAssignee("teacher");
    }
}

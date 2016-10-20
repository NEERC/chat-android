package ru.ifmo.neerc.chat.packet;

import ru.ifmo.neerc.task.Task;
import ru.ifmo.neerc.task.TaskStatus;

import org.jivesoftware.smack.packet.IQ;

public class TaskStatusIQ extends IQ {
    public static final String ELEMENT_NAME = "taskstatus";
    public static final String NAMESPACE = "http://neerc.ifmo.ru/protocol/neerc#taskstatus";

    private Task task;
    private TaskStatus status;

    public TaskStatusIQ(Task task, TaskStatus status) {
        super(ELEMENT_NAME, NAMESPACE);
        this.task = task;
        this.status = status;
    }

    protected IQ.IQChildElementXmlStringBuilder getIQChildElementBuilder(IQ.IQChildElementXmlStringBuilder xml) {
        xml.attribute("id", task.getId());
        xml.attribute("type", status.getType());
        xml.attribute("value", status.getValue());
        xml.rightAngleBracket();

        return xml;
    }
}

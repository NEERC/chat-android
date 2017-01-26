package ru.ifmo.neerc.chat.packet;

import java.util.Map;

import ru.ifmo.neerc.task.Task;
import ru.ifmo.neerc.task.TaskStatus;

import org.jivesoftware.smack.packet.IQ;

public class TaskIQ extends IQ {
    public static final String ELEMENT_NAME = "task";
    public static final String NAMESPACE = "http://neerc.ifmo.ru/protocol/neerc#task";

    private Task task;

    public TaskIQ(Task task) {
        super(ELEMENT_NAME, NAMESPACE);
        this.task = task;
    }

    protected IQ.IQChildElementXmlStringBuilder getIQChildElementBuilder(IQ.IQChildElementXmlStringBuilder xml) {
        xml.attribute("title", task.getTitle());
        xml.attribute("type", task.getType());
        xml.optAttribute("id", task.getId());
        xml.rightAngleBracket();

        for (Map.Entry<String, TaskStatus> entry : task.getStatuses().entrySet()) {
            xml.halfOpenElement("status");
            xml.attribute("for", entry.getKey());
            xml.attribute("type", entry.getValue().getType());
            xml.optAttribute("value", entry.getValue().getValue());
            xml.closeEmptyElement();
        }

        return xml;
    }
}

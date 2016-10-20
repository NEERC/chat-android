package ru.ifmo.neerc.chat.packet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ru.ifmo.neerc.task.Task;
import ru.ifmo.neerc.task.TaskStatus;

import org.jivesoftware.smack.packet.IQ;

public class TaskList extends IQ {
    public static final String ELEMENT_NAME = "query";
    public static final String NAMESPACE = "http://neerc.ifmo.ru/protocol/neerc#tasks";

    private List<Task> tasks = new ArrayList<Task>();

    public TaskList() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public Collection<Task> getTasks() {
        return tasks;
    }

    protected IQ.IQChildElementXmlStringBuilder getIQChildElementBuilder(IQ.IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();

        for (Task task : tasks) {
            xml.halfOpenElement("task");
            xml.attribute("title", task.getTitle());
            xml.attribute("type", task.getType());
            xml.attribute("id", task.getId());
            xml.rightAngleBracket();

            for (Map.Entry<String, TaskStatus> entry : task.getStatuses().entrySet()) {
                xml.halfOpenElement("status");
                xml.attribute("for", entry.getKey());
                xml.attribute("type", entry.getValue().getType());
                xml.optAttribute("value", entry.getValue().getValue());
                xml.closeEmptyElement();
            }

            xml.closeElement("task");
        }

        return xml;
    }
}

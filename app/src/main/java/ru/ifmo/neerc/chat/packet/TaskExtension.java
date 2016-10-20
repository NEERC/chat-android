package ru.ifmo.neerc.chat.packet;

import java.util.Map;

import ru.ifmo.neerc.task.Task;
import ru.ifmo.neerc.task.TaskStatus;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class TaskExtension implements ExtensionElement {
    public static final String ELEMENT_NAME = "x";
    public static final String NAMESPACE = "http://neerc.ifmo.ru/protocol/neerc#tasks";

    private Task task;

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();

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
        xml.closeElement(getElementName());

        return xml;
    }
}

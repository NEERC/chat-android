package ru.ifmo.neerc.chat.packet;

import java.io.IOException;
import java.util.Date;

import ru.ifmo.neerc.task.Task;
import ru.ifmo.neerc.task.TaskStatus;

import org.jivesoftware.smack.provider.ExtensionElementProvider;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class TaskExtensionProvider extends ExtensionElementProvider<TaskExtension> {

    @Override
    public TaskExtension parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
        TaskExtension extension = new TaskExtension();

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if ("task".equals(parser.getName())) {
                    extension.setTask(parseTask(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (TaskExtension.ELEMENT_NAME.equals(parser.getName())) {
                    done = true;
                }
            }
        }

        return extension;
    }

    private Task parseTask(XmlPullParser parser) throws XmlPullParserException, IOException {
        Date date = new Date();
        String timestamp = parser.getAttributeValue("", "timestamp");
        if (timestamp != null) {
            date = new Date(Long.parseLong(timestamp));
        }

        Task task = new Task(
            parser.getAttributeValue("", "id"),
            parser.getAttributeValue("", "type"),
            parser.getAttributeValue("", "title")
        );

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if ("status".equals(parser.getName())) {
                    task.setStatus(
                        parser.getAttributeValue("", "for"),
                        parser.getAttributeValue("", "type"),
                        parser.getAttributeValue("", "value")
                    );
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("task".equals(parser.getName())) {
                    done = true;
                }
            }
        }

        return task;
    }
}

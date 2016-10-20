package ru.ifmo.neerc.chat.packet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ru.ifmo.neerc.chat.user.UserEntry;

import org.jivesoftware.smack.packet.IQ;

public class UserList extends IQ {
    public static final String ELEMENT_NAME = "query";
    public static final String NAMESPACE = "http://neerc.ifmo.ru/protocol/neerc#users";

    private List<UserEntry> users = new ArrayList<UserEntry>();

    public UserList() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    public void addUser(UserEntry user) {
        users.add(user);
    }

    public Collection<UserEntry> getUsers() {
        return users;
    }

    protected IQ.IQChildElementXmlStringBuilder getIQChildElementBuilder(IQ.IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();

        for (UserEntry user : users) {
            xml.halfOpenElement("user");
            xml.attribute("name", user.getName());
            xml.attribute("group", user.getGroup());
            xml.attribute("power", user.isPower() ? "yes" : "no");
            xml.closeEmptyElement();
        }

        return xml;
    }
}

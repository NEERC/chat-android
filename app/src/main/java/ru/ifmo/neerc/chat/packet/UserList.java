/*
 * Copyright 2017 NEERC team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

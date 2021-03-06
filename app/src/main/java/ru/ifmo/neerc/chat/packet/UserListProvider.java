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

import java.io.IOException;

import ru.ifmo.neerc.chat.user.UserEntry;

import org.jivesoftware.smack.provider.IQProvider;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class UserListProvider extends IQProvider<UserList> {

    @Override
    public UserList parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
        UserList userList = new UserList();

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if ("user".equals(parser.getName())) {
                    userList.addUser(parseUser(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (UserList.ELEMENT_NAME.equals(parser.getName())) {
                    done = true;
                }
            }
        }

        return userList;
    }

    private UserEntry parseUser(XmlPullParser parser) throws XmlPullParserException, IOException {
        String name = parser.getAttributeValue("", "name");
        String group = parser.getAttributeValue("", "group");
        boolean power = "yes".equals(parser.getAttributeValue("", "power"));

        UserEntry user = new UserEntry(name, 0, name, power);
        user.setGroup(group);

        return user;
    }
}

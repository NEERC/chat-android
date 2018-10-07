/*
 * Copyright 2018 NEERC team
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

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class ComputerExtension implements ExtensionElement {
    public static final String ELEMENT_NAME = "computer";
    public static final String NAMESPACE = "http://neerc.ifmo.ru/protocol/neerc#computer";

    private boolean reachable;

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public boolean isReachable() {
        return reachable;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();

        xml.element("reachable", Boolean.toString(reachable));

        xml.closeElement(getElementName());

        return xml;
    }
}

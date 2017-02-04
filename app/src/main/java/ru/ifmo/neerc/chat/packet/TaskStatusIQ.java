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

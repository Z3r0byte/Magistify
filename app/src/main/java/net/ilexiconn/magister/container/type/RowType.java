/*
 * Copyright (c) 2016-2017 Bas van den Boom 'Z3r0byte'
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ilexiconn.magister.container.type;

import java.io.Serializable;

public enum RowType implements Serializable {
    UNKNOWN(0),
    GRADE(1),
    AVERAGE(2),
    MAXIMUM(3),
    FORMULA(4),
    MINIMUM(5),
    ACTUAL(6),
    COUNT(7),
    CEVO(8),
    TEXT(9),
    CEVOCPE(10),
    CEVOCIE(11),
    WEIGHT(12),
    ENDGRADE(13),
    FAILS(14),
    TREETOP(15),
    SUBJECTCONDITION(16);


    private int id;

    RowType(int i) {
        id = i;
    }

    public static RowType getTypeById(int i) {
        for (RowType type : values()) {
            if (type.getID() == i) {
                return type;
            }
        }
        return null;
    }

    public int getID() {
        return id;
    }
}

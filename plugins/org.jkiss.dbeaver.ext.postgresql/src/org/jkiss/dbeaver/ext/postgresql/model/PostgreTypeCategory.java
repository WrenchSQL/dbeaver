/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * PostgreProcedure
 */
public enum PostgreTypeCategory implements DBPNamedObject
{

    A("Array"),
    B("Boolean"),
    C("Composite"),
    D("Date/time"),
    E("Enum"),
    G("Geometric"),
    I("Network address"),
    N("Numeric"),
    P("Pseudo"),
    S("String"),
    T("Timespan"),
    U("User-defined"),
    V("Bit-string"),
    X("Unknown"),
    R("Range");

    private final String desc;

    PostgreTypeCategory(String desc) {
        this.desc = desc;
    }

    @NotNull
    @Override
    public String getName() {
        return desc;
    }
}

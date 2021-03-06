/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model.dict;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DB2 Type of Package Dependency
 * 
 * @author Denis Forveille
 */
public enum DB2PackageDepType implements DBPNamedObject {
    A("Table alias", DB2ObjectType.ALIAS),

    B("Trigger", DB2ObjectType.TRIGGER),

    D("Server"),

    F("Routine", DB2ObjectType.ROUTINE),

    G("Global temporary table", DB2ObjectType.TABLE),

    I("Index", DB2ObjectType.INDEX),

    M("Function Mapping", DB2ObjectType.UDF),

    N("Nickname", DB2ObjectType.NICKNAME),

    O("Privilege dependency on all subtables or subviews in a table or view hierarchy"),

    P("Page Size"),

    Q("Sequence", DB2ObjectType.SEQUENCE),

    R("UDT", DB2ObjectType.UDT),

    S("MQT", DB2ObjectType.MQT),

    T("Table", DB2ObjectType.TABLE),

    U("Typed table", DB2ObjectType.TABLE),

    V("View", DB2ObjectType.VIEW),

    W("Typed view", DB2ObjectType.VIEW),

    X("Index extension"),

    Z("XSR object", DB2ObjectType.XML_SCHEMA),

    m("Module", DB2ObjectType.MODULE),

    n("Database Partiton Group"),

    q("Sequence alias", DB2ObjectType.ALIAS),

    u("Module alias", DB2ObjectType.ALIAS),

    v("Global variable", DB2ObjectType.VARIABLE),

    ZZ_4("Application-period temporal table"),

    ZZ_5("Application-period temporal table");

    public static final String FAKE_PREFIX = "ZZ_";

    private String name;
    private DB2ObjectType db2ObjectType;

    // -----------
    // Constructor
    // -----------

    private DB2PackageDepType(String name, DB2ObjectType db2ObjectType)
    {
        this.name = name;
        this.db2ObjectType = db2ObjectType;
    }

    private DB2PackageDepType(String name)
    {
        this(name, null);
    }

    // -----------------------
    // Display @Property Value
    // -----------------------
    @Override
    public String toString()
    {
        return name;
    }

    // ----------------
    // Standard Getters
    // ----------------

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    public DB2ObjectType getDb2ObjectType()
    {
        return db2ObjectType;
    }

}
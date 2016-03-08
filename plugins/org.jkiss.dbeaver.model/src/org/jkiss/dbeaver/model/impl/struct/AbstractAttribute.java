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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.DBPositiveNumberTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

/**
 * AbstractAttribute
 */
public abstract class AbstractAttribute implements DBSAttributeBase
{
    protected String name;
    protected int valueType;
    protected long maxLength;
    protected boolean required;
    protected boolean autoGenerated;
    protected int scale;
    protected int precision;
    protected String typeName;
    protected int ordinalPosition;

    protected AbstractAttribute()
    {
    }

    protected AbstractAttribute(
            String name,
            String typeName,
            int valueType,
            int ordinalPosition,
            long maxLength,
            int scale,
            int precision,
            boolean required,
            boolean autoGenerated)
    {
        this.name = name;
        this.valueType = valueType;
        this.maxLength = maxLength;
        this.scale = scale;
        this.precision = precision;
        this.required = required;
        this.autoGenerated = autoGenerated;
        this.typeName = typeName;
        this.ordinalPosition = ordinalPosition;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 10)
    public String getName()
    {
        return name;
    }

    public void setName(String columnName)
    {
        this.name = columnName;
    }

    @Override
    @Property(viewable = true, order = 20)
    public String getTypeName()
    {
        return typeName;
    }

    public void setTypeName(String typeName)
    {
        this.typeName = typeName;
    }

    @Property(viewable = true, order = 0)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public void setOrdinalPosition(int ordinalPosition)
    {
        this.ordinalPosition = ordinalPosition;
    }

    @Override
    public int getTypeID()
    {
        return valueType;
    }

    public void setValueType(int valueType)
    {
        this.valueType = valueType;
    }

    @Override
    @Property(viewable = true, order = 40)
    public long getMaxLength()
    {
        return maxLength;
    }

    public void setMaxLength(long maxLength)
    {
        this.maxLength = maxLength;
    }

    @Override
    @Property(viewable = false, valueRenderer = DBPositiveNumberTransformer.class, order = 41)
    public int getScale()
    {
        return scale;
    }

    public void setScale(int scale)
    {
        this.scale = scale;
    }

    @Override
    @Property(viewable = false, valueRenderer = DBPositiveNumberTransformer.class, order = 42)
    public int getPrecision()
    {
        return precision;
    }

    public void setPrecision(int precision)
    {
        this.precision = precision;
    }

    @Property(viewable = true, order = 50)
    public boolean isRequired()
    {
        return required;
    }

    public void setRequired(boolean required)
    {
        this.required = required;
    }

    @Property(viewable = true, order = 55)
    public boolean isAutoGenerated() {
        return autoGenerated;
    }

    @Override
    public boolean isPseudoAttribute() {
        return false;
    }

    public void setAutoGenerated(boolean autoGenerated) {
        this.autoGenerated = autoGenerated;
    }

    @Nullable
    public String getDescription()
    {
        return null;
    }

    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public String toString() {
        return name + ", type=" + typeName + ", pos=" + ordinalPosition;
    }

}

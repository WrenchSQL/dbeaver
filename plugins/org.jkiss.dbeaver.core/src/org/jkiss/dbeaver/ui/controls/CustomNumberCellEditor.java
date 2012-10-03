/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;

/**
 * Number cell editor
 */
public class CustomNumberCellEditor extends TextCellEditor {

    private final Class<?> valueType;

    public CustomNumberCellEditor(Composite parent, Class<?> valueType)
    {
        super();
        this.valueType = valueType;
        create(parent);
    }

    @Override
    protected Control createControl(Composite parent)
    {
        super.createControl(parent);
        if (valueType == Float.class ||
            valueType == Float.TYPE ||
            valueType == Double.class ||
            valueType == Double.TYPE ||
            valueType == BigDecimal.class)
        {
            text.addVerifyListener(UIUtils.NUMBER_VERIFY_LISTENER);
        } else {
            text.addVerifyListener(UIUtils.INTEGER_VERIFY_LISTENER);
        }
        return text;
    }

    @Override
    protected Object doGetValue()
    {
        return RuntimeUtils.convertString((String) super.doGetValue(), valueType);
    }

    @Override
    protected void doSetValue(Object value)
    {
        super.doSetValue(CommonUtils.toString(value));
    }

}

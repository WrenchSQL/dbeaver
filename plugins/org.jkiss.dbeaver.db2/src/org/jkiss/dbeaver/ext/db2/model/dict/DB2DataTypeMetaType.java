/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model.dict;

/**
 * DB2 Meta Type for DataTypes
 * 
 * @author Denis Forveille
 * 
 */
public enum DB2DataTypeMetaType {
   A("A (User-defined array type )"),

   C("C (User-defined cursor type )"),

   F("F (User-defined row type )"),

   L("L (User-defined associative array type)"),

   R("R (User-defined structured type)"),

   S("S (System predefined type)"),

   T("T (User-defined distinct type)");

   private String description;

   // -----------------
   // Constructor
   // -----------------
   private DB2DataTypeMetaType(String description) {
      this.description = description;
   }

   // ----------------
   // Standard Getters
   // ----------------
   public String getDescription() {
      return description;
   }
}
/*
 * Copyright (C) 2010-2015 Serge Rieder
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
/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;
import org.jkiss.dbeaver.ext.erd.command.AssociationCreateCommand;
import org.jkiss.dbeaver.ext.erd.command.AssociationReconnectSourceCommand;
import org.jkiss.dbeaver.ext.erd.command.AssociationReconnectTargetCommand;
import org.jkiss.dbeaver.ext.erd.model.ERDAssociation;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;

/**
 * Handles manipulation of relationships between tables
 * @author Serge Rieder
 */
public class EntityNodeEditPolicy extends GraphicalNodeEditPolicy
{

	/**
	 * @see GraphicalNodeEditPolicy#getConnectionCreateCommand(CreateConnectionRequest)
	 */
	@Override
    protected Command getConnectionCreateCommand(CreateConnectionRequest request)
	{

		AssociationCreateCommand cmd = new AssociationCreateCommand();
		EntityPart part = (EntityPart) getHost();
		cmd.setForeignEntity(part.getTable());
		request.setStartCommand(cmd);
		return cmd;
	}

	/**
	 * @see GraphicalNodeEditPolicy#getConnectionCompleteCommand(CreateConnectionRequest)
	 */
	@Override
    protected Command getConnectionCompleteCommand(CreateConnectionRequest request)
	{
		AssociationCreateCommand cmd = (AssociationCreateCommand) request.getStartCommand();
		EntityPart part = (EntityPart) request.getTargetEditPart();
		cmd.setPrimaryEntity(part.getTable());
		return cmd;
	}

	/**
	 * @see GraphicalNodeEditPolicy#getReconnectSourceCommand(ReconnectRequest)
	 */
	@Override
    protected Command getReconnectSourceCommand(ReconnectRequest request)
	{
		
		AssociationReconnectSourceCommand cmd = new AssociationReconnectSourceCommand();
		cmd.setRelationship((ERDAssociation) request.getConnectionEditPart().getModel());
		EntityPart entityPart = (EntityPart) getHost();
		cmd.setSourceForeignKey(entityPart.getTable());
		return cmd;
	}

	/**
	 * @see GraphicalNodeEditPolicy#getReconnectTargetCommand(ReconnectRequest)
	 */
	@Override
    protected Command getReconnectTargetCommand(ReconnectRequest request)
	{
		AssociationReconnectTargetCommand cmd = new AssociationReconnectTargetCommand();
		cmd.setRelationship((ERDAssociation) request.getConnectionEditPart().getModel());
		EntityPart entityPart = (EntityPart) getHost();
		cmd.setTargetPrimaryKey(entityPart.getTable());
		return cmd;
	}

}
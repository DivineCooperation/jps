/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dc.jps.preset;

/*
 * #%L
 * JPS
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPValidationException;

/**
 *
 * @author mpohling
 */
public class JPShowGUI extends AbstractJPBoolean {
	
	public final static String[] COMMAND_IDENTIFIERS = {"--gui"};

	public JPShowGUI() {
		super(COMMAND_IDENTIFIERS);
	}
	
	@Override
	protected void validate() throws JPValidationException {
		if(!isIdentifiered() && !getValue()) {
			logger.info("GUI is disabled! Set "+COMMAND_IDENTIFIERS[0]+" property to display the GUI.");
		}
	}

    @Override
    protected Boolean getPropertyDefaultValue() {
        return true;
    }
	
	@Override
	public String getDescription() {
		return "Displays the Graphic-User-Interface of "+JPService.getApplicationName();
	}
}
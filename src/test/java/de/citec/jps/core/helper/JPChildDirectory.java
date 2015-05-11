/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.jps.core.helper;

import de.citec.jps.core.JPService;
import de.citec.jps.preset.AbstractJPDirectory;
import de.citec.jps.tools.FileHandler;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class JPChildDirectory extends AbstractJPDirectory {

	public final static String[] COMMAND_IDENTIFIERS = {"--child"};
	
	public JPChildDirectory() {
		super(COMMAND_IDENTIFIERS, FileHandler.ExistenceHandling.Must, FileHandler.AutoMode.On);
	}

    @Override
    public File getParentDirectory() {
        return JPService.getProperty(JPBaseDirectory.class).getValue();
    }
    
	@Override
	protected File getPropertyDefaultValue() {
		return new File("child");
	}

	@Override
	public String getDescription() {
		return "Specifies the device config database location.";
	}
}
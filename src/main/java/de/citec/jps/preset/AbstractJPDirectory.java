/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.jps.preset;

import de.citec.jps.tools.FileHandler;
import de.citec.jps.tools.FileHandler.AutoMode;
import de.citec.jps.tools.FileHandler.ExistenceHandling;

/**
 *
 * @author mpohling
 */
public abstract class AbstractJPDirectory extends AbstractJPFile {
    
    private static final String[] ARGUMENT_IDENTIFIERS = {"DIRECTORY"};

	public AbstractJPDirectory(String[] commandIdentifier, ExistenceHandling existenceHandling, AutoMode autoCreateMode) {
        this(commandIdentifier, ARGUMENT_IDENTIFIERS, existenceHandling, autoCreateMode);
    }
	public AbstractJPDirectory(String[] commandIdentifier, String[] argumentIdentifiers, ExistenceHandling existenceHandling, AutoMode autoCreateMode) {
		super(commandIdentifier, argumentIdentifiers, existenceHandling ,autoCreateMode, FileHandler.FileType.Directory);
	}
}
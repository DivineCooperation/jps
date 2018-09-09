package org.openbase.jps.core;

/*
 * #%L
 * JPS
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.jps.exception.*;
import org.openbase.jps.preset.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Java Property Service, this is the central lib controller used to initialize and manage all properties.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * <p>
 * JPS Library can be used for managing the properties of an application. The argument definition is realized by registering classes which extends the AbstractJavaProperty class. Common argument types
 * are supported by the preset properties (e.g. Integer, Boolean, String types).
 * <p>
 * The library supports the generation of a properties overview page.
 */
public class JPService {

    private static final Set<Class<? extends AbstractJavaProperty>> registeredPropertyClasses = new HashSet<>();
    private static final HashMap<Class<? extends AbstractJavaProperty>, AbstractJavaProperty> initializedProperties = new HashMap<>();
    private static final HashMap<Class<? extends AbstractJavaProperty>, AbstractJavaProperty> loadedProperties = new HashMap<>();
    private static final HashMap<Class<? extends AbstractJavaProperty>, Object> overwrittenDefaultValueMap = new HashMap<>();
    private static Logger LOGGER = LoggerFactory.getLogger(JPService.class);
    private static String applicationName = "";
    private static Class applicationMainClass;
    private static boolean argumentsAnalyzed = false;

    static {
        initJPSDefaultProperties();
    }

    private static void initJPSDefaultProperties() {
        registerProperty(JPHelp.class);
        registerProperty(JPVerbose.class);
    }

    /**
     * Set the application name. The name is displayed in the help screen in the property overview page.
     *
     * @param name the application name
     */
    public static void setApplicationName(String name) {
        applicationName = name;
    }

    /**
     * Returns the configurated application name.
     *
     * @return the application name.
     */
    public static String getApplicationName() {
        return applicationName;
    }

    /**
     * Set the application name by the main class name. The name is displayed in the help screen in the property overview page. The name is generated by using the lower case simple class name of the
     * given class.
     *
     * @param mainclass the application mainclass which is used to generate the application name.
     */
    public static void setApplicationName(final Class mainclass) {
        applicationMainClass = mainclass;

        // format and setup application name
        setApplicationName(mainclass.getSimpleName().replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase());
    }

    /**
     * Register the given property and overwrite the default value of the given one.
     * <p>
     * Do not use this method after calling the analyze method, otherwise recursive property usage is not effected by the new default value!
     *
     * @param <V>
     * @param <C>
     * @param propertyClass
     * @param defaultValue
     */
    public static synchronized <V, C extends AbstractJavaProperty<V>> void registerProperty(Class<C> propertyClass, V defaultValue) {
        if (argumentsAnalyzed) {
            LOGGER.warn("Property modification after argumend analysis detected! Read JPService doc for more information.");

        }
        registeredPropertyClasses.add(propertyClass);
        overwrittenDefaultValueMap.put(propertyClass, defaultValue);
    }

    /**
     * Overwrites the default value of the given property without displaying the property in the help overview, For overwriting a regular property default value, use the property registration method
     * instead.
     * <p>
     * Do not use this method after calling the analyze method, otherwise recursive property usage is not effected by the new default value!
     *
     * @param <V>
     * @param <C>
     * @param propertyClass
     * @param defaultValue
     */
    public static synchronized <V, C extends AbstractJavaProperty<V>> void overwriteDefaultValue(Class<C> propertyClass, V defaultValue) {
        if (argumentsAnalyzed) {
            LOGGER.warn("Property modification after argumend analysis detected! Read JPService doc for more information.");
        }
        overwrittenDefaultValueMap.put(propertyClass, defaultValue);
    }

    /**
     * Register new property.
     *
     * @param propertyClass
     */
    public static void registerProperty(Class<? extends AbstractJavaProperty> propertyClass) {
        if (argumentsAnalyzed) {
            LOGGER.warn("Property modification after argumend analysis detected! Read JPService doc for more information.");
        }
        registeredPropertyClasses.add(propertyClass);
    }

    /**
     * Analyze the input arguments and setup all registered Properties. If one argument could not be handled or something else goes wrong this methods calls System.exit(255);
     * <p>
     * Make sure all desired properties are registered before calling this method. Otherwise the properties will not be listed in the help screen.
     * <p>
     * Note: In case the JPUnitTestMode was enabled this method does not call exit.
     *
     * @param args Arguments as a string list e.g. given by a java fx application {@code getParameters().getRaw()} in the start method.
     */
    public static void parseAndExitOnError(final List<String> args) {
        parseAndExitOnError(args.toArray(new String[args.size()]));
    }

    /**
     * Analyze the input arguments and setup all registered Properties. If one argument could not be handled or something else goes wrong this methods calls System.exit(255);
     * <p>
     * Make sure all desired properties are registered before calling this method. Otherwise the properties will not be listed in the help screen.
     * <p>
     * Note: In case the JPUnitTestMode was enabled this method does not call exit.
     *
     * @param args Arguments given by the main method.
     */
    public static void parseAndExitOnError(String[] args) {
        try {
            JPService.parse(args);
        } catch (JPServiceException ex) {
            try {
                JPService.printHelp();
            } catch (JPServiceException ex1) {
                getApplicationLogger().error("Could not print help text!");
                printError(ex1);
            }
            printError(ex);
            getApplicationLogger().info("Exit " + applicationName);

            if (!testMode()) {
                System.exit(255);
            }
        }
    }

    /**
     * Generate help page.
     *
     * @throws JPServiceException is thrown if something went wrong.
     */
    private static void handleHelpCall() throws JPServiceException {
        try {
            if (JPService.getProperty(JPHelp.class).isIdentified()) {
                try {
                    JPService.printHelp();
                } catch (Exception ex) {
                    LOGGER.error("Could not fully generate help page!", ex);
                }

                if (!JPService.testMode()) {
                    System.exit(0);
                }
            }
        } catch (JPServiceException ex) {
            throw new JPServiceException("Could not generate help page!", ex);
        }
    }

    /**
     * Analyze the input arguments and setup all registered Properties.
     * <p>
     * Make sure all desired properties are registered before calling this method. Otherwise the properties will not be listed in the help screen.
     *
     * @param args Arguments as a string list e.g. given by a java fx application {@code getParameters().getRaw()} in the start method.
     *
     * @throws JPServiceException
     */
    public static void parse(final List<String> args) throws JPServiceException {
        parse(args.toArray(new String[args.size()]));
    }


    /**
     * Setups all registered Properties.
     * <p>
     * Make sure all desired properties are registered before calling this method.
     * Method calls system exit if at least one property could not be initialized.
     */
    public static void exitOnError() {
        parseAndExitOnError(new String[0]);
    }

    /**
     * Analyze the input arguments and setup all registered Properties.
     * <p>
     * Make sure all desired properties are registered before calling this method. Otherwise the properties will not be listed in the help screen.
     *
     * @param args Arguments given by the main method.
     *
     * @throws JPServiceException
     */
    public static void parse(final String[] args) throws JPServiceException {
        argumentsAnalyzed = true;
        try {
            printValueModification(args);
            initRegisteredProperties(args);
        } catch (Exception ex) {
            throw new JPServiceException("Could not analyse arguments: " + ex.getMessage(), ex);
        }
        handleHelpCall();
    }

    public static void printError(String message, Throwable cause) {
        printError(new JPServiceException(message, cause));
    }

    /**
     * @param cause
     */
    public static void printError(Throwable cause) {
        getApplicationLogger().error("=========================================================================");
        printError(cause, "=");
        try {
            if (getProperty(JPVerbose.class).getValue()) {
                cause.printStackTrace(System.err);
            }
        } catch (JPNotAvailableException ex) {
            getApplicationLogger().error("Could not load exception stack: " + ex.getMessage());
        }
        getApplicationLogger().error("=========================================================================");
    }

    /**
     * @param cause
     * @param prefix
     */
    protected static void printError(Throwable cause, String prefix) {
        getApplicationLogger().error(prefix + " " + cause.getMessage());
        Throwable innerCause = cause.getCause();
        if (innerCause != null) {
            printError(innerCause, prefix + "==");
        }
    }

    /**
     * @param args
     */
    private static void printValueModification(String[] args) {

        if (args == null) {
            return;
        }

        String argsString = "";
        for (String arg : args) {
            if (arg.startsWith("--")) {
                argsString += "\n\t";
            } else if (arg.startsWith("-")) {
                argsString += "\n\t ";
            } else {
                argsString += " ";
            }
            argsString += arg;
        }

        if (!argsString.isEmpty()) {
            argsString += "\n";
            getApplicationLogger().info("[command line value modification]" + argsString);
        }
    }

    /**
     * @throws JPServiceException
     */
    private static void initRegisteredProperties() throws JPServiceException {
        initRegisteredProperties(null);
    }

    /**
     * @param args
     *
     * @throws JPServiceException
     */
    private static void initRegisteredProperties(final String[] args) throws JPServiceException {

        // reset already loaded properties.
        loadedProperties.clear();

        try {
            boolean modification = true;

            // init recursive all properties which are not already initialized.
            while (modification) {
                modification = false;
                for (Class<? extends AbstractJavaProperty> propertyClass : new HashSet<>(registeredPropertyClasses)) {
                    if (!initializedProperties.containsKey(propertyClass)) {
                        initProperty(propertyClass);
                        modification = true;
                    }
                }
            }

            if (args != null) {
                parseArguments(args);

                // unload all properties to apply recursive parsed changes.
                loadedProperties.clear();
            }

            loadAllProperties(true);

        } catch (JPServiceException ex) {
            throw new JPServiceException("Could not init registered properties!", ex);
        }

        //print help if required.
        getProperty(JPHelp.class);
    }

    /**
     * @param propertyClass
     *
     * @return
     *
     * @throws JPServiceException
     */
    private static synchronized AbstractJavaProperty initProperty(Class<? extends AbstractJavaProperty> propertyClass) throws JPServiceException {

        try {
            // Avoid double initialization
            if (initializedProperties.containsKey(propertyClass)) {
                throw new JPServiceException("Already initialized!");
            }

            if (!registeredPropertyClasses.contains(propertyClass)) {
                registeredPropertyClasses.add(propertyClass);
            }
            AbstractJavaProperty newInstance = propertyClass.newInstance();

            initializedProperties.put(propertyClass, newInstance);

            // init load dependencies.
            for (Class<? extends AbstractJavaProperty> dependentPropertyClass : (List<Class<? extends AbstractJavaProperty>>) newInstance.getDependencyList()) {
                if (!initializedProperties.containsKey(dependentPropertyClass)) {
                    initProperty(dependentPropertyClass);
                }
            }

            return newInstance;
        } catch (JPServiceException | InstantiationException | IllegalAccessException ex) {
            throw new JPInitializationException("Could not init " + propertyClass.getSimpleName(), ex);
        }
    }

    /**
     * Setup JPService for JUnitTests By using the JPService during JUnit Tests it's recommended to call this method after property registration instead using the parsing methods because command line
     * property handling makes no sense in the context of unit tests..
     * <p>
     * The following properties are activated by default while running JPService in TestMode:
     * <p>
     * - JPVerbose is set to true to print more debug messages.
     * <p>
     * - JPTestMode is activated.
     *
     * @throws JPServiceException
     */
    public static void setupJUnitTestMode() throws JPServiceException {
        try {
            registerProperty(JPVerbose.class, true);
            registerProperty(JPTestMode.class, true);
            initRegisteredProperties();
        } catch (JPValidationException ex) {
            throw new JPServiceException("Could not setup JPService for UnitTestMode!", ex);
        }
    }

    /**
     * @param property
     *
     * @throws JPServiceException
     */
    @SuppressWarnings("unchecked")
    private static void loadProperty(final AbstractJavaProperty property) throws JPServiceException {

        try {
            if (loadedProperties.containsKey(property.getClass()) && !loadedProperties.get(property.getClass()).neetToBeParsed()) {
                return;
            }

            parseProperty(property);
            if (overwrittenDefaultValueMap.containsKey(property.getClass())) {
                property.overwriteDefaultValue(overwrittenDefaultValueMap.get(property.getClass()));
            }
            property.updateValue();
            property.validate();
        } catch (JPBadArgumentException | JPValidationException ex) {
            throw new JPServiceException("Could not load " + property + "!", ex);
        }

        loadedProperties.put(property.getClass(), property);
        try {
            property.loadAction();
        } catch (Throwable ex) {
            throw new JPServiceException("Could not load Property[" + property.getClass().getSimpleName() + "] action!", ex);
        }
    }

    /**
     * @param args
     *
     * @throws JPServiceException
     */
    private static void parseArguments(String[] args) throws JPServiceException {
        AbstractJavaProperty lastProperty = null;

        for (String arg : args) {
            try {
                arg = arg.trim();

                // handle final pattern
                if (arg.equals("--")) {
                    break;
                }

                // handle default properties
                if (arg.startsWith("-D")) {
                    try {
                        // remove "-D" prefix
                        final String propertyString = arg.substring(2);
                        if (propertyString.contains("=")) {
                            final String[] keyValueArray = propertyString.split("=");
                            System.setProperty(keyValueArray[0], keyValueArray[1]);
                        } else {
                            System.setProperty(propertyString, "");
                        }
                        continue;
                    } catch (IllegalArgumentException | IndexOutOfBoundsException | SecurityException | NullPointerException ex) {
                        throw new JPParsingException("invalid system property syntax: " + arg);
                    }
                }

                if (arg.startsWith("-") || arg.startsWith("--")) { // handle property
                    boolean unknownProperty = true;
                    for (AbstractJavaProperty property : initializedProperties.values()) {

                        if (property.match(arg)) {
                            lastProperty = property;
                            lastProperty.reset(); // In case of property overwriting during script recursion. Example: -p 5 -p 9
                            unknownProperty = false;
                            break;
                        }
                    }
                    if (unknownProperty) {
                        throw new JPParsingException("unknown property: " + arg);
                    }
                } else {
                    if (lastProperty == null) {
                        throw new JPParsingException("= bad property: " + arg);
                    }
                    lastProperty.addArgument(arg);
                }
            } catch (JPServiceException ex) {
                throw new JPServiceException("Could not parse Argument[" + arg + "]!", ex);
            }
        }
    }

    /**
     * @param property
     *
     * @throws JPBadArgumentException
     */
    private static void parseProperty(final AbstractJavaProperty property) throws JPBadArgumentException {
        if (property.neetToBeParsed()) {
            try {
                property.parseArguments();
            } catch (Exception ex) {
                throw new JPBadArgumentException("Could not parse " + property + "!", ex);
            }
        }
    }

    /**
     * Returns the current value of the given property line class.
     * <p>
     * If the property is never registered but the class is known in the classpath, the method returns the default value.
     *
     * @param <C>
     * @param propertyClass property class which defines the property.
     *
     * @return the current value of the given property type.
     *
     * @throws org.openbase.jps.exception.JPNotAvailableException thrown if the given property could not be .
     */
    public static synchronized <C extends AbstractJavaProperty> C getProperty(Class<C> propertyClass) throws JPNotAvailableException {
        try {
            if (propertyClass == null) {
                throw new JPNotAvailableException(propertyClass, new JPServiceException("Given propertyClass is a Nullpointer!"));
            }

            // load if not already done.
            if (!loadedProperties.containsKey(propertyClass) || loadedProperties.get(propertyClass).neetToBeParsed()) {
                // init if not already done.
                if (!initializedProperties.containsKey(propertyClass)) {
                    initProperty(propertyClass);
                }
                loadProperty(initializedProperties.get(propertyClass));
            }
            return (C) loadedProperties.get(propertyClass);
        } catch (JPServiceException ex) {
            throw new JPNotAvailableException(propertyClass, ex);
        }
    }

    /**
     * @return @throws JPServiceException
     */
    private static List<AbstractJavaProperty> loadAllProperties(final boolean errorReport) throws JPServiceException {
        List<AbstractJavaProperty> properties = new ArrayList<>();
        Collection<Class<? extends AbstractJavaProperty>> currentlyregisteredPropertyClasses = new HashSet(registeredPropertyClasses);
        boolean modification = true;

        // load recursive all properties which are not already loaded.
        while (modification) {
            modification = false;
            currentlyregisteredPropertyClasses.forEach(propertyClass -> {
                try {
                    properties.add(getProperty(propertyClass));
                } catch (Exception ex) {
                    if (errorReport) {
                        new JPServiceException("Could not load Property[" + propertyClass.getSimpleName() + "]!", ex);
                    } else {
                        LOGGER.debug("Could not load Property[" + propertyClass.getSimpleName() + "]!", ex);
                    }
                }
            });
        }
        return properties;
    }

    /**
     * Method prints the help screen.
     *
     * @throws org.openbase.jps.exception.JPServiceException
     */
    public static void printHelp() throws JPServiceException {

        String help = "\n\nusage: " + applicationName;
        String header = "";
        List<AbstractJavaProperty> propertyList = new ArrayList(initializedProperties.values());
        Collections.sort(propertyList);
        header = propertyList.stream().map((property) -> " [" + property.getSyntax() + "]").reduce(header, (s, str) -> s.concat(str));
        help += newLineFormatter(header, "\n\t", 100);
        help += "\nwhere:\n";

        List<AbstractJavaProperty> properties = loadAllProperties(false);

        Collections.sort(properties, (AbstractJavaProperty o1, AbstractJavaProperty o2) -> {
            try {
                return o1.getDefaultExample().compareTo(o2.getDefaultExample());
            } catch (Exception ex) {
                getApplicationLogger().warn("Could not compare properties!");
                return -1;
            }
        });

        for (AbstractJavaProperty property : properties) {
            help += "\t" + property.getSyntax() + " " + getDefault(property);
            help += "\n ";
            help += "\t\t" + newLineFormatter(property.getDescription(), "\n\t\t", 100);
            help += "\n";
        }
        getApplicationLogger().info(help);
    }

    /**
     * Returns the logger instance of the main class.
     * <p>
     * Note: If the main class is not set via {@code setApplicationName()} the {@code defaultLogger} is used.
     *
     * @return a default logger instance.
     */
    public static Logger getApplicationLogger(final Logger defaultLogger) {
        return (applicationMainClass != null ? LoggerFactory.getLogger(applicationMainClass) : defaultLogger);
    }

    /**
     * Returns the logger instance of the main class.
     * <p>
     * Note: If the main class is not set via {@code setApplicationName()} the jpservice default logger instance is used.
     *
     * @return a logger instance.
     */
    public static Logger getApplicationLogger() {
        return getApplicationLogger(LOGGER);
    }

    /**
     * @param property
     *
     * @return
     */
    private static String getDefault(AbstractJavaProperty property) {
        return "[Default: " + property.getDefaultExample() + "]";
    }

    /**
     * @param text
     * @param newLineOperator
     * @param maxChars
     *
     * @return
     */
    public static String newLineFormatter(String text, String newLineOperator, int maxChars) {
        String[] textArray = text.split(" ");
        text = "";
        int charCounter = 0;

        for (int i = 0; i < textArray.length; i++) {
            if ((charCounter + textArray[i].length()) >= maxChars) {
                text += newLineOperator + textArray[i];
                charCounter = textArray[i].length();
            } else {
                text += textArray[i];

                if (textArray[i].contains("\n")) {
                    charCounter = textArray[i].indexOf("\n");
                } else {
                    charCounter += textArray[i].length();
                }
            }
            if (i != textArray.length - 1) {
                text += " ";
            }
        }
        return text;
    }

//	public void saveProperties() {
//		Properties properties = new Properties();
//
//		for (AbstractJavaProperty property : runProperties.values()) {
//
//			/* check if property is modifiered */
//			if (property.getPropertyDefaultValue() != property.getValue()) {
//				properties.put(property.getClass().getName(), property.getValue());
//			}
//
//			try {
//				FileOutputStream fos = new FileOutputStream(JPService.getProperty(JPPropertyFile.class).getValue());
//				properties.store(fos, "MyProperties");
//			} catch (IOException ex) {
//				getApplicationLogger().error("Could not save properties!", ex);
//			}
//		}
//	}

    /**
     *
     */
    public static void reset() {
        registeredPropertyClasses.clear();
        initializedProperties.clear();
        loadedProperties.clear();
        overwrittenDefaultValueMap.clear();
        argumentsAnalyzed = false;
        initJPSDefaultProperties();
    }

    public static boolean testMode() {
        try {
            return JPService.getProperty(JPTestMode.class).getValue();
        } catch (JPServiceException ex) {
            printError("Could not detect TestMode!", ex);
        }
        return false;
    }

    public static boolean verboseMode() {
        try {
            return JPService.getProperty(JPVerbose.class).getValue();
        } catch (JPServiceException ex) {
            printError("Could not detect VerboseMode!", ex);
        }
        return false;
    }

    public static boolean forceMode() {
        try {
            return JPService.getProperty(JPForce.class).getValue();
        } catch (JPServiceException ex) {
            printError("Could not detect ForceMode!", ex);
        }
        return false;
    }

    public static boolean debugMode() {
        try {
            return JPService.getProperty(JPDebugMode.class).getValue();
        } catch (JPServiceException ex) {
            printError("Could not detect DebugMode!", ex);
        }
        return false;
    }
}

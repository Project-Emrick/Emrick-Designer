/**
 * <b>Main Source Package</b>
 * <pre>
 *
 * <b>Basic Overview of Code Structure:</b>
 *   <b>Main File - MediaEditorGUI.java</b>
 *     Contains all logic for app setup and contains many implemented listener methods for handling events and passing data between classes
 *
 *     Notable Methods (in no particular order):
 *       createAndShowGUI() - sets up the window and initializes and adds all the panels
 *       updateEffectViewPanel() - called whenever a new effect panel needs to be created (selecting a different performer, selecting a different effect type, etc.)
 *       exportPackets() - exports all packet data to a single file in a format that is readable by the lights
 *       onFinishRepaint() - called after every repaint. contains load balancing logic that adjusts the frame timer to account for processing time
 *
 *       there are too many methods to list them all out, but for the most part the naming conventions are pretty intuitive
 *
 *   <b>FootballFieldPanel.java</b>
 *     Contains the visual rendering and logic surrounding the main field panel including the management of realtime drill positions
 *
 *     Notable Methods:
 *       paintComponent() - used to paint all performers over the field
 *       calculateColor() - used to calculate the color of each performer in the phase of the light effect
 *       dotToPoint() - converts drill positions to screen positions
 *
 *   <b>Drill.java</b>
 *     contains all data necessary for managing drill positions
 *
 *   <b>Actions Folder</b>
 *     This folder contains files related to actions. We use action objects to execute changes to the effects during editing. Using these classes to make changes that can be undone or redone during editing. When able, using actions that are cabable of handling multiple changes at once so undo does not need to be pressed multiple times.
 *
 *     Notable Files:
 *       UndoableAction.java - This interface is what is implemented to allow for undo/redo features. All logic should be placed in the execute() method and then the logic to undo an action is placed in the undo() method. Ensure that when an action is executed, undone, or redone, the previous state of the relevant data is not lost.
 *       EffectPerformerMap.java - Used to pair an effect with a performer. Using a list of these objects, multiple effect changes can take place within 1 undoable action.
 *
 *   <b>Audio Folder</b>
 *     contains all relevant files to handle audio playback
 *
 *   <b>Effect Folder</b>
 *     contains relevant files for creating and managing effects
 *
 *     Notable Files:
 *       Effect.java - contains object used to store effect data
 *       EffectManager.java - used to manage effect actions (create, replace, remove)
 *
 *   <b>Serde Folder</b>
 *     Contains files used to convert project data into a saveable format. The adapters are used as instructions to the Google Gson package for how data should be converted from objecct form to JSON form and vice versa.
 *
 *     Notable Files:
 *       ModernProjectFile.java - This file contains the object that is saved to a json file when saving the project. Any data that should be contained in the save file should be loaded into this object. If any errors are present when saving a particular object, it may be necessary to create an adapter object to help Gson parse the object.
 *
 * Message Alex Bolinger (alex.bolinger3514@gmail.com if he isn't still in the discord) if you have any further questions
 * </pre>
 */
package org.emrick.project;


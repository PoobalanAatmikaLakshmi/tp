package seedu.apollo.command.module;

import seedu.apollo.exception.module.DuplicateModuleException;
import seedu.apollo.exception.module.LessonAddedException;
import seedu.apollo.exception.utils.IllegalCommandException;
import seedu.apollo.module.LessonType;
import seedu.apollo.module.Timetable;
import seedu.apollo.storage.Storage;
import seedu.apollo.ui.Ui;
import seedu.apollo.command.Command;
import seedu.apollo.exception.module.InvalidModule;
import seedu.apollo.module.Module;
import seedu.apollo.module.ModuleList;
import seedu.apollo.task.TaskList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class AddModuleCommand extends Command {
    private static Logger logger = Logger.getLogger("AddModuleCommand");
    private Module module;

    private String params;


    /**
     * Constructor for AddModuleCommand.
     *
     * @param param The module code of the module to be added.
     * @param allModules The list of all modules.
     * @throws InvalidModule If the module code is invalid.
     */
    public AddModuleCommand(String param, ModuleList allModules) throws InvalidModule, IllegalCommandException {

        AddModuleCommand.setUpLogger();
        assert (param != null) : "AddModuleCommand: Params should not be null!";
        assert (allModules != null) : "AddModuleCommand: Module list should not be null!";

        params = param;
        String[] args = param.split("\\s+");

        if (args.length != 3 && args.length != 1) {
            throw new IllegalCommandException();
        }

        String moduleCode = args[0];
        Module toAdd = allModules.findModule(moduleCode);

        if (toAdd == null) {

            throw new InvalidModule();
        }

        module = new Module(toAdd.getCode(), toAdd.getTitle(), toAdd.getModuleCredits());

    }

    public static void setUpLogger() {
        LogManager.getLogManager().reset();
        logger.setLevel(Level.ALL);
        ConsoleHandler logConsole = new ConsoleHandler();
        logConsole.setLevel(Level.SEVERE);
        logger.addHandler(logConsole);

        try {

            if (!new File("apollo.log").exists()) {
                new File("apollo.log").createNewFile();
            }

            FileHandler logFile = new FileHandler("apollo.log", true);
            logFile.setLevel(Level.FINE);
            logger.addHandler(logFile);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "File logger not working.", e);
        }
    }

    public boolean isAdded(ModuleList moduleList, Module module) {
        for (Module mod: moduleList) {
            if (mod.getCode().equals(module.getCode())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void execute(TaskList taskList, Ui ui, Storage storage, ModuleList moduleList, ModuleList allModules) {
        try {
            String[] args = params.split("\\s+");
            if (args.length == 3) {
                handleMultiCommand(moduleList, allModules, args);
                ui.printClassAddedMessage(args[0].toUpperCase(), getCommand(args[1]), args[2]);
            } else {
                if (isAdded(moduleList, module)) {
                    throw new DuplicateModuleException();
                }

                if (module != null) {
                    moduleList.add(module);
                    moduleList.sortModules();
                    Module referenceModule = allModules.findModule(module.getCode());
                    ui.printAddModuleMessage(module);
                    ui.printLessonTypeMessage(getLessonTypes(referenceModule));

                }
            }


            storage.updateModule(moduleList);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO Exception", e);
            ui.printErrorForIO();

        } catch (DuplicateModuleException e) {
            ui.printDuplicateModule();

        } catch (IllegalCommandException e) {
            ui.printInvalidCommand();
        } catch (ClassNotFoundException e) {
            ui.printInvalidLessonType();
        } catch (LessonAddedException e) {
            ui.printLessonExists();
        }
    }

    private void handleMultiCommand(ModuleList moduleList, ModuleList allModules, String[] args)
            throws IllegalCommandException, ClassNotFoundException, LessonAddedException {

        LessonType lessonType = this.getCommand(args[1]);
        Module searchModule = null;
        for (Module module1: allModules){
            if (module1.getCode().equalsIgnoreCase(this.module.getCode())){
                searchModule = module1;
                break;
            }
        }

        if (this.isAdded(moduleList, module)){
            int index = 0;
            for (Module module: moduleList){
                if (module.getCode().equals(this.module.getCode())){
                    this.module.setTimetable(module.getModuleTimetable());
                    break;
                }
                index++;
            }
            module.setTimetable(moduleList.get(index).getModuleTimetable());
            if (module.hasLessonType(lessonType)){
                throw new LessonAddedException();
            }

            addTimetable(searchModule, lessonType, args[2]);
            moduleList.get(index).setTimetable(module.getModuleTimetable());
        } else {
            module.createNewTimeTable();
            addTimetable(searchModule, lessonType, args[2]);
            moduleList.add(module);
        }
    }

    private void addTimetable(Module searchModule, LessonType lessonType, String args) throws ClassNotFoundException {
        Boolean isFound = false;
        ArrayList<Timetable> copyList = new ArrayList<>(searchModule.getModuleTimetable());
        for (Timetable timetable: copyList){
            LessonType searchLessonType = determineLessonType(timetable.getLessonType());
            if (searchLessonType.equals(lessonType) && timetable.getClassnumber().equals(args)){
                module.getModuleTimetable().add(timetable);
                isFound = true;
            }
        }

        if (!isFound){
            throw new ClassNotFoundException();
        }
    }

    public ArrayList<LessonType> getLessonTypes(Module module) {
        ArrayList<LessonType> lessonTypes = new ArrayList<>();
        for (Timetable timetable : module.getModuleTimetable()) {
            LessonType lessonType = determineLessonType(timetable.getLessonType());
            if (!lessonTypes.contains(lessonType) && lessonType != null) {
                lessonTypes.add(lessonType);
            }
        }
        return lessonTypes;
    }

    private LessonType getCommand(String arg) throws IllegalCommandException {
        switch (arg) {
        case "-lec":
            return LessonType.LECTURE;
        case "-plec":
            return LessonType.PACKAGED_LECTURE;
        case "-st":
            return LessonType.SECTIONAL_TEACHING;
        case "-dlec":
            return LessonType.DESIGN_LECTURE;
        case "-tut":
            return LessonType.TUTORIAL;
        case "-ptut":
            return LessonType.PACKAGED_TUTORIAL;
        case "-rcit":
            return LessonType.RECITATION;
        case "-lab":
            return LessonType.LABORATORY;
        case "-ws":
            return LessonType.WORKSHOP;
        case "-smc":
            return LessonType.SEMINAR_STYLE_MODULE_CLASS;
        case "-mp":
            return LessonType.MINI_PROJECT;
        case "-tt2":
            return LessonType.TUTORIAL_TYPE_2;
        default:
            throw new IllegalCommandException();
        }
    }

    public static LessonType determineLessonType(String lessonType) {
        switch (lessonType) {
        case "Lecture":
            return LessonType.LECTURE;
        case "Packaged Lecture":
            return LessonType.PACKAGED_LECTURE;
        case "Sectional Teaching":
            return LessonType.SECTIONAL_TEACHING;
        case "Design Lecture":
            return LessonType.DESIGN_LECTURE;
        case "Tutorial":
            return LessonType.TUTORIAL;
        case "Packaged Tutorial":
            return LessonType.PACKAGED_TUTORIAL;
        case "Recitation":
            return LessonType.RECITATION;
        case "Laboratory":
            return LessonType.LABORATORY;
        case "Workshop":
            return LessonType.WORKSHOP;
        case "Seminar-Style Module Class":
            return LessonType.SEMINAR_STYLE_MODULE_CLASS;
        case "Mini-Project":
            return LessonType.MINI_PROJECT;
        case "Tutorial Type 2":
            return LessonType.TUTORIAL_TYPE_2;
        default:
            return null;
        }
    }

}

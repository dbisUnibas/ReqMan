package ch.unibas.dmi.dbis.cs108pet.ui;

import ch.unibas.dmi.dbis.cs108pet.common.Version;
import ch.unibas.dmi.dbis.cs108pet.control.EntityController;
import ch.unibas.dmi.dbis.cs108pet.data.Milestone;
import ch.unibas.dmi.dbis.cs108pet.export.ExcelMilestoneExporter;
import ch.unibas.dmi.dbis.cs108pet.management.OperationFactory;
import ch.unibas.dmi.dbis.cs108pet.storage.UuidMismatchException;
import ch.unibas.dmi.dbis.cs108pet.templating.ExportHelper;
import ch.unibas.dmi.dbis.cs108pet.ui.common.PopupStage;
import ch.unibas.dmi.dbis.cs108pet.ui.common.Utils;
import ch.unibas.dmi.dbis.cs108pet.ui.editor.EditorHandler;
import ch.unibas.dmi.dbis.cs108pet.ui.evaluator.EvaluatorHandler;
import ch.unibas.dmi.dbis.cs108pet.ui.event.CUDEvent;
import ch.unibas.dmi.dbis.cs108pet.ui.event.TargetEntity;
import ch.unibas.dmi.dbis.cs108pet.ui.help.HelpDisplay;
import ch.unibas.dmi.dbis.cs108pet.ui.overview.ExportOverviewView;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.RadioMenuItem;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;

/**
 * TODO: Write JavaDoc
 *
 * @author loris.sauter
 */
public class MainHandler implements MenuHandler {
  
  public static final String EXPORT_DISABLED_REASON = "The export feature is currently being re-written.\n" +
      "In particular, the complete export language is subject to change.";
  private static final Logger LOGGER = LogManager.getLogger(MainHandler.class);
  private static MainHandler instance = null;
  private final EvaluatorHandler evaluatorHandler;
  private final EditorHandler editorHandler;
  private MainScene mainScene;
  private MenuManager manager = MenuManager.getInstance();
  private StatusBar statusBar;
  
  public MainHandler(EvaluatorHandler evaluatorHandler, EditorHandler editorHandler) {
    LOGGER.traceEntry();
    this.evaluatorHandler = evaluatorHandler;
    this.evaluatorHandler.setOnFirstGroup(() -> {
      manager.enableGroupNeeded();
      evaluatorHandler.enableEvaluator();
    });
    this.editorHandler = editorHandler;
    manager.enableOpenItems();
    
  }
  
  public static MainHandler getInstance(EvaluatorHandler evaluatorHandler, EditorHandler editorHandler) {
    if (instance == null) {
      instance = new MainHandler(evaluatorHandler, editorHandler);
    }
    return instance;
  }
  
  @Override
  public void handleNewCatalogue(ActionEvent event) {
    editorHandler.handle(CUDEvent.generateCreationEvent(event, TargetEntity.CATALOGUE));
    if (!editorHandler.isCatalogueLoaded()) {
      return;
    }
    mainScene.setActive(MainScene.Mode.EDITOR);
    manager.enableCatalogueNeeded();
  }
  
  @Override
  public void handleNewGroup(ActionEvent event) {
    if (EntityController.getInstance().hasCourse() && EntityController.getInstance().hasCatalogue()) {
      boolean changeMode = false;
      if (!mainScene.isEvaluatorActive()) {
        changeMode = true;
      }
      evaluatorHandler.handle(CUDEvent.generateCreationEvent(event, TargetEntity.GROUP));
      manager.enableGroupNeeded();
      if (changeMode) {
        mainScene.setActive(MainScene.Mode.EVALUATOR);
      }
    }
  }
  
  @Override
  public void handleOpenCat(ActionEvent event) {
    if (EntityController.getInstance().hasCatalogue()) {
      LOGGER.warn("Cannot handle re-opening of catalogue. Silently ignoring");
      /*
      TODO re-open / new-open catalogue:
      * Reset EntityController
      * All Handlers
      * Save all open files
      * perform open.
       */
      return;
    }
    try {
      
      if (EntityController.getInstance().isStorageManagerReady()) {
        LOGGER.debug("Open Catalogue - course loaded");
        EntityController.getInstance().openCatalogue();
        mainScene.setActive(MainScene.Mode.EDITOR);
        editorHandler.setupEditor();
        manager.enableCatalogueNeeded();
      } else if (event.isConsumed()) {
        LOGGER.warn("Something went very wrong");
      } else {
        LOGGER.debug("Open catalogue and course");
        handleOpenCourse(event);
        event.consume();
        handleOpenCat(event);
      }
    } catch (IllegalStateException ex) {
      LOGGER.catching(ex);
      Utils.showErrorDialog("Error on loading catalgoue", ex.getMessage());
    } catch (UuidMismatchException e) {
      LOGGER.catching(e);
      Utils.showErrorDialog("ID Mismatch", e.getMessage());
    } catch (IOException e) {
      LOGGER.catching(e);
      Utils.showErrorDialog("IOException during Open Catalogue", e.getLocalizedMessage());
    }
    
  }
  
  @Override
  public void handleOpenGroups(ActionEvent event) {
    if (mainScene.isEditorActive()) {
      handleShowEvaluator(event);
    }
    boolean catCoursNeeded = false;
    if (EntityController.getInstance().hasCourse()) {
      LOGGER.debug("Opening group(s) with course set...");
      if (EntityController.getInstance().hasCatalogue()) {
        LOGGER.debug("... and catalogue set");
        evaluatorHandler.handleOpenGroups(event);
        manager.enableGroupNeeded();
      } else {
        // No cat set
        catCoursNeeded = true;
      }
    } else if (event.isConsumed()) {
      LOGGER.warn("Open Groups: Already consumed event. Ignoring");
    } else {
      // No course set
      catCoursNeeded = true;
    }
    if (catCoursNeeded) {
      LOGGER.debug("Opening groups and loading cat/course");
      ActionEvent catEvent = event.copyFor(event, Event.NULL_SOURCE_TARGET);
      handleOpenCat(catEvent); // Loads course as well
      event.consume();
      handleOpenGroups(event);
    }
    LOGGER.debug("Opening performed");
  }
  
  @Override
  public void handleSaveCat(ActionEvent event) {
    editorHandler.saveCatalogue();
  }
  
  @Override
  public void handleSaveGroup(ActionEvent event) {
    if (!evaluatorHandler.isGroupLoaded()) {
      return;
    }
    evaluatorHandler.handleSaveGroup(event);
  }
  
  @Override
  public void handleSaveCatAs(ActionEvent event) {
    editorHandler.saveAsCatalogue();
  }
  
  @Override
  public void handleSaveGroupAs(ActionEvent event) {
    if (!evaluatorHandler.isGroupLoaded()) {
      return;
    }
    evaluatorHandler.handleSaveGroupAs(event);
  }
  
  @Override
  public void handleExcelExport(ActionEvent event) {
    DirectoryChooser dc = new DirectoryChooser();
    dc.setTitle("Folder for Excel Files");
    File exportDir = dc.showDialog(mainScene.getWindow());
    if (exportDir == null) {
      return;
    }
    
    for (Milestone milestone : EntityController.getInstance().getObservableMilestones()) {
      ExcelMilestoneExporter.exportRequirements(EntityController.getInstance().groupList(), Paths.get(exportDir.getPath(), milestone.getName() + ".xlsx").toFile(), milestone);
    }
  }
  
  @Override
  public void handleExportCat(ActionEvent event) {
    if (!EntityController.getInstance().hasCatalogue() && EntityController.getInstance().hasCourse()) {
      return;
    }
    if (ExportHelper.getInstance().canQuickExport()) {
      try {
        ExportHelper.getInstance().exportCatalogue();
        mainScene.showNotification("Catalogue export finished");
        return;
        //Notifications.create().title("Export successful!").hideAfter(Duration.seconds(5)).text("Catalogue exported to:\\"+f.getAbsolutePath()).showInformation();
      } catch (FileNotFoundException | UnsupportedEncodingException e) {
        LOGGER.catching(Level.FATAL, e);
      }
    }
    FileChooser exportConfigFC = new FileChooser();
    exportConfigFC.setTitle("Templating Config");
    File exportConfig = exportConfigFC.showOpenDialog(mainScene.getWindow());
    if (exportConfig == null) {
      // Userabort
      return;
    }
    LOGGER.debug("Templating Config: {}", exportConfig);
    FileChooser fc = new FileChooser();
    fc.setTitle("Export Catalogue");
    File f = fc.showSaveDialog(mainScene.getWindow());
    if (f != null) {
      LOGGER.debug("Exporting to {}", f);
      try {
        ExportHelper.getInstance().exportCatalogue(exportConfig, f);
        mainScene.showNotification("Export finished to " + f.getAbsolutePath());
        
        //Notifications.create().title("Export successful!").hideAfter(Duration.seconds(5)).text("Catalogue exported to:\\"+f.getAbsolutePath()).showInformation();
      } catch (FileNotFoundException | UnsupportedEncodingException e) {
        LOGGER.catching(Level.FATAL, e);
      }
    }
  }
  
  @Override
  public void handleExportGroups(ActionEvent event) {
    // TODO Temporary solution, until pretty ui is made
    Utils.showInfoDialog("Export All Groups", "Export All Groups", "You are about the export all opened groups.\n" +
        "Please be aware, that this operation may take a while and during the export, the application may not respond.\n" +
        "\n" +
        "Please make also sure, that you have saved your assessment so far.");
    if (!EntityController.getInstance().hasCatalogue() && EntityController.getInstance().hasCourse() && EntityController.getInstance().hasGroups()) {
      return;
    }
    FileChooser exportConfigFC = new FileChooser();
    exportConfigFC.setTitle("Open Templating Config");
    File exportConfig = exportConfigFC.showOpenDialog(mainScene.getWindow());
    if (exportConfig == null) {
      // Userabort
      return;
    }
    LOGGER.debug("Templating Config: {}", exportConfig);
    DirectoryChooser dc = new DirectoryChooser();
    dc.setTitle("Export Destination");
    File destDir = dc.showDialog(mainScene.getWindow());
    if (destDir == null) {
      // Userabort
      return;
    }
    LOGGER.debug("Exporting groups to {}", destDir);
    EntityController.getInstance().groupList().forEach(group -> {
      try {
        String name = StringUtils.isNotBlank(group.getExportFileName()) ? group.getExportFileName() : group.getName();
        File f = Paths.get(destDir.getPath(), name).toFile();
        ExportHelper.exportGroup(exportConfig, f, group);
        try {
          Notifications.create().title("Export successful!").hideAfter(Duration.seconds(5)).text("Export of group " + group.getName() + " finished!").showInformation();
        } catch (NullPointerException e) {
          LOGGER.warn("Catching NullPointerException for Notification. This is an untriangulated bug.");
        }
      } catch (FileNotFoundException | UnsupportedEncodingException e) {
        LOGGER.catching(Level.FATAL, e);
      }
    });
    try {
      Notifications.create().title("Export successful!").hideAfter(Duration.seconds(5)).text("Exported all groups").showInformation();
    } catch (NullPointerException e) {
      LOGGER.warn("Catching NullPointerException for Notification. This is an untriangulated bug.");
    }
  }
  
  @Override
  public void handleExportGroup(ActionEvent event) {
    if (!EntityController.getInstance().hasCatalogue() && EntityController.getInstance().hasCourse() && EntityController.getInstance().hasGroups()) {
      return;
    }
    FileChooser exportConfigFC = new FileChooser();
    exportConfigFC.setTitle("Templating Config");
    File exportConfig = exportConfigFC.showOpenDialog(mainScene.getWindow());
    if (exportConfig == null) {
      // Userabort
      return;
    }
    LOGGER.debug("Templating Config: {}", exportConfig);
    FileChooser fc = new FileChooser();
    fc.setTitle("Export Group");
    File f = fc.showSaveDialog(mainScene.getWindow());
    if (f != null) {
      LOGGER.debug("Exporting to {}", f);
      try {
        ExportHelper.exportGroup(exportConfig, f, evaluatorHandler.getActiveGroup());
        mainScene.showNotification("Export finished to " + f.getAbsolutePath());
        
        //Notifications.create().title("Export successful!").hideAfter(Duration.seconds(5)).text("Group exported to:\\"+f.getAbsolutePath()).showInformation();
      } catch (FileNotFoundException | UnsupportedEncodingException e) {
        LOGGER.catching(Level.FATAL, e);
      }
    }
  }
  
  @Override
  public void handleQuit(ActionEvent event) {
    Platform.exit();
  }
  
  @Override
  public void handleNewReq(ActionEvent event) {
    if (mainScene.isEditorActive()) {
      if (editorHandler.isCatalogueLoaded()) {
        editorHandler.handle(CUDEvent.generateCreationEvent(event, TargetEntity.REQUIREMENT));
      }
    }
  }
  
  @Override
  public void handleNewMS(ActionEvent event) {
    if (mainScene.isEditorActive()) {
      if (editorHandler.isCatalogueLoaded()) {
        editorHandler.handle(CUDEvent.generateCreationEvent(event, TargetEntity.MILESTONE));
      }
    }
    
  }
  
  @Override
  public void handleModCat(ActionEvent event) {
    if (mainScene.isEditorActive()) {
      if (editorHandler.isCatalogueLoaded()) {
        editorHandler.handle(CUDEvent.generateModificationEvent(event, TargetEntity.CATALOGUE, null));// By design, can be null
      }
    }
    
  }
  
  @Override
  public void handleModReq(ActionEvent event) {
    if (mainScene.isEditorActive()) {
      if (editorHandler.isCatalogueLoaded()) {
        editorHandler.handleModification(CUDEvent.generateModificationEvent(event, TargetEntity.REQUIREMENT, editorHandler.getSelectedRequirement()));
      }
    }
  }
  
  @Override
  public void handleModMS(ActionEvent event) {
    if (mainScene.isEditorActive()) {
      if (editorHandler.isCatalogueLoaded()) {
        editorHandler.handleModification(CUDEvent.generateModificationEvent(event, TargetEntity.MILESTONE, editorHandler.getSelectedMS()));
      }
    }
  }
  
  @Override
  public void handleModGroup(ActionEvent event) {
    if (mainScene.isEvaluatorActive()) {
      if (evaluatorHandler.isGroupLoaded()) {
        evaluatorHandler.handle(CUDEvent.generateModificationEvent(event, TargetEntity.GROUP, null));
      }
    }
  }
  
  @Override
  public void handleShowEditor(ActionEvent event) {
    if (mainScene.isEvaluatorActive()) {
      mainScene.setActive(MainScene.Mode.EDITOR);
    }
  }
  
  @Override
  public void handleShowEvaluator(ActionEvent event) {
    if (mainScene.isEditorActive()) {
      mainScene.setActive(MainScene.Mode.EVALUATOR);
    }
    
  }
  
  @Override
  public void handlePresentationMode(ActionEvent event) {
    if (event.getSource() instanceof RadioMenuItem) {
      RadioMenuItem rmi = (RadioMenuItem) event.getSource();
      if (rmi.isSelected()) {
        if (!mainScene.getRoot().getStyleClass().contains("presentation")) {
          mainScene.getRoot().getStyleClass().add("presentation");
        }
      } else {
        mainScene.getRoot().getStyleClass().remove("presentation");
      }
    }
  }
  
  @Override
  public void handleNewCourse(ActionEvent event) {
    editorHandler.handle(CUDEvent.generateCreationEvent(event, TargetEntity.COURSE));
    if (!EntityController.getInstance().hasCourse()) {
      return;
    }
    mainScene.setActive(MainScene.Mode.EDITOR);
    editorHandler.setupEditor();
    manager.enableCatalogueNeeded();
  }
  
  @Override
  public void handleOpenCourse(ActionEvent event) {
    editorHandler.openCourse();
  }
  
  @Override
  public void handleSaveCourse(ActionEvent event) {
    editorHandler.saveCourse();
  }
  
  @Override
  public void handleSaveCourseAs(ActionEvent event) {
    editorHandler.saveAsCourse();
  }
  
  @Override
  public void handleClearFilter(ActionEvent event) {
    LOGGER.debug("Clearing filter");
    switch (mainScene.getActiveMode()) {
      case EDITOR:
        if (EntityController.getInstance().hasCatalogue()) {
          editorHandler.closeFilterBar();
        }
        break;
      case EVALUATOR:
        if (EntityController.getInstance().hasGroups()) {
          evaluatorHandler.closeFilterBar();
        }
        break;
    }
    
  }
  
  @Override
  public void handleShowFilterBar(ActionEvent event) {
    LOGGER.debug("Showing filter bar");
    switch (mainScene.getActiveMode()) {
      case EDITOR:
        LOGGER.debug("Showing editor filter");
        if (EntityController.getInstance().hasCatalogue()) {
          editorHandler.showFilterBar();
        } else {
          LOGGER.debug("Not showing filter bar because no catalogue available");
        }
        break;
      case EVALUATOR:
        LOGGER.debug("Showing evaluator filter");
        if (EntityController.getInstance().hasGroups()) {
          evaluatorHandler.showFilterBar();
        } else {
          LOGGER.debug("Not showing filter bar because no groups available");
        }
        break;
    }
    
  }
  
  @Override
  public void handleSplitGroup(ActionEvent event) {
    if (EntityController.getInstance().hasGroups()) {
      mainScene.setActive(MainScene.Mode.EVALUATOR);
      evaluatorHandler.handleSplit(event);
    } else {
      LOGGER.debug("Cannot split group if there is no group available");
    }
  }
  
  @Override
  public void handleCatalogueStatistics(ActionEvent event) {
    editorHandler.showStatistics();
  }
  
  @Override
  public void handleImport(ActionEvent event) {
    if (Utils.showConfirmationDialog("Import Catalogue", "You will lose unsafed changes on both, groups and catalogue / course.\nAre you sure to continue?")) {
      mainScene.setActive(MainScene.Mode.EDITOR);
      FileChooser fc = Utils.createFileChooser("Import Catalogue");
      File f = fc.showOpenDialog(mainScene.getWindow());
      evaluatorHandler.closeAll();
      editorHandler.closeAll();
      EntityController.getInstance().reset();
      try {
        if (EntityController.getInstance().convertOld(f)) {
          Utils.showInfoDialog("Conversion Finished", "The conversion finished and will be displayed.");
          editorHandler.setupEditor();
          manager.enableCatalogueNeeded();
          manager.enableEditorItems();
        }
      } catch (RuntimeException ex) {
        LOGGER.fatal("Exception in conversion");
        LOGGER.catching(Level.FATAL, ex);
        Utils.showErrorDialog("Error - " + ex.getClass().getSimpleName(),
            "An exception occurred",
            "An uncaught exception occurred. The exception is of type " + ex.getClass().getSimpleName() + ".\n" +
                "The exception's message is as follows:\n\t" + ex.getMessage() + "\n" +
                "pet probably would still work, but re-start is recommended.\n");
      }
    }
    
  }
  
  @Override
  public void handleGroupStatistics(ActionEvent event) {
    evaluatorHandler.showStatistics();
  }
  
  @Override
  public void handleShowAbout(ActionEvent event) {
    LOGGER.debug("ABOUT");
    Utils.showInfoDialog("About", "cs108pet " + Version.getInstance().getFullVersion(), "Performance Evaluation Tool.\n" +
        "Tool do define a schema for requirements (known as the catalogue in the editor mode) and use this schema to assess progress on it (in the evaluator mode)");
  }
  
  @Override
  public void handleShowHelp(ActionEvent event) {
    LOGGER.debug("HELP");
    HelpDisplay helpDisplay = new HelpDisplay();
    helpDisplay.show();
  }
  
  @Override
  public void handleModCourse(ActionEvent event) {
    if (mainScene.isEditorActive()) {
      if (editorHandler.isCatalogueLoaded()) {
        editorHandler.handle(CUDEvent.generateModificationEvent(event, TargetEntity.COURSE, null));// By design, can be null
      }
    }
  }
  
  @Override
  public void handleExportOverviewGroups(ActionEvent event) {
    LOGGER.debug("Handling menu: Export Overview Groups");
    Scene s = new Scene(new ExportOverviewView());
    PopupStage ps = new PopupStage("Export Groups Overview", s);
    ps.showAndWait();
  }
  
  public void setMainScene(MainScene mainScene) {
    this.mainScene = mainScene;
  }
  
  public void setStatusBar(StatusBar statusBar) {
    this.statusBar = statusBar;
    evaluatorHandler.setStatusBar(statusBar);
    editorHandler.setStatusBar(statusBar);
    OperationFactory.registerStatusBar(statusBar);
  }
  
  void stop() {
    evaluatorHandler.stop();
    EntityController.getInstance().saveSession();
  }
  
  void checkGroupsPresent() {
    if (evaluatorHandler.isGroupLoaded()) {
      MenuManager.getInstance().enableGroupNeeded();
    }
  }
}

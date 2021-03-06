package ch.unibas.dmi.dbis.cs108pet.ui.editor;

import ch.unibas.dmi.dbis.cs108pet.ui.common.CourseInfoView;
import ch.unibas.dmi.dbis.cs108pet.ui.common.FilterBar;
import ch.unibas.dmi.dbis.cs108pet.ui.common.PopupStage;
import ch.unibas.dmi.dbis.cs108pet.ui.common.TitleProvider;
import ch.unibas.dmi.dbis.cs108pet.ui.overview.CatalogueStatisticsView;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;

/**
 * TODO: Write JavaDoc
 *
 * @author loris.sauter
 */
public class EditorView extends BorderPane implements TitleProvider {
  
  static final Logger LOGGER_UI = LogManager.getLogger(EditorView.class);
  private final EditorHandler handler;
  private SplitPane splitter;
  private RequirementTableView reqTableView;
  private MilestonesListView msView;
  private CourseInfoView infoView;
  private VBox topBox;
  private FilterBar filterBar;
  private String title = "Editor";
  
  public EditorView(EditorHandler handler) {
    super();
    LOGGER_UI.trace("<init>");
    this.handler = handler;
    this.handler.setEditorView(this);
    initComponents();
    layoutComponents();
    if (handler.isCatalogueLoaded()) {
      handler.setupEditor();
    }
  }
  
  @Override
  public String getTitle() {
    return title;
  }
  
  public void indicateWaiting(boolean waiting) {
    getScene().getRoot().setCursor(waiting ? Cursor.WAIT : Cursor.DEFAULT);
  }
  
  public void refresh() {
    handler.setupEditor();
  }
  
  public CourseInfoView getCourseInfoView() {
    return infoView;
  }
  
  public void closeFilterBar() {
    if (filterBar != null) {
      filterBar.close();
    }
    //topBox.getChildren().remove(filterBar);
  }
  
  private Function<Void, Void> statisticsCloser;
  
  public void showStatistics() {
    CatalogueStatisticsView view = new CatalogueStatisticsView();
    Scene scene = new Scene(view);
    PopupStage stage = new PopupStage("Catalogue Overview", scene, false);
    // Following code hides the stage asap the focus is lost
    /*stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if(!stage.isFocused()){
        stage.hide();
      }
    });*/
    stage.show();
    reqTableView.setOnPointsChanged(pts -> {
      if (stage.isShowing()) {
        view.update();
      }
    });
    statisticsCloser = (unused) -> {
      stage.close();
      return null;
    };
  }
  
  public void closeAll() {
    reqTableView.clear();
  }
  
  public void stop() {
    if (statisticsCloser != null) {
      statisticsCloser.apply(null);
    }
  }
  
  void enableAll() {
    reqTableView.setDisable(false);
    msView.setDisable(false);
  }
  
  void disableAll() {
    reqTableView.setDisable(true);
    msView.setDisable(true);
  }
  
  RequirementTableView getRequirementsView() {
    return reqTableView;
  }
  
  MilestonesListView getMilestoneView() {
    return msView;
  }
  
  private void initComponents() {
    LOGGER_UI.trace(":initComps");
    reqTableView = new RequirementTableView();
    reqTableView.setOnAdd(handler::handleCreation);
    reqTableView.setOnRemove(handler::handleDeletion);
    reqTableView.setOnModify(handler::handleModification);
    
    msView = new MilestonesListView(handler);
    
    infoView = new CourseInfoView();
    
    splitter = new SplitPane();
    
    splitter.prefWidthProperty().bind(widthProperty());
    splitter.prefHeightProperty().bind(heightProperty());
    
    filterBar = new FilterBar(handler);
    filterBar.prefWidthProperty().bind(widthProperty());
    
    topBox = new VBox();
  }
  
  public void showFilterBar() {
    filterBar.clear();
    if (!topBox.getChildren().contains(filterBar)) {
      topBox.getChildren().add(0, filterBar);
    } else {
      topBox.getChildren().remove(filterBar);
    }
  }
  
  private void layoutComponents() {
    LOGGER_UI.trace(":layoutComps");
    splitter.getItems().addAll(msView, reqTableView);
    splitter.setDividerPositions(0.33);
    
    topBox.getChildren().add(infoView);
    setTop(topBox);
    
    
    infoView.prefWidthProperty().bind(widthProperty());
    
    setCenter(splitter);
    disableAll();
    
    // TEMP
    /*
    infoView.setOnMouseClicked(evt -> {
      if (evt.getClickCount() == 2) {
        CUDEvent event = CUDEvent.generateModificationEvent(new ActionEvent(evt.getSource(), evt.getTarget()), TargetEntity.CATALOGUE, null);
        handler.handleModification(event);
      }
    });
    */
  }
}

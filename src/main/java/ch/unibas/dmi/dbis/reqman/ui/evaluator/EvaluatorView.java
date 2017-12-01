package ch.unibas.dmi.dbis.reqman.ui.evaluator;

import ch.unibas.dmi.dbis.reqman.data.Catalogue;
import ch.unibas.dmi.dbis.reqman.data.Group;
import ch.unibas.dmi.dbis.reqman.ui.common.CourseInfoView;
import ch.unibas.dmi.dbis.reqman.ui.common.TitleProvider;
import ch.unibas.dmi.dbis.reqman.ui.editor.EditorView;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

/**
 * TODO: Write JavaDoc
 *
 * @author loris.sauter
 */
public class EvaluatorView extends HBox implements TitleProvider {
  
  final static Logger LOGGER = LogManager.getLogger(EditorView.class);
  
  private final String title = "Evaluator";
  private final EvaluatorHandler handler;
  private SplitPane horizontalSplit;
  private SplitPane verticalSplit;
  private VBox leftContent;
  private VBox rightContent;
  private TabPane tabPane;
  private GroupListView groupView;
  private CourseInfoView courseView;
  private HashMap<String, Tab> legacyGroupTabMap = new HashMap<>();
  
  public EvaluatorView(EvaluatorHandler handler) {
    super();
    this.handler = handler;
    this.handler.setEvaluatorView(this);
    
    initComponents();
    layoutComponents();
    disableAll();
  }
  
  @Override
  public String getTitle() {
    return title;
  }
  
  public void displayCatalogueInfo(Catalogue catalogue) {
    courseView.refresh();
  }
  
  public boolean isGroupTabbed(Group active) {
    return false;
  }
  
  public void addGroupTab(AssessmentView view, boolean fresh) {
    Tab tab = new Tab();
    
    tab.setUserData(view.getActiveGroup().getName());
    tab.setText(view.getActiveGroup().getName());
    view.bindToParentSize(rightContent);
    tab.setContent(view);
    tabPane.getTabs().addAll(tab);
    
    legacyGroupTabMap.put(view.getActiveGroup().getName(), tab);
    if (fresh) {
      markDirty(view.getActiveGroup());
    }
    
  }
  
  public void markDirty(Group group) {
    Tab tab = legacyGroupTabMap.get(group.getName());
    if (tab.getText().indexOf("*") < 0) {
      tab.setText(tab.getText() + "*");
    }
    if (!tab.getStyleClass().contains("modified")) {
      tab.getStyleClass().add("modified");
    }
  }
  
  public void unmarkDirty(Group modified) {
    Tab tab = legacyGroupTabMap.get(modified.getName());
    tab.getStyleClass().remove("modified");
    if (tab.getText().indexOf("*") >= 0) {
      String text = tab.getText().substring(0, tab.getText().indexOf("*"));
      tab.setText(text);
    }
  }
  
  public boolean isDirty(Group group) {
    Tab tab = legacyGroupTabMap.get(group.getName());
    return tab.getStyleClass().contains("modified");
  }
  
  public void setActiveTab(AssessmentView assessmentView) {
    setActiveTab(assessmentView.getActiveGroup().getName());
  }
  
  public void setActiveTab(String name) {
    LOGGER.trace(":setActiveTab");
    LOGGER.entry(name);
    Tab toActive = legacyGroupTabMap.get(name);
    tabPane.getSelectionModel().select(toActive);
  }
  
  public Group getActiveGroup() {
    Tab tab = tabPane.getSelectionModel().getSelectedItem();
    if (tab.getUserData() instanceof String) {
      return handler.getGroupByName((String) tab.getUserData());
    }
    return null;
  }
  
  public void removeTab(Group g) {
    Tab tab = legacyGroupTabMap.get(g.getName());
    legacyGroupTabMap.remove(g.getName());
    tabPane.getTabs().remove(tab);
  }
  
  void enableAll() {
    LOGGER.traceEntry();
    groupView.setDisable(false);
    courseView.setDisable(false);
    tabPane.setDisable(false);
  }
  
  private void layoutComponents() {
    horizontalSplit.prefWidthProperty().bind(widthProperty());
    horizontalSplit.prefHeightProperty().bind(heightProperty());
    verticalSplit.setOrientation(Orientation.VERTICAL);
    verticalSplit.prefWidthProperty().bind(widthProperty());
    verticalSplit.prefHeightProperty().bind(heightProperty());
    
    VBox upper = new VBox();
    upper.getChildren().add(courseView);
    
    VBox lower = new VBox();
    lower.getChildren().add(horizontalSplit);
    
    verticalSplit.getItems().addAll(upper, lower);
    verticalSplit.setDividerPositions(0.08);
    leftContent.getChildren().add(groupView);
    
    rightContent.setPadding(new Insets(10));
    rightContent.setSpacing(10);
    rightContent.prefHeightProperty().bind(heightProperty());
    
    tabPane.setPadding(new Insets(10));
    tabPane.getStylesheets().add("style.css");
    rightContent.getChildren().addAll(tabPane); // TODO Iff no catalogue loaded display usage message like intellij
    VBox.setVgrow(tabPane, Priority.ALWAYS);
    
    
    horizontalSplit.setDividerPositions(0.33);
    horizontalSplit.getItems().addAll(leftContent, rightContent);
    
    getChildren().addAll(verticalSplit);
  }
  
  private void initComponents() {
    horizontalSplit = new SplitPane();
    verticalSplit = new SplitPane();
    
    leftContent = new VBox();
    rightContent = new VBox();
    tabPane = new TabPane();
    
    courseView = new CourseInfoView();
    groupView = new GroupListView(handler);
  }
  
  private void disableAll() {
    groupView.setDisable(true);
    courseView.setDisable(true);
    tabPane.setDisable(true);
  }
}

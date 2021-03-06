package ch.unibas.dmi.dbis.cs108pet.ui.common;

import javafx.event.ActionEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Wrapper for easier usage
 *
 * @author loris.sauter
 */
public abstract class AbstractVisualCreator<T> extends AbstractPopulatedGridScene implements Creator<T> {
  
  protected Logger log = LogManager.getLogger();
  
  protected SaveCancelPane buttons = new SaveCancelPane();
  
  public AbstractVisualCreator() {
    super();
    setupButtonHandling();
  }
  
  public abstract String getPromptTitle();
  
  public abstract void handleSaving(ActionEvent event);
  
  
  /**
   * Dismisses the visual creator: It hides the parental window und thus cancels the creation.
   */
  public void dismiss() {
    getWindow().hide();
  }
  
  protected void setupButtonHandling() {
    buttons.setOnSave(this::handleSaving);
    buttons.setOnCancel(event -> dismiss());
  }
}

package ch.unibas.dmi.dbis.cs108pet.ui.common;

/**
 * TODO: write JavaDoc
 *
 * @author loris.sauter
 */
public interface ModifiableListHandler<T> {
  void onRemove(ModifiableListView.RemoveEvent<T> event);
  
  void onAdd(ModifiableListView.AddEvent<T> event);
}

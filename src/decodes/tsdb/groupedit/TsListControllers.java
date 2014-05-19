package decodes.tsdb.groupedit;

/**
* Panels that use the TsListControlsPanel of buttons must provide a controller
* that implements this interface.
*/
public interface TsListControllers
{
        /** @return the type of entity being listed. */
        public String getEntityType();

        /**
          Called when user presses the 'Open' Button.
          @param e ignored.
        */
        public void openPressed();

        /**
          Called when user presses the 'New' Button.
          @param e ignored.
        */
        public void newPressed();

        /**
          Called when user presses the 'Delete' Button.
          @param e ignored.
        */
        public void deletePressed();

        /**
          Called when user presses the 'refresh' Button.
          @param e ignored.
        */
        public void refresh();
}

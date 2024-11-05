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
        */
        public void openPressed();

        /**
          Called when user presses the 'New' Button.
        */
        public void newPressed();

        /**
          Called when user presses the 'Delete' Button.
        */
        public void deletePressed();

        /**
          Called when user presses the 'refresh' Button.
        */
        public void refresh();

        public void plot();
}

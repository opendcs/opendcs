package lrgs.noaaportrecv;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import ilex.gui.JobDialog;
import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;


public class NoaaportTestSvrThread extends BasicSvrThread {

	
	private JobDialog myDialog;
	private Socket mySocket;
	
	NoaaportTestSvrThread(BasicServer parent, Socket socket, JobDialog mydialog)
	{
		super(parent, socket);
		myDialog = mydialog;
		mySocket=socket;
		myDialog.addToProgress("New Client Connected");
	}
	
	@Override
	protected void serviceClient() {
		if(mySocket.isConnected())
		{
			InputStream mystream = null;
			try {
				mystream = mySocket.getInputStream();
				int readInt = mystream.read();
				if(readInt<0)
				{
					this.disconnect();
					myDialog.addToProgress("\nClient Dissconnected");
					parent.rmSvrThread(this);
					return;
				}
				myDialog.addToProgressNLF(String.valueOf((char)readInt));
			} catch (IOException e) {
				myDialog.addToProgress("IO Error "+ e.getMessage());
				myDialog.finishedJob();
				this.disconnect();
				return;
			}
		}
	}

}

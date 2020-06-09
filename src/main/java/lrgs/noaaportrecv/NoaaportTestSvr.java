package lrgs.noaaportrecv;
import java.io.IOException;
import java.net.Socket;


import ilex.gui.JobDialog;
import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;


public class NoaaportTestSvr extends BasicServer {

	private int myPort;
	private JobDialog myDialog = null;
	@Override
	protected BasicSvrThread newSvrThread(Socket sock) throws IOException {
		return new NoaaportTestSvrThread(this, sock,myDialog);
	}
	
	public NoaaportTestSvr(int port, JobDialog tmp) throws IOException, IllegalArgumentException
	{
		super(port);
		myDialog = tmp;
		myDialog.addToProgress("Starting Up Server");
		myPort = port;
	}

}
